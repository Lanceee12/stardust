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
import java.util.UUID

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

    suspend fun replacement(mode: String, interests: List<String>, excludedTitles: Set<String>): JourneyActivity {
        val apiKey = System.getenv("OPENAI_API_KEY")?.takeIf(String::isNotBlank)
        if (apiKey != null) {
            val generated = runCatching {
                val prompt = "Create one warm, specific, low-pressure ${if (mode == "friend") "friendship" else "dating"} activity. Keep it safe in public places, accessible, low-cost, and doable in one or two hours. Consider these interests: ${interests.joinToString()}. Do not repeat any of these ideas: ${excludedTitles.joinToString("; ")}. Return JSON only as {\"title\":\"...\",\"detail\":\"...\"}."
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
                json.decodeFromString<GeneratedActivityIdea>(content)
            }.getOrNull()
            if (generated != null && generated.title.isNotBlank() && generated.title !in excludedTitles) {
                return JourneyActivity("${mode}-${UUID.randomUUID()}", generated.title, generated.detail)
            }
        }
        return fallbackReplacement(mode, interests, excludedTitles)
    }

    private fun fallbackReplacement(mode: String, interests: List<String>, excludedTitles: Set<String>): JourneyActivity {
        val focus = interests.firstOrNull() ?: "something new"
        val options = if (mode == "friend") listOf(
            "Try a neighborhood photo walk" to "Pick a relaxed public route and take turns choosing a detail worth photographing.",
            "Share a favorite local snack" to "Choose a casual public market and introduce each other to a small treat you enjoy.",
            "Trade tiny creative challenges" to "Try a playful ten-minute sketch, playlist, or word game inspired by $focus.",
            "Explore a free community event" to "Find a welcoming public event and each pick one thing to check out together.",
            "Plan a two-person trivia round" to "Choose a topic you both like and make a few lighthearted questions for each other."
        ) else listOf(
            "Build a mini date playlist" to "Choose three songs each around $focus and share the story behind one pick.",
            "Visit a public garden" to "Take a relaxed walk and each choose a favorite spot to stop and talk.",
            "Pick a bookstore question" to "Browse a local bookshop and choose a cover that reminds you of a good memory.",
            "Try a low-key dessert stop" to "Meet at a public cafe and each choose a treat for the other to try.",
            "Trade travel daydreams" to "Sketch out a pretend one-day trip and compare what you would both include."
        )
        val available = options.filterNot { it.first in excludedTitles }.ifEmpty { options }
        val (title, detail) = available.random()
        return JourneyActivity("${mode}-${UUID.randomUUID()}", title, detail)
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