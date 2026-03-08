package com.example.voicebrief.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.voicebrief.data.local.entities.AudioChunkEntity

@Dao
interface AudioChunkDao {
    @Insert
    suspend fun insert(chunk: AudioChunkEntity): Long

    @Update
    suspend fun update(chunk: AudioChunkEntity)

    @Query("SELECT * FROM audio_chunks WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    suspend fun getChunksForMeeting(meetingId: Int): List<AudioChunkEntity>

    @Query("SELECT * FROM audio_chunks WHERE id = :chunkId")
    suspend fun getChunkById(chunkId: Int): AudioChunkEntity?

    @Query("SELECT * FROM audio_chunks WHERE isTranscribed = 0")
    suspend fun getUntranscribedChunks(): List<AudioChunkEntity>
    
    @Query("UPDATE audio_chunks SET isTranscribed = 1 WHERE id = :chunkId")
    suspend fun markChunkTranscribed(chunkId: Int)
}
