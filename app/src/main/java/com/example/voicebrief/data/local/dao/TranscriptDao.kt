package com.example.voicebrief.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.voicebrief.data.local.entities.TranscriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {
    @Insert
    suspend fun insert(transcript: TranscriptEntity): Long

    @Query("SELECT * FROM transcripts WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    suspend fun getTranscriptsForMeeting(meetingId: Int): List<TranscriptEntity>

    @Query("SELECT * FROM transcripts WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    fun getTranscriptsFlow(meetingId: Int): Flow<List<TranscriptEntity>>
}
