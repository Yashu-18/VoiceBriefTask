package com.example.voicebrief.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicebrief.data.repository.MeetingRepository
import com.example.voicebrief.service.RecordingForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val meetingRepository: MeetingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecordingUiState>(RecordingUiState.Idle)
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    // Observe shared state directly from the service companion object
    val elapsedTime: StateFlow<Long> = RecordingForegroundService.elapsedTime
    val statusText: StateFlow<String> = RecordingForegroundService.statusText

    private var currentMeetingId: Int? = null

    fun startRecording() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            currentMeetingId = meetingRepository.createMeeting(startTime)

            val intent = Intent(context, RecordingForegroundService::class.java).apply {
                action = RecordingForegroundService.ACTION_START
                putExtra(RecordingForegroundService.EXTRA_MEETING_ID, currentMeetingId)
            }
            context.startService(intent)

            _uiState.value = RecordingUiState.Recording
        }
    }

    fun pauseRecording() {
        val intent = Intent(context, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_PAUSE
        }
        context.startService(intent)
        _uiState.value = RecordingUiState.Paused
    }

    fun resumeRecording() {
        val intent = Intent(context, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_RESUME
        }
        context.startService(intent)
        _uiState.value = RecordingUiState.Recording
    }

    fun stopRecording() {
        val intent = Intent(context, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_STOP
        }
        context.startService(intent)

        viewModelScope.launch {
            currentMeetingId?.let {
                meetingRepository.updateMeetingStatus(it, "Stopped", System.currentTimeMillis())
            }
            _uiState.value = RecordingUiState.Stopped
        }
    }
}

sealed class RecordingUiState {
    object Idle : RecordingUiState()
    object Recording : RecordingUiState()
    object Paused : RecordingUiState()
    object Stopped : RecordingUiState()
}
