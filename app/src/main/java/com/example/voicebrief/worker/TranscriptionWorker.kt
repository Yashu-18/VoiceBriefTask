package com.example.voicebrief.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.voicebrief.data.repository.TranscriptionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transcriptionRepository: TranscriptionRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val chunkId = inputData.getInt(KEY_CHUNK_ID, -1)
        if (chunkId == -1) {
            return Result.failure()
        }

        // Wait for network
        if (!isNetworkAvailable()) {
            return Result.retry()
        }

        // Add backoff delay based on attempt count to avoid rate limiting
        if (runAttemptCount > 0) {
            val backoffMs = (runAttemptCount * 15_000L).coerceAtMost(60_000L)
            delay(backoffMs)
        }
        
        val success = transcriptionRepository.transcribeAudioChunk(chunkId)
        
        return if (success) {
            Result.success()
        } else {
            if (runAttemptCount < 5) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        const val TAG = "TranscriptionWorker"
        const val KEY_CHUNK_ID = "CHUNK_ID"
    }
}
