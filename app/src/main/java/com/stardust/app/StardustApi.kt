package com.stardust.app

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class StardustApi {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val baseUrl = "http://10.0.2.2:8080/api"

    suspend fun profiles(mode: String): List<MatchProfile> = client.get("$baseUrl/profiles") {
        parameter("mode", mode)
    }.body()

    suspend fun journey(): Journey = client.get("$baseUrl/journey").body()

    suspend fun generateJourney(mode: String): Journey = client.post("$baseUrl/journey/generate") {
        contentType(ContentType.Application.Json)
        setBody(JourneyGenerateRequest(mode))
    }.body()

    suspend fun submitSwipe(profileId: String, accepted: Boolean) {
        client.post("$baseUrl/swipes") {
            contentType(ContentType.Application.Json)
            setBody(SwipeRequest(profileId, accepted))
        }
    }

    suspend fun completeActivity(id: String, feeling: String) {
        client.post("$baseUrl/journey/$id/complete") {
            contentType(ContentType.Application.Json)
            setBody(CompletionRequest(feeling))
        }
    }

    suspend fun submitOutcome(outcome: String) {
        client.post("$baseUrl/journey/outcome") {
            contentType(ContentType.Application.Json)
            setBody(OutcomeRequest(outcome))
        }
    }

    suspend fun likes(): List<MatchProfile> = client.get("$baseUrl/likes").body()

    suspend fun comment(profileId: String, text: String) {
        client.post("$baseUrl/likes/$profileId") {
            contentType(ContentType.Application.Json)
            setBody(CommentRequest(text))
        }
    }

    suspend fun chats(): List<ChatPreview> = client.get("$baseUrl/chats").body()

    suspend fun messages(chatId: String): List<ChatMessage> = client.get("$baseUrl/chats/$chatId/messages").body()

    suspend fun sendMessage(chatId: String, text: String): List<ChatMessage> = client.post("$baseUrl/chats/$chatId/messages") {
        contentType(ContentType.Application.Json)
        setBody(SendMessageRequest(text))
    }.body()

    suspend fun profile(): UserProfile = client.get("$baseUrl/profile").body()

    suspend fun updateProfile(profile: ProfileUpdate): UserProfile = client.put("$baseUrl/profile") {
        contentType(ContentType.Application.Json)
        setBody(profile)
    }.body()
}