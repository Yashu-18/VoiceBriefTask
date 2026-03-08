package com.example.voicebrief.ui.summary

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicebrief.viewmodel.SummaryUiState
import com.example.voicebrief.viewmodel.SummaryViewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    meetingId: Int,
    onNavigateBack: () -> Unit,
    viewModel: SummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val backgroundColor = Color(0xFF111827)
    val cardColor = Color(0xFF1F2937)
    val accentColor = Color(0xFF3B82F6)

    LaunchedEffect(meetingId) {
        viewModel.loadSummary(meetingId)
    }

    Scaffold(
        containerColor = backgroundColor,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Custom header with proper spacing and standard iconography
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate Back",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Meeting Summary",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            when (val state = uiState) {
                is SummaryUiState.Loading -> {
                    Spacer(modifier = Modifier.height(100.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = accentColor)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Generating summary...",
                                color = Color(0xFF9CA3AF), fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("This may take a moment",
                                color = Color(0xFF6B7280), fontSize = 13.sp)
                        }
                    }
                }

                is SummaryUiState.Success -> {
                    val s = state.summary

                    // Title section
                    SummarySection(
                        title = "Title",
                        content = s.title,
                        cardColor = cardColor
                    )

                    // Summary section
                    SummarySection(
                        title = "Summary",
                        content = s.summary,
                        cardColor = cardColor
                    )

                    // Action Items section
                    SummarySection(
                        title = "Action Items",
                        content = s.actionItems,
                        cardColor = cardColor
                    )

                    // Key Points section
                    SummarySection(
                        title = "Key Points",
                        content = s.keyPoints,
                        cardColor = cardColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                is SummaryUiState.Error -> {
                    Spacer(modifier = Modifier.height(100.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(state.message,
                                color = Color(0xFFEF4444), fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.retrySummary() },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Retry", modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummarySection(title: String, content: String, cardColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = Color(0xFF9CA3AF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = content,
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}
