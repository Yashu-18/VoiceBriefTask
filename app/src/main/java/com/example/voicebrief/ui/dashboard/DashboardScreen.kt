package com.example.voicebrief.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicebrief.data.local.entities.MeetingEntity
import com.example.voicebrief.viewmodel.DashboardUiState
import com.example.voicebrief.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSummary: (Int) -> Unit,
    onNavigateToRecording: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val backgroundColor = Color(0xFF111827)
    val cardColor = Color(0xFF1F2937)
    val accentColor = Color(0xFF3B82F6)

    Scaffold(
        containerColor = backgroundColor,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToRecording,
                containerColor = accentColor,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Text("+", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Light)
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text("VoiceBrief", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 24.sp)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Your Meetings",
                color = Color(0xFF9CA3AF),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
            )

            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentColor)
                    }
                }
                is DashboardUiState.Success -> {
                    if (state.meetings.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎙️", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No meetings yet", color = Color(0xFF9CA3AF), fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tap + to record your first meeting",
                                    color = Color(0xFF6B7280), fontSize = 14.sp)
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.meetings) { meeting ->
                                MeetingCard(
                                    meeting = meeting,
                                    cardColor = cardColor,
                                    onClick = { onNavigateToSummary(it) }
                                )
                            }
                        }
                    }
                }
                is DashboardUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}", color = Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}

@Composable
fun MeetingCard(meeting: MeetingEntity, cardColor: Color, onClick: (Int) -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault())
    val startDate = dateFormat.format(Date(meeting.startTime))
    val durationMs = (meeting.endTime ?: System.currentTimeMillis()) - meeting.startTime
    val durationSec = (durationMs / 1000).toInt()
    val durationText = if (durationSec < 60) "${durationSec} sec" else "${durationSec / 60} min"

    val statusColor = when (meeting.status) {
        "Completed" -> Color(0xFF10B981)
        "Recording" -> Color(0xFFEF4444)
        "Processing" -> Color(0xFFF59E0B)
        "Stopped" -> Color(0xFF6B7280)
        else -> Color(0xFF6B7280)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(meeting.id) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Recording icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🎙️", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Meeting ${meeting.id}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = startDate,
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                // Status chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = meeting.status,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = durationText,
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp
                )
            }
        }
    }
}
