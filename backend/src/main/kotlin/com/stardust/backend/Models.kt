package com.stardust.backend

import kotlinx.serialization.Serializable

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
data class Journey(val id: String, val partnerName: String, val activities: List<JourneyActivity>, val outcome: String? = null)

@Serializable
data class SwipeRequest(val profileId: String, val accepted: Boolean)

@Serializable
data class CompletionRequest(val feeling: String)

@Serializable
data class OutcomeRequest(val outcome: String)

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
    val relationshipLocked: Boolean = false
)

@Serializable
data class ProfileUpdate(
    val name: String,
    val age: Int,
    val gender: String,
    val sexuality: String,
    val ageRange: String,
    val interests: List<String>
)

@Serializable
data class JourneyGenerateRequest(val mode: String)

@Serializable
data class GeneratedActivities(val activities: List<JourneyActivity>)

@Serializable
data class ApiMessage(val message: String)