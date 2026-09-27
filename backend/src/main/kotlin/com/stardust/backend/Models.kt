
package com.stardust.backend

import kotlinx.serialization.Serializable

data class RegisteredAccountProfile(val accountId: Long, val profile: UserProfile)

@Serializable
data class Profile(
    val id: String,
    val name: String,
    val age: Int,
    val gender: String,
    val sexuality: String,
    val bio: String,
    val photoUrl: String,
    val interests: List<String>
)

@Serializable
data class JourneyActivity(val id: String = "", val title: String, val detail: String, val done: Boolean = false)

@Serializable
data class Journey(
    val id: String,
    val partnerName: String,
    val activities: List<JourneyActivity>,
    val outcome: String? = null,
    val partnerId: String = "",
    val mode: String = "dating",
    val feedback: Map<String, String> = emptyMap()
)

@Serializable
data class JourneySummary(val partnerId: String, val partnerName: String, val mode: String, val completedActivities: Int, val totalActivities: Int, val outcome: String? = null)

@Serializable
data class SwipeNotification(val id: Long, val actorName: String, val chatId: String, val createdAt: Long)

@Serializable
data class SwipeRequest(val profileId: String, val accepted: Boolean)

@Serializable
data class CompletionRequest(val feeling: String)

@Serializable
data class OutcomeRequest(val outcome: String, val partnerId: String = "")

@Serializable
data class ChatMessage(val id: String, val text: String, val mine: Boolean, val time: String)

@Serializable
data class ChatPreview(val id: String, val name: String, val photoUrl: String, val lastMessage: String, val streak: Int)

@Serializable
data class SendMessageRequest(val text: String)

@Serializable
data class CommentRequest(val text: String)

@Serializable
data class UserProfile(
    val name: String,
    val age: Int,
    val gender: String,
    val sexuality: String,
    val ageRange: String,
    val interests: List<String>,
    val relationshipLocked: Boolean = false,
    val birthDate: String = "",
    val location: String = "",
    val phoneNumber: String = "",
    val email: String = ""
)

@Serializable
data class StoredJourney(
    val mode: String,
    val activities: List<JourneyActivity>,
    val outcome: String? = null,
    val feedback: Map<String, String> = emptyMap()
)

@Serializable
data class PersistedUserState(
    val profile: UserProfile,
    val committedPartnerId: String? = null,
    val acceptedProfiles: Set<String> = emptySet(),
    val journeys: Map<String, StoredJourney> = emptyMap(),
    val messages: Map<String, List<ChatMessage>> = emptyMap(),
    val streaks: Map<String, Int> = emptyMap(),
    val lastChatDates: Map<String, String> = emptyMap(),
    val comments: Map<String, List<String>> = emptyMap()
)

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val gender: String,
    val birthDate: String,
    val location: String,
    val phoneNumber: String,
    val sexuality: String = "Straight",
    val ageRange: String = "26-34",
    val interests: List<String> = emptyList()
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthSession(val token: String, val profile: UserProfile)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ForgotPasswordResponse(val message: String, val demoCode: String? = null)

@Serializable
data class ResetPasswordRequest(val email: String, val code: String, val newPassword: String)

@Serializable
data class ProfileUpdate(
    val name: String,
    val age: Int,
    val gender: String,
    val sexuality: String,
    val ageRange: String,
    val interests: List<String>,
    val location: String = "",
    val birthDate: String = "",
    val phoneNumber: String = ""
)

@Serializable
data class JourneyGenerateRequest(val mode: String, val partnerId: String = "")

@Serializable
data class GeneratedActivities(val activities: List<JourneyActivity>)

@Serializable
data class GeneratedActivityIdea(val title: String, val detail: String)

@Serializable
data class ApiMessage(val message: String)