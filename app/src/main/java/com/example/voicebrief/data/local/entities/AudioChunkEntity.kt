package com.example.voicebrief.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio_chunks",
    foreignKeys = [
        ForeignKey(
            entity = MeetingEntity::class,
            parentColumns = ["id"],
            childColumns = ["meetingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("meetingId")]
)
data class AudioChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val meetingId: Int,
    val chunkIndex: Int,
    val filePath: String,
    val isTranscribed: Boolean = false
)
