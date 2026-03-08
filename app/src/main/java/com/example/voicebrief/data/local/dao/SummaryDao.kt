package com.example.voicebrief.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.voicebrief.data.local.entities.SummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: SummaryEntity)

    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    suspend fun getSummaryForMeeting(meetingId: Int): SummaryEntity?

    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    fun getSummaryFlow(meetingId: Int): Flow<SummaryEntity?>
}
