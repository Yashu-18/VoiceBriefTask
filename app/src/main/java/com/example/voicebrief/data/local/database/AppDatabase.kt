package com.example.voicebrief.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.voicebrief.data.local.dao.AudioChunkDao
import com.example.voicebrief.data.local.dao.MeetingDao
import com.example.voicebrief.data.local.dao.SummaryDao
import com.example.voicebrief.data.local.dao.TranscriptDao
import com.example.voicebrief.data.local.entities.AudioChunkEntity
import com.example.voicebrief.data.local.entities.MeetingEntity
import com.example.voicebrief.data.local.entities.SummaryEntity
import com.example.voicebrief.data.local.entities.TranscriptEntity

@Database(
    entities = [
        MeetingEntity::class,
        AudioChunkEntity::class,
        TranscriptEntity::class,
        SummaryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val meetingDao: MeetingDao
    abstract val audioChunkDao: AudioChunkDao
    abstract val transcriptDao: TranscriptDao
    abstract val summaryDao: SummaryDao
}
