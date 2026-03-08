package com.example.voicebrief.data.repository

import com.example.voicebrief.data.local.dao.AudioChunkDao
import com.example.voicebrief.data.local.dao.MeetingDao
import com.example.voicebrief.data.local.entities.MeetingEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingRepository @Inject constructor(
    private val meetingDao: MeetingDao,
    private val audioChunkDao: AudioChunkDao
) {
    fun getAllMeetings(): Flow<List<MeetingEntity>> {
        return meetingDao.getAllMeetings()
    }

    suspend fun getMeetingById(id: Int): MeetingEntity? {
        return meetingDao.getMeetingById(id)
    }

    suspend fun createMeeting(startTime: Long): Int {
        val newMeeting = MeetingEntity(
            startTime = startTime,
            endTime = null,
            status = "Recording"
        )
        return meetingDao.insert(newMeeting).toInt()
    }

    suspend fun updateMeetingStatus(id: Int, status: String, endTime: Long? = null) {
        if (endTime != null) {
            meetingDao.updateMeetingStatus(id, status, endTime)
        } else {
            meetingDao.updateStatus(id, status)
        }
    }
}
