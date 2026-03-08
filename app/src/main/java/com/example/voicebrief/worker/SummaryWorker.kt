package com.example.voicebrief.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.voicebrief.data.repository.MeetingRepository
import com.example.voicebrief.data.repository.SummaryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SummaryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val summaryRepository: SummaryRepository,
    private val meetingRepository: MeetingRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val meetingId = inputData.getInt(KEY_MEETING_ID, -1)
        if (meetingId == -1) {
            return Result.failure()
        }



        // Update status to processing
        meetingRepository.updateMeetingStatus(meetingId, "Processing")

        val success = summaryRepository.generateSummary(meetingId)

        return if (success) {
            meetingRepository.updateMeetingStatus(meetingId, "Completed")
            Result.success()
        } else {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                meetingRepository.updateMeetingStatus(meetingId, "Failed Summary")
                Result.failure()
            }
        }
    }

    companion object {
        const val TAG = "SummaryWorker"
        const val KEY_MEETING_ID = "MEETING_ID"
    }
}
