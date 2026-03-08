package com.example.voicebrief.di

import android.content.Context
import androidx.room.Room
import com.example.voicebrief.data.local.dao.AudioChunkDao
import com.example.voicebrief.data.local.dao.MeetingDao
import com.example.voicebrief.data.local.dao.SummaryDao
import com.example.voicebrief.data.local.dao.TranscriptDao
import com.example.voicebrief.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "voicebrief_db"
        ).build()
    }

    @Provides
    fun provideMeetingDao(appDatabase: AppDatabase): MeetingDao {
        return appDatabase.meetingDao
    }

    @Provides
    fun provideAudioChunkDao(appDatabase: AppDatabase): AudioChunkDao {
        return appDatabase.audioChunkDao
    }

    @Provides
    fun provideTranscriptDao(appDatabase: AppDatabase): TranscriptDao {
        return appDatabase.transcriptDao
    }

    @Provides
    fun provideSummaryDao(appDatabase: AppDatabase): SummaryDao {
        return appDatabase.summaryDao
    }
}
