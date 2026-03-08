package com.example.voicebrief.data.repository

import android.content.Context
import android.util.Log
import com.example.voicebrief.BuildConfig
import com.example.voicebrief.data.local.dao.AudioChunkDao
import com.example.voicebrief.data.local.dao.TranscriptDao
import com.example.voicebrief.data.local.entities.TranscriptEntity
import com.example.voicebrief.worker.TranscriptionWorker
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioChunkDao: AudioChunkDao,
    private val transcriptDao: TranscriptDao
) {
    companion object {
        private const val TAG = "TranscriptionRepo"
    }

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun transcribeAudioChunk(chunkId: Int): Boolean {
        return try {
            val chunk = audioChunkDao.getChunkById(chunkId)
            if (chunk == null) {
                return false
            }
            if (chunk.isTranscribed) {
                return true
            }

            val file = File(chunk.filePath)
            if (!file.exists()) {
                return false
            }


            val audioBytes = file.readBytes()

            val response = generativeModel.generateContent(
                content {
                    blob("audio/wav", audioBytes)
                    text("Please transcribe this audio into text. Return only the spoken words, no extra formatting.")
                }
            )

            val text = response.text ?: ""
            if (text.isNotBlank()) {
                val transcriptEntity = TranscriptEntity(
                    meetingId = chunk.meetingId,
                    chunkIndex = chunk.chunkIndex,
                    transcriptText = text
                )
                transcriptDao.insert(transcriptEntity)
                audioChunkDao.markChunkTranscribed(chunk.id)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun retryAllFailedChunks(meetingId: Int) {
        val untranscribed = audioChunkDao.getUntranscribedChunks()
            .filter { it.meetingId == meetingId }
        for (chunk in untranscribed) {
            val workRequest = OneTimeWorkRequestBuilder<TranscriptionWorker>()
                .setInputData(workDataOf(TranscriptionWorker.KEY_CHUNK_ID to chunk.id))
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
