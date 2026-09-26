package com.stardust.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object JourneyPlanner {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(CIO) {
        install(HttpTimeout) { requestTimeoutMillis = 25_000 }
    }

    suspend fun generate(mode: String, interests: List<String>): List<JourneyActivity> {
        val apiKey = System.getenv("OPENAI_API_KEY")?.takeIf(String::isNotBlank) ?: return fallback(mode, interests)
        return runCatching {
            val prompt = "Create exactly 3 warm, specific, low-pressure ${if (mode == "friend") "friendship" else "first-date"} activities. Keep them safe in public places, accessible, low-cost, and doable in one or two hours. Consider these shared interests: ${interests.joinToString()}. Return JSON only in the shape {\"activities\":[{\"title\":\"...\",\"detail\":\"...\"}]}"
            val response = client.post("https://api.openai.com/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(buildJsonObject {
                    put("model", System.getenv("OPENAI_MODEL") ?: "gpt-4o-mini")
                    put("response_format", buildJsonObject { put("type", "json_object") })
                    put("messages", buildJsonArray {
                        add(buildJsonObject { put("role", "user"); put("content", prompt) })
                    })
                }))
            }
            val body = json.parseToJsonElement(response.bodyAsText())
            val content = body.jsonObject["choices"]!!.jsonArray[0].jsonObject["message"]!!.jsonObject["content"]!!.jsonPrimitive.content
            val generated = json.decodeFromString<GeneratedActivities>(content).activities
            require(generated.size == 3)
            generated.mapIndexed { index, activity -> activity.copy(id = "${mode}-${index + 1}", done = false) }
        }.getOrElse { fallback(mode, interests) }
    }

    private fun fallback(mode: String, interests: List<String>): List<JourneyActivity> {
        val focus = interests.firstOrNull() ?: "something new"
        val options = if (mode == "friend") listOf(
            "Trade favorite local finds" to "Each choose a welcoming spot tied to $focus, then compare what you discovered.",
            "Make a tiny team playlist" to "Take turns adding songs and share the places, people, or memories behind them.",
            "Plan a no-pressure mini adventure" to "Pick a free public event or park walk and let each person choose one small detour."
        ) else listOf(
            "Meet over a favorite drink" to "Choose a relaxed public cafe and share the story behind your go-to order.",
            "Take the scenic route" to "Pick an easy public walk and each choose one interesting stop along the way.",
            "Swap three-song soundtracks" to "Share songs connected to $focus and tell each other why they made the list."
        )
        return options.mapIndexed { index, (title, detail) -> JourneyActivity("${mode}-${index + 1}", title, detail) }
    }
}