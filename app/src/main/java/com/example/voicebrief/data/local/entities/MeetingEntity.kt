package com.example.voicebrief.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,
    val endTime: Long?,
    val status: String // "Recording", "Paused", "Stopped", "Processing", "Completed"
)
