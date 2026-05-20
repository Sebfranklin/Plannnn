package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.CalendarEvent
import com.example.data.model.Goal
import com.example.data.model.Task
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- Moshi Mapped DTOs for Gemini API ---

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiPlannerService {

    suspend fun generateDynamicDailyPlan(
        events: List<CalendarEvent>,
        tasks: List<Task>,
        goals: List<Goal>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API_KEY_MISSING"
        }

        // Build a detailed planning prompt
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val eventsStr = if (events.isEmpty()) {
            "- No scheduled calendar events today."
        } else {
            events.joinToString("\n") { 
                val start = sdf.format(Date(it.startTime))
                val end = sdf.format(Date(it.endTime))
                "- Event: ${it.title} ($start - $end) | Loc: ${it.location ?: "None"} | Details: ${it.description}"
            }
        }

        val tasksStr = if (tasks.isEmpty()) {
            "- No open tasks assigned for today."
        } else {
            tasks.joinToString("\n") {
                "- Task: ${it.title} | Priority: ${it.priority} | Completed: ${it.isCompleted} | Category: ${it.category} | Details: ${it.description}"
            }
        }

        val goalsStr = if (goals.isEmpty()) {
            "- No active goals set."
        } else {
            goals.joinToString("\n") {
                "- Goal: ${it.title} | Progress: ${it.currentValue}/${it.targetValue} ${it.unit} | Category: ${it.category}"
            }
        }

        val prompt = """
            Here is my setup for the day:
            
            [CALENDAR SYNCED EVENTS]
            $eventsStr
            
            [TASKS TO COMPLETE]
            $tasksStr
            
            [ACTIVE PROGRESSIVE GOALS]
            $goalsStr
            
            Based on the above, please:
            1. Suggest a **Dynamic Daily Agenda** (hour-by-hour) that integrates my tasks perfectly around my calendar commitments. Avoid overlaps!
            2. Give **3 Smart Focus Recommendations** for today based on my goals. (e.g. Actionable ways/slots to advance my active goals).
            3. Highlight **Dynamic Reminders** (critical check-in times to stay on track).
            
            Format the response nicely using clear markdown sections, headers, and bullet points.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(
                parts = listOf(
                    Part(
                        text = "You are Aura, an elite daily planner and high-performance coach. " +
                               "Your tone is professional, encouraging, analytical, and highly organized. " +
                               "Always structure your output with clean headers like '📅 Dynamic Agenda', '💡 Smart Goal Focus', and '🔔 Scheduled Check-Ins' so that the user can scan it instantly on a mobile screen. Keep suggestions laser-targeted and realistic."
                    )
                )
            )
        )

        try {
            val response = GeminiClient.service.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            responseText ?: "Failed to generate plan advice. Please check again."
        } catch (e: Exception) {
            "Unable to generate AI schedule right now. Check internet connection or API keys. Error: ${e.localizedMessage}"
        }
    }
}
