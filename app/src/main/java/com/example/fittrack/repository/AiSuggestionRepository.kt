package com.example.fittrack.repository

import com.example.fittrack.BuildConfig
import com.example.fittrack.model.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Produces short coaching feedback after a session.
 * The app can call an external LLM if an API key exists, but it also has a local
 * fallback so the Pro feature works during classroom demonstrations without internet/API billing.
 */
class AiSuggestionRepository {

    /**
     * Returns a coaching suggestion for the finished session.
     * O bloqueio Pro é feito pela UI/ViewModel usando o perfil Firestore do utilizador atual.
     * Se não existir uma API externa configurada, a app devolve uma sugestão local útil.
     */
    suspend fun getSuggestion(session: Session): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.AI_API_KEY
            if (apiKey.isBlank() || apiKey == "YOUR_API_KEY_HERE") {
                return@withContext Result.success(buildLocalSuggestion(session))
            }

            val responseText = callAnthropicApi(apiKey, buildPrompt(session))
            Result.success(responseText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(session: Session): String {
        val minutes = session.durationSeconds / 60
        return """You are a personal fitness coach. A user just finished a workout:
- Duration: $minutes minutes
- Total volume: ${"%.1f".format(session.totalVolume)} kg
- Sets completed: ${session.totalSets}
- Total reps: ${session.totalReps}
- Exercises: ${session.exercisesCompleted}
- Personal records: ${session.personalRecords}
- Plan: ${session.planName.ifEmpty { "Quick workout" }}

Give a short motivating coaching tip (2-3 sentences). Be specific and actionable. No markdown."""
    }


    private fun buildLocalSuggestion(session: Session): String {
        val minutes = session.durationSeconds / 60
        return when {
            session.personalRecords > 0 -> "Great session: you hit ${session.personalRecords} personal record(s). Keep the same movement quality next time and only increase the load slightly."
            session.totalVolume > 0 && minutes >= 30 -> "Strong volume today. For the next workout, try to keep the same total volume with slightly cleaner rest times or add one controlled set to your main exercise."
            else -> "Good job finishing the workout. Build consistency first: repeat this plan this week and try to add a few reps or a small amount of weight."
        }
    }

    private fun callAnthropicApi(apiKey: String, prompt: String): String {
        val connection = (URL("https://api.anthropic.com/v1/messages")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 30_000
        }

        try {
            val body = JSONObject().apply {
                put("model", "claude-haiku-4-5-20251001")
                put("max_tokens", 200)
                put("messages", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                }))
            }

            connection.outputStream.use { it.write(body.toString().toByteArray()) }

            val code = connection.responseCode
            val responseText = if (code in 200..299) {
                connection.inputStream.bufferedReader().readText()
            } else {
                val err = connection.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                throw Exception("API error $code: $err")
            }

            return JSONObject(responseText)
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
                .trim()
        } finally {
            connection.disconnect()
        }
    }
}