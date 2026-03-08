package com.example.voicebrief.data.repository

import android.util.Log
import com.example.voicebrief.BuildConfig
import com.example.voicebrief.data.local.dao.SummaryDao
import com.example.voicebrief.data.local.dao.TranscriptDao
import com.example.voicebrief.data.local.entities.SummaryEntity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryRepository @Inject constructor(
    private val transcriptDao: TranscriptDao,
    private val summaryDao: SummaryDao
) {
    companion object {
        private const val TAG = "SummaryRepo"
    }

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun generateSummary(meetingId: Int): Boolean {
        return try {
            val transcripts = transcriptDao.getTranscriptsForMeeting(meetingId)

            if (transcripts.isEmpty()) {
                return false
            }

            val fullTranscript = transcripts.joinToString(" ") { it.transcriptText }

            val prompt = """
                You are an expert meeting assistant. Please analyze the following meeting transcript.
                Return your response strictly in JSON format with the following keys:
                - title (string): A short, descriptive title for the meeting.
                - summary (string): A 2-3 sentence overview of the meeting.
                - actionItems (string): A bulleted list of action items / tasks assigned. If none, write "None".
                - keyPoints (string): A bulleted list of the main topics discussed.
                
                Transcript:
                $fullTranscript
            """.trimIndent()



            val response = generativeModel.generateContent(
                content { text(prompt) }
            )

            val responseText = response.text ?: ""

            // Clean up the JSON response (Gemini sometimes wraps with ```json)
            val jsonString = responseText.replace("```json", "").replace("```", "").trim()
            val jsonObject = JSONObject(jsonString)

            val summaryEntity = SummaryEntity(
                meetingId = meetingId,
                title = jsonObject.optString("title", "Untitled Meeting"),
                summary = jsonObject.optString("summary", "No summary available."),
                actionItems = jsonObject.optString("actionItems", "None"),
                keyPoints = jsonObject.optString("keyPoints", "No key points.")
            )

            summaryDao.insert(summaryEntity)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getSummaryFlow(meetingId: Int) = summaryDao.getSummaryFlow(meetingId)
}
