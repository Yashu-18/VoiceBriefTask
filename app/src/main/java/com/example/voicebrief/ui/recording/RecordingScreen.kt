package com.example.voicebrief.ui.recording

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicebrief.viewmodel.RecordingUiState
import com.example.voicebrief.viewmodel.RecordingViewModel

@Composable
fun RecordingScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val elapsed by viewModel.elapsedTime.collectAsState()
    val statusText by viewModel.statusText.collectAsState()

    val seconds = (elapsed / 1000) % 60
    val minutes = (elapsed / 1000) / 60
    val timerText = String.format("%02d:%02d", minutes, seconds)

    // Pulsing animation for recording indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val isActiveRecording = uiState is RecordingUiState.Recording
    val indicatorColor by animateColorAsState(
        targetValue = when (uiState) {
            is RecordingUiState.Recording -> Color(0xFFEF4444) // Red
            is RecordingUiState.Paused -> Color(0xFFF59E0B) // Yellow/Amber
            else -> Color(0xFF6B7280) // Gray
        },
        label = "indicatorColor"
    )

    val backgroundColor = Color(0xFF111827) // Dark background

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Status text
        Text(
            text = statusText,
            color = indicatorColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Pulsing recording indicator
        Box(
            modifier = Modifier
                .size(20.dp)
                .scale(if (isActiveRecording) pulseScale else 1f)
                .clip(CircleShape)
                .background(indicatorColor)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Large timer
        Text(
            text = timerText,
            color = Color.White,
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Subtitle
        Text(
            text = when (uiState) {
                is RecordingUiState.Idle -> "Tap Record to start"
                is RecordingUiState.Recording -> "Meeting in progress..."
                is RecordingUiState.Paused -> "Recording paused"
                is RecordingUiState.Stopped -> "Meeting ended"
            },
            color = Color(0xFF9CA3AF),
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Control buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 48.dp)
        ) {
            when (uiState) {
                is RecordingUiState.Idle -> {
                    // Large record button
                    Button(
                        onClick = { viewModel.startRecording() },
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
                is RecordingUiState.Recording -> {
                    // Pause button
                    OutlinedButton(
                        onClick = { viewModel.pauseRecording() },
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("⏸", fontSize = 20.sp)
                    }

                    // Stop button (large, red square inside circle)
                    Button(
                        onClick = { viewModel.stopRecording() },
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White)
                        )
                    }
                }
                is RecordingUiState.Paused -> {
                    // Resume button
                    OutlinedButton(
                        onClick = { viewModel.resumeRecording() },
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("▶", fontSize = 20.sp)
                    }

                    // Stop button
                    Button(
                        onClick = { viewModel.stopRecording() },
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White)
                        )
                    }
                }
                is RecordingUiState.Stopped -> {
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Back to Dashboard", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
            }
        }
    }
}
