package com.stardust.app

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class StardustApi {
    @Volatile private var sessionToken: String? = null

    private val client = HttpClient(Android) {
        install(DefaultRequest) {
            sessionToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val baseUrl = "http://10.0.2.2:8080/api"

    fun setSessionToken(token: String?) {
        sessionToken = token
    }

    suspend fun register(request: RegisterRequest): AuthSession = client.post("$baseUrl/auth/register") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }.body()

    suspend fun login(request: LoginRequest): AuthSession = client.post("$baseUrl/auth/login") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }.body()

    suspend fun requestPasswordReset(email: String): ForgotPasswordResponse = client.post("$baseUrl/auth/forgot-password") {
        contentType(ContentType.Application.Json)
        setBody(ForgotPasswordRequest(email))
    }.body()

    suspend fun resetPassword(request: ResetPasswordRequest): ApiMessage = client.post("$baseUrl/auth/reset-password") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }.body()

    suspend fun logout() {
        client.delete("$baseUrl/auth/session")
        sessionToken = null
    }

    suspend fun profiles(mode: String): List<MatchProfile> = client.get("$baseUrl/profiles") {
        parameter("mode", mode)
    }.body()

    suspend fun journeys(): List<JourneySummary> = client.get("$baseUrl/journeys").body()

    suspend fun journey(partnerId: String? = null): Journey = client.get("$baseUrl/journey") {
        partnerId?.let { parameter("partnerId", it) }
    }.body()

    suspend fun generateJourney(mode: String, partnerId: String = ""): Journey = client.post("$baseUrl/journey/generate") {
        contentType(ContentType.Application.Json)
        setBody(JourneyGenerateRequest(mode, partnerId))
    }.body()

    suspend fun submitSwipe(profileId: String, accepted: Boolean) {
        client.post("$baseUrl/swipes") {
            contentType(ContentType.Application.Json)
            setBody(SwipeRequest(profileId, accepted))
        }
    }

    suspend fun completeActivity(id: String, feeling: String, partnerId: String = "") {
        client.post("$baseUrl/journey/$id/complete") {
            if (partnerId.isNotBlank()) parameter("partnerId", partnerId)
            contentType(ContentType.Application.Json)
            setBody(CompletionRequest(feeling))
        }
    }

    suspend fun refreshActivity(id: String, partnerId: String): Journey = client.post("$baseUrl/journey/$id/refresh") {
        parameter("partnerId", partnerId)
    }.body()

    suspend fun submitOutcome(outcome: String, partnerId: String = ""): Journey = client.post("$baseUrl/journey/outcome") {
        contentType(ContentType.Application.Json)
        setBody(OutcomeRequest(outcome, partnerId))
    }.body()

    suspend fun likes(): List<MatchProfile> = client.get("$baseUrl/likes").body()

    suspend fun comment(profileId: String, text: String) {
        client.post("$baseUrl/likes/$profileId") {
            contentType(ContentType.Application.Json)
            setBody(CommentRequest(text))
        }
    }

    suspend fun chats(): List<ChatPreview> = client.get("$baseUrl/chats").body()

    suspend fun notifications(): List<SwipeNotification> = client.get("$baseUrl/notifications").body()

    suspend fun markNotificationRead(id: Long) {
        client.post("$baseUrl/notifications/$id/read")
    }

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