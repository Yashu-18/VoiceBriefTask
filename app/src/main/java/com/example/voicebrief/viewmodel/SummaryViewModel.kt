package com.example.voicebrief.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicebrief.data.local.entities.SummaryEntity
import com.example.voicebrief.data.repository.SummaryRepository
import com.example.voicebrief.data.repository.TranscriptionRepository
import com.example.voicebrief.worker.SummaryWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val summaryRepository: SummaryRepository,
    private val transcriptionRepository: TranscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SummaryUiState>(SummaryUiState.Loading)
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    private var currentMeetingId: Int = -1
    private var summaryTriggered = false

    fun loadSummary(meetingId: Int) {
        currentMeetingId = meetingId
        viewModelScope.launch {
            // First check if summary already exists
            summaryRepository.getSummaryFlow(meetingId).collect { summary ->
                if (summary != null) {
                    _uiState.value = SummaryUiState.Success(summary)
                } else if (!summaryTriggered) {
                    // No summary yet — trigger generation
                    summaryTriggered = true
                    triggerSummaryGeneration(meetingId)
                }
            }
        }
    }

    private fun triggerSummaryGeneration(meetingId: Int) {
        
        val workRequest = OneTimeWorkRequestBuilder<SummaryWorker>()
            .setInputData(workDataOf(SummaryWorker.KEY_MEETING_ID to meetingId))
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)

        // Also observe work status for error reporting
        viewModelScope.launch {
            // Give it a timeout — if still loading after 60s, show error
            delay(60_000)
            if (_uiState.value is SummaryUiState.Loading) {
                _uiState.value = SummaryUiState.Error("Summary generation timed out. Transcripts may not be ready yet.")
            }
        }
    }

    fun retrySummary() {
        if (currentMeetingId == -1) return
        _uiState.value = SummaryUiState.Loading
        summaryTriggered = false

        val workRequest = OneTimeWorkRequestBuilder<SummaryWorker>()
            .setInputData(workDataOf(SummaryWorker.KEY_MEETING_ID to currentMeetingId))
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)

        // Re-observe
        viewModelScope.launch {
            summaryRepository.getSummaryFlow(currentMeetingId).collect { summary ->
                if (summary != null) {
                    _uiState.value = SummaryUiState.Success(summary)
                }
            }
        }
    }

    fun retryTranscription() {
        if (currentMeetingId == -1) return
        viewModelScope.launch {
            transcriptionRepository.retryAllFailedChunks(currentMeetingId)
        }
    }
}

sealed class SummaryUiState {
    object Loading : SummaryUiState()
    data class Success(val summary: SummaryEntity) : SummaryUiState()
    data class Error(val message: String) : SummaryUiState()
}
