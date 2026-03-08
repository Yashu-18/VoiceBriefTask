package com.example.voicebrief.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.StatFs
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.voicebrief.data.local.dao.AudioChunkDao
import com.example.voicebrief.data.local.entities.AudioChunkEntity
import com.example.voicebrief.data.repository.MeetingRepository
import com.example.voicebrief.worker.TranscriptionWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class RecordingForegroundService : Service() {

    companion object {
        const val TAG = "RecordingService"
        const val EXTRA_MEETING_ID = "EXTRA_MEETING_ID"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val CHANNEL_ID = "recording_channel"
        const val NOTIFICATION_ID = 1
        const val SAMPLE_RATE = 16000
        const val MIN_STORAGE_MB = 50L
        const val SILENCE_THRESHOLD = 80
        const val SILENCE_WARNING_SECONDS = 10

        // Shared state for UI — ViewModel observes these directly
        private val _elapsedTime = MutableStateFlow(0L)
        val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()

        private val _statusText = MutableStateFlow("Ready to Record")
        val statusText: StateFlow<String> = _statusText.asStateFlow()
    }

    @Inject lateinit var meetingRepository: MeetingRepository
    @Inject lateinit var audioChunkDao: AudioChunkDao

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRecording = false
    private var isPaused = false
    private var pauseReason = ""
    private var currentMeetingId: Int = -1
    private var chunkIndex = 0
    private var recordingStartTime = 0L
    private var elapsedBeforePause = 0L

    private var audioRecord: AudioRecord? = null
    private val bufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    )

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager

    // Silence tracking
    private var silentReadCount = 0
    private val readsPerSecond get() = SAMPLE_RATE / (bufferSize / 2)

    // ─── Lifecycle ───────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        registerHeadsetReceiver()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val meetingId = intent?.getIntExtra(EXTRA_MEETING_ID, -1) ?: -1
        if (meetingId != -1) currentMeetingId = meetingId

        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
            ACTION_PAUSE -> pauseRecording(intent.getStringExtra("REASON") ?: "User paused")
            ACTION_RESUME -> resumeRecording()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        isPaused = false
        audioRecord?.release()
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        audioManager.abandonAudioFocus(audioFocusListener)
        try { unregisterReceiver(headsetReceiver) } catch (_: Exception) {}
        serviceScope.cancel()
    }

    // ─── Recording Control ───────────────────────────────────────

    private fun startRecording() {
        if (isRecording) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            stopSelf(); return
        }

        // Low storage check
        if (!hasEnoughStorage()) {
            updateNotification("Recording stopped - Low storage")
            stopSelf(); return
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Recording...", isRecording = true))

        // Request audio focus
        audioManager.requestAudioFocus(
            audioFocusListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )

        isRecording = true
        isPaused = false
        recordingStartTime = System.currentTimeMillis()
        silentReadCount = 0
        _elapsedTime.value = 0L
        _statusText.value = "Recording"

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize
        )
        audioRecord?.startRecording()

        serviceScope.launch { recordAudioChunks() }
        serviceScope.launch { updateTimerLoop() }
    }

    private fun pauseRecording(reason: String) {
        if (!isRecording || isPaused) return
        isPaused = true
        pauseReason = reason
        elapsedBeforePause += System.currentTimeMillis() - recordingStartTime
        audioRecord?.stop()
        _statusText.value = "Paused - $reason"
        updateNotification("Paused - $reason")
    }

    private fun resumeRecording() {
        if (!isRecording || !isPaused) return

        // Check storage before resume
        if (!hasEnoughStorage()) {
            updateNotification("Recording stopped - Low storage")
            stopRecording(); return
        }

        isPaused = false
        pauseReason = ""
        recordingStartTime = System.currentTimeMillis()
        silentReadCount = 0
        _statusText.value = "Recording"

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize
        )
        audioRecord?.startRecording()
        serviceScope.launch { recordAudioChunks() }
        updateNotification("Recording...")
    }

    private fun stopRecording() {
        isRecording = false
        isPaused = false
        _statusText.value = "Stopped"
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        audioManager.abandonAudioFocus(audioFocusListener)

        serviceScope.launch {
            currentMeetingId.takeIf { it != -1 }?.let {
                meetingRepository.updateMeetingStatus(it, "Stopped", System.currentTimeMillis())
            }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ─── Audio Chunking Loop ─────────────────────────────────────

    private suspend fun recordAudioChunks() {
        val shortsPerChunk = SAMPLE_RATE * 30
        val overlapShorts = SAMPLE_RATE * 2
        val advanceShorts = shortsPerChunk - overlapShorts

        var buffer = ShortArray(shortsPerChunk)
        var bufferIndex = 0
        val readBuffer = ShortArray(bufferSize / 2)

        while (isRecording && !isPaused &&
            audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {

            // Periodic storage check
            if (!hasEnoughStorage()) {
                withContext(Dispatchers.Main) {
                    updateNotification("Recording stopped - Low storage")
                }
                stopRecording()
                return
            }

            val readCount = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: 0
            if (readCount > 0) {
                // Silence detection
                val maxAmplitude = readBuffer.take(readCount).maxOf { Math.abs(it.toInt()) }
                if (maxAmplitude < SILENCE_THRESHOLD) {
                    silentReadCount++
                    val silentSeconds = silentReadCount / readsPerSecond
                    if (silentSeconds >= SILENCE_WARNING_SECONDS) {
                        withContext(Dispatchers.Main) {
                            updateNotification("⚠ No audio detected - Check microphone")
                        }
                    }
                } else {
                    if (silentReadCount / readsPerSecond >= SILENCE_WARNING_SECONDS) {
                        withContext(Dispatchers.Main) {
                            updateNotification("Recording...")
                        }
                    }
                    silentReadCount = 0
                }

                val spaceLeft = shortsPerChunk - bufferIndex
                if (readCount <= spaceLeft) {
                    System.arraycopy(readBuffer, 0, buffer, bufferIndex, readCount)
                    bufferIndex += readCount
                } else {
                    System.arraycopy(readBuffer, 0, buffer, bufferIndex, spaceLeft)
                    saveChunkAndEnqueue(buffer.clone())

                    val newBuffer = ShortArray(shortsPerChunk)
                    System.arraycopy(buffer, advanceShorts, newBuffer, 0, overlapShorts)
                    buffer = newBuffer
                    bufferIndex = overlapShorts

                    val remainingCount = readCount - spaceLeft
                    System.arraycopy(readBuffer, spaceLeft, buffer, bufferIndex, remainingCount)
                    bufferIndex += remainingCount
                }
            }
        }

        // Save last partial chunk
        if (bufferIndex > overlapShorts) {
            val finalChunk = ShortArray(bufferIndex)
            System.arraycopy(buffer, 0, finalChunk, 0, bufferIndex)
            saveChunkAndEnqueue(finalChunk)
        }
    }

    private suspend fun saveChunkAndEnqueue(audioData: ShortArray) {
        if (currentMeetingId == -1) return

        val dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val file = File(dir, "meeting_${currentMeetingId}_chunk_${chunkIndex}.wav")

        FileOutputStream(file).use { out ->
            val pcmBytes = ByteArray(audioData.size * 2)
            for (i in audioData.indices) {
                pcmBytes[i * 2] = (audioData[i].toInt() and 0x00FF).toByte()
                pcmBytes[i * 2 + 1] = (audioData[i].toInt() shr 8).toByte()
            }
            // Write WAV header
            val wavHeader = createWavHeader(pcmBytes.size)
            out.write(wavHeader)
            out.write(pcmBytes)
        }

        val chunkEntity = AudioChunkEntity(
            meetingId = currentMeetingId,
            chunkIndex = chunkIndex,
            filePath = file.absolutePath
        )
        val chunkId = audioChunkDao.insert(chunkEntity).toInt()
        chunkIndex++

        val workRequest = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setInputData(workDataOf(TranscriptionWorker.KEY_CHUNK_ID to chunkId))
            .build()
        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }

    // ─── Timer Update Loop ───────────────────────────────────────

    private suspend fun updateTimerLoop() {
        while (isRecording) {
            delay(1000)
            if (!isPaused && isRecording) {
                val elapsed = elapsedBeforePause + (System.currentTimeMillis() - recordingStartTime)
                val seconds = (elapsed / 1000) % 60
                val minutes = (elapsed / 1000) / 60
                val timerText = String.format("%02d:%02d", minutes, seconds)
                withContext(Dispatchers.Main) {
                    updateNotification("Recording... $timerText")
                }
                // Update shared state for UI
                _elapsedTime.value = elapsed
                _statusText.value = if (isPaused) "Paused - $pauseReason" else "Recording"
            }
        }
    }

    // ─── Edge Case Handlers ──────────────────────────────────────

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING,
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    if (isRecording && !isPaused) pauseRecording("Phone call")
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    if (isRecording && isPaused && pauseReason == "Phone call") {
                        resumeRecording()
                    }
                }
            }
        }
    }

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (isRecording && !isPaused) pauseRecording("Audio focus lost")
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (isRecording && isPaused && pauseReason == "Audio focus lost") {
                    resumeRecording()
                }
            }
        }
    }

    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                AudioManager.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    val name = if (state == 1) "Wired headset connected" else "Wired headset disconnected"
                    if (isRecording && !isPaused) {
                        updateNotification("Recording... ($name)")
                        // Continue recording — the OS handles mic source switch
                    }
                }
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1)
                    val name = if (state == BluetoothHeadset.STATE_CONNECTED)
                        "Bluetooth headset connected" else "Bluetooth headset disconnected"
                    if (isRecording && !isPaused) {
                        updateNotification("Recording... ($name)")
                    }
                }
            }
        }
    }

    private fun registerHeadsetReceiver() {
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        registerReceiver(headsetReceiver, filter)
    }

    // ─── Storage Check ───────────────────────────────────────────

    private fun hasEnoughStorage(): Boolean {
        val dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return false
        val stat = StatFs(dir.absolutePath)
        val availableMB = stat.availableBytes / (1024 * 1024)
        return availableMB > MIN_STORAGE_MB
    }

    // ─── Notifications ───────────────────────────────────────────

    private fun updateNotification(statusText: String) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(statusText, isRecording && !isPaused))
    }

    private fun buildNotification(statusText: String, isRecording: Boolean = true): Notification {
        val stopIntent = Intent(this, RecordingForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VoiceBrief Meeting")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (isRecording) {
            val pauseIntent = Intent(this, RecordingForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePending = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePending)
        } else {
            // Paused state → show Resume
            val resumeIntent = Intent(this, RecordingForegroundService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePending = PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePending)
        }

        builder.addAction(android.R.drawable.ic_delete, "Stop", stopPending)
        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Meeting Recording", NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createWavHeader(pcmDataSize: Int): ByteArray {
        val totalSize = pcmDataSize + 36
        val channels = 1
        val bitsPerSample = 16
        val byteRate = SAMPLE_RATE * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        val header = ByteArray(44)
        // RIFF header
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        // File size - 8
        writeInt(header, 4, totalSize)
        // WAVE
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        // fmt subchunk
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        writeInt(header, 16, 16) // Subchunk1Size (PCM = 16)
        writeShort(header, 20, 1) // AudioFormat (PCM = 1)
        writeShort(header, 22, channels)
        writeInt(header, 24, SAMPLE_RATE)
        writeInt(header, 28, byteRate)
        writeShort(header, 32, blockAlign)
        writeShort(header, 34, bitsPerSample)
        // data subchunk
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        writeInt(header, 40, pcmDataSize)
        return header
    }

    private fun writeInt(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value shr 8) and 0xFF).toByte()
        data[offset + 2] = ((value shr 16) and 0xFF).toByte()
        data[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun writeShort(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }
}
