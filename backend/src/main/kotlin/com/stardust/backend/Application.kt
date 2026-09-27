package com.stardust.backend

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

private fun Profile.isInAgeRange(range: String): Boolean {
    val bounds = Regex("(\\d+)\\s*-\\s*(\\d+)").matchEntire(range.trim())?.groupValues ?: return true
    val minimum = bounds[1].toIntOrNull() ?: return true
    val maximum = bounds[2].toIntOrNull() ?: return true
    return age in minimum..maximum
}

private fun genderGroup(gender: String): String = when (gender.trim().lowercase()) {
    "male", "man" -> "man"
    "female", "woman" -> "woman"
    else -> gender.trim().lowercase()
}

private fun RegisteredAccountProfile.asCandidate(): Profile {
    val interests = profile.interests
    val location = profile.location.takeIf(String::isNotBlank)
    val bio = buildList {
        if (location != null) add("From $location.")
        if (interests.isNotEmpty()) add("Into ${interests.joinToString(", ")}.")
    }.joinToString(" ").ifBlank { "New to Stardust and open to meeting someone kind." }
    val photo = if (genderGroup(profile.gender) == "woman") {
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=900"
    } else {
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=900"
    }
    return Profile("member-$accountId", profile.name, profile.age, profile.gender, profile.sexuality, bio, photo, interests)
}

private fun isDatingMatch(viewer: UserProfile, candidate: Profile): Boolean {
    val sameOrientation = viewer.sexuality.equals(candidate.sexuality, ignoreCase = true)
    val gendersCompatible = if (viewer.sexuality.equals("Straight", ignoreCase = true)) {
        genderGroup(viewer.gender) != genderGroup(candidate.gender)
    } else {
        genderGroup(viewer.gender) != genderGroup(candidate.gender)
    }
    return sameOrientation && gendersCompatible && candidate.isInAgeRange(viewer.ageRange)
}

private fun availableProfiles(account: AuthenticatedAccount, mode: String): List<Profile> {
    val viewer = account.state.profile
    val candidates = LocalDatabase.discoverableUsers(account.id).map(RegisteredAccountProfile::asCandidate)
    return candidates.filter { candidate ->
        candidate.id != "member-${account.id}" && if (mode == "friend" || viewer.relationshipLocked) {
            genderGroup(candidate.gender) == genderGroup(viewer.gender) && candidate.sexuality.equals("Straight", ignoreCase = true)
        } else {
            isDatingMatch(viewer, candidate)
        }
    }
}

private fun findCandidate(account: AuthenticatedAccount, candidateId: String): Profile? =
    candidateId.removePrefix("member-").toLongOrNull()?.let { memberId ->
        LocalDatabase.discoverableUsers(account.id).firstOrNull { it.accountId == memberId }?.asCandidate()
    }

private fun memberAccountId(candidateId: String): Long? = candidateId.takeIf { it.startsWith("member-") }
    ?.removePrefix("member-")?.toLongOrNull()

private val defaultJourneyActivities = listOf(
    JourneyActivity("coffee", "Find your little coffee spot", "Pick a cozy cafe and trade the stories behind your favorite comfort drinks."),
    JourneyActivity("walk", "Take the scenic route", "Choose a new neighborhood walk. Each of you picks one stop along the way."),
    JourneyActivity("playlist", "Build a two-person playlist", "Add three songs each, then listen together and share the memories they bring back.")
)

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

private fun Application.module() {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    routing {
        get("/api/health") { call.respond(ApiMessage("Stardust API is online")) }
        post("/api/auth/register") {
            val request = call.receive<RegisterRequest>()
            val birthDate = runCatching { LocalDate.parse(request.birthDate) }.getOrNull()
            val age = birthDate?.let { Period.between(it, LocalDate.now()).years }
            if (request.name.isBlank() || request.name.length > 80 ||
                !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(request.email.trim()) ||
                request.password.length < 8 || request.gender !in setOf("Male", "Female", "Others") ||
                request.location.isBlank() || request.phoneNumber.isBlank() || age == null || age !in 18..100
            ) return@post call.respond(HttpStatusCode.BadRequest, ApiMessage("Check your details. You must be at least 18 and use YYYY-MM-DD for birth date."))
            try {
                call.respond(LocalDatabase.register(request))
            } catch (_: DuplicateEmailException) {
                call.respond(HttpStatusCode.Conflict, ApiMessage("An account with this email already exists"))
            }
        }
        post("/api/auth/login") {
            val request = call.receive<LoginRequest>()
            val session = LocalDatabase.login(request.email, request.password)
                ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiMessage("Email or password is incorrect"))
            call.respond(session)
        }
        post("/api/auth/forgot-password") {
            val request = call.receive<ForgotPasswordRequest>()
            val code = LocalDatabase.issueResetCode(request.email)
            call.respond(ForgotPasswordResponse("Prototype reset codes are shown here; no email is sent.", code))
        }
        post("/api/auth/reset-password") {
            val request = call.receive<ResetPasswordRequest>()
            if (request.newPassword.length < 8) return@post call.respond(HttpStatusCode.BadRequest, ApiMessage("Use at least 8 characters for the new password"))
            if (!LocalDatabase.resetPassword(request.email, request.code, request.newPassword)) {
                return@post call.respond(HttpStatusCode.BadRequest, ApiMessage("The code is invalid or expired"))
            }
            call.respond(ApiMessage("Password updated. Sign in with your new password."))
        }
        delete("/api/auth/session") {
            LocalDatabase.revokeSession(call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer "))
            call.respond(ApiMessage("Signed out"))
        }
        get("/api/profiles") {
            val account = call.requireAccount() ?: return@get
            val mode = call.request.queryParameters["mode"] ?: "dating"
            call.respond(availableProfiles(account, mode))
        }
        post("/api/swipes") {
            val account = call.requireAccount() ?: return@post
            val request = call.receive<SwipeRequest>()
            val profile = findCandidate(account, request.profileId)
                ?: return@post call.respond(HttpStatusCode.NotFound, ApiMessage("Profile not found"))
            val userProfile = account.state.profile
            if (userProfile.relationshipLocked && request.accepted && genderGroup(profile.gender) != genderGroup(userProfile.gender)) {
                return@post call.respond(HttpStatusCode.Forbidden, ApiMessage("Dating discovery is locked"))
            }
            val mode = if (userProfile.relationshipLocked) "friend" else "dating"
            val newJourney = if (request.accepted && request.profileId !in account.state.journeys) {
                StoredJourney(mode, JourneyPlanner.generate(mode, (userProfile.interests + profile.interests).distinct()))
            } else null
            val otherAccountId = memberAccountId(request.profileId)
            if (request.accepted && otherAccountId != null) {
                LocalDatabase.connectUsers(account.id, otherAccountId)
                val recipientState = LocalDatabase.accountState(otherAccountId)
                if (recipientState != null) {
                    val actorCandidateId = "member-${account.id}"
                    val recipientMode = if (recipientState.profile.relationshipLocked) "friend" else "dating"
                    val recipientJourney = recipientState.journeys[actorCandidateId] ?: StoredJourney(
                        recipientMode,
                        JourneyPlanner.generate(recipientMode, (recipientState.profile.interests + userProfile.interests).distinct())
                    )
                    LocalDatabase.updateState(otherAccountId) { current ->
                        current.copy(
                            acceptedProfiles = current.acceptedProfiles + actorCandidateId,
                            journeys = current.journeys + (actorCandidateId to recipientJourney)
                        )
                    }
                    LocalDatabase.createSwipeNotification(otherAccountId, account.id)
                }
            }
            LocalDatabase.updateState(account.id) { current ->
                val accepted = current.acceptedProfiles.toMutableSet()
                val journeys = current.journeys.toMutableMap()
                if (request.accepted) {
                    accepted.add(request.profileId)
                    if (newJourney != null) journeys.putIfAbsent(request.profileId, newJourney)
                } else accepted.remove(request.profileId)
                current.copy(acceptedProfiles = accepted, journeys = journeys)
            }
            call.respond(ApiMessage(if (request.accepted) "Like recorded" else "Passed"))
        }
        get("/api/journeys") {
            val account = call.requireAccount() ?: return@get
            call.respond(account.state.acceptedProfiles.mapNotNull { partnerId ->
                val partner = findCandidate(account, partnerId) ?: return@mapNotNull null
                val journey = account.state.journeys[partnerId] ?: return@mapNotNull null
                JourneySummary(partnerId, partner.name, journey.mode, journey.activities.count { it.done }, journey.activities.size, journey.outcome)
            })
        }
        get("/api/journey") {
            val account = call.requireAccount() ?: return@get
            val partnerId = call.request.queryParameters["partnerId"] ?: account.state.acceptedProfiles.firstOrNull()
                ?: return@get call.respond(HttpStatusCode.Conflict, ApiMessage("Connect with a match before starting a journey"))
            if (partnerId !in account.state.acceptedProfiles) return@get call.respond(HttpStatusCode.Forbidden, ApiMessage("That journey is not available"))
            val partner = findCandidate(account, partnerId)
                ?: return@get call.respond(HttpStatusCode.NotFound, ApiMessage("Profile not found"))
            val journey = account.state.journeys[partnerId]
                ?: StoredJourney(if (account.state.profile.relationshipLocked) "friend" else "dating", defaultJourneyActivities)
            call.respond(Journey("journey-$partnerId", partner.name, journey.activities, journey.outcome, partnerId, journey.mode, journey.feedback))
        }
        post("/api/journey/generate") {
            val account = call.requireAccount() ?: return@post
            val request = call.receive<JourneyGenerateRequest>()
            val mode = request.mode
            if (mode !in setOf("dating", "friend")) return@post call.respond(HttpStatusCode.BadRequest, ApiMessage("Choose dating or friend mode"))
            val partnerId = request.partnerId.ifBlank { account.state.acceptedProfiles.firstOrNull().orEmpty() }
            if (partnerId !in account.state.acceptedProfiles) return@post call.respond(HttpStatusCode.Forbidden, ApiMessage("Swipe right to start a journey"))
            if (account.state.profile.relationshipLocked && mode != "friend" && partnerId != account.state.committedPartnerId) {
                return@post call.respond(HttpStatusCode.Forbidden, ApiMessage("Dating journeys are locked"))
            }
            val partner = findCandidate(account, partnerId)
                ?: return@post call.respond(HttpStatusCode.NotFound, ApiMessage("Profile not found"))
            val journey = StoredJourney(mode, JourneyPlanner.generate(mode, (account.state.profile.interests + partner.interests).distinct()))
            val state = LocalDatabase.updateState(account.id) { current -> current.copy(journeys = current.journeys + (partnerId to journey)) }
            call.respond(Journey("journey-$partnerId", partner.name, journey.activities, journey.outcome, partnerId, journey.mode, journey.feedback))
        }
        post("/api/journey/{id}/complete") {
            val account = call.requireAccount() ?: return@post
            val id = call.parameters["id"]!!
            val partnerId = call.request.queryParameters["partnerId"] ?: account.state.acceptedProfiles.firstOrNull()
                ?: return@post call.respond(HttpStatusCode.Conflict, ApiMessage("Select a connected person's journey"))
            if (partnerId !in account.state.acceptedProfiles) return@post call.respond(HttpStatusCode.Forbidden, ApiMessage("That journey is not available"))
            val completion = call.receive<CompletionRequest>()
            if (completion.feeling !in setOf("great", "not-for-us")) return@post call.respond(HttpStatusCode.BadRequest, ApiMessage("Choose a journey check-in"))
            val state = LocalDatabase.updateState(account.id) { current ->
                val journey = current.journeys[partnerId] ?: return@updateState current
                if (journey.activities.none { it.id == id }) return@updateState current
                current.copy(journeys = current.journeys + (partnerId to journey.copy(
                    activities = journey.activities.map { if (it.id == id) it.copy(done = true) else it },
                    feedback = journey.feedback + (id to completion.feeling)
                )))
            }
            if (state.journeys[partnerId]?.activities?.any { it.id == id && it.done } != true) return@post call.respond(HttpStatusCode.NotFound, ApiMessage("Activity not found"))
            call.respond(ApiMessage("Journey check-in saved"))
        }
        post("/api/journey/{id}/refresh") {
            val account = call.requireAccount() ?: return@post
            val activityId = call.parameters["id"]!!
            val partnerId = call.request.queryParameters["partnerId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ApiMessage("Choose whose journey to refresh"))
            if (partnerId !in account.state.acceptedProfiles) return@post call.respond(HttpStatusCode.Forbidden, ApiMessage("That journey is not available"))
            val currentJourney = account.state.journeys[partnerId]
                ?: return@post call.respond(HttpStatusCode.NotFound, ApiMessage("Journey not found"))
            val activity = currentJourney.activities.firstOrNull { it.id == activityId }
                ?: return@post call.respond(HttpStatusCode.NotFound, ApiMessage("Activity not found"))
            if (activity.done) return@post call.respond(HttpStatusCode.Conflict, ApiMessage("Completed ideas cannot be skipped"))
            val partner = findCandidate(account, partnerId)
                ?: return@post call.respond(HttpStatusCode.NotFound, ApiMessage("Profile not found"))
            val replacement = JourneyPlanner.replacement(
                currentJourney.mode,
                (account.state.profile.interests + partner.interests).distinct(),
                currentJourney.activities.map { it.title }.toSet()
            )
            val savedState = LocalDatabase.updateState(account.id) { current ->
                val journey = current.journeys[partnerId] ?: return@updateState current
                current.copy(journeys = current.journeys + (partnerId to journey.copy(
                    activities = journey.activities.map { if (it.id == activityId) replacement else it },
                    feedback = journey.feedback - activityId
                )))
            }
            val savedJourney = savedState.journeys[partnerId] ?: return@post call.respond(HttpStatusCode.NotFound, ApiMessage("Journey not found"))
            call.respond(Journey("journey-$partnerId", partner.name, savedJourney.activities, savedJourney.outcome, partnerId, savedJourney.mode, savedJourney.feedback))
        }
        post("/api/journey/outcome") {
            val account = call.requireAccount() ?: return@post
            val request = call.receive<OutcomeRequest>()
            val partnerId = request.partnerId.ifBlank { account.state.acceptedProfiles.firstOrNull().orEmpty() }
            if (partnerId !in account.state.acceptedProfiles) return@post call.respond(HttpStatusCode.Forbidden, ApiMessage("That journey is not available"))
            val currentJourney = account.state.journeys[partnerId] ?: return@post call.respond(HttpStatusCode.NotFound, ApiMessage("Journey not found"))
            if (request.outcome == "still-thinking") {
                if (currentJourney.mode != "dating") return@post call.respond(HttpStatusCode.BadRequest, ApiMessage("Still thinking is only available during dating"))
                val partner = findCandidate(account, partnerId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ApiMessage("Profile not found"))
                val refreshed = StoredJourney("dating", JourneyPlanner.generate("dating", (account.state.profile.interests + partner.interests).distinct()))
                LocalDatabase.updateState(account.id) { it.copy(journeys = it.journeys + (partnerId to refreshed)) }
                return@post call.respond(Journey("journey-$partnerId", partner.name, refreshed.activities, null, partnerId, refreshed.mode))
            }
            if (request.outcome !in setOf("marriage", "friends")) return@post call.respond(HttpStatusCode.BadRequest, ApiMessage("Choose marriage, friends, or still thinking"))
            if (currentJourney.mode == "friend" && request.outcome == "marriage") return@post call.respond(HttpStatusCode.Forbidden, ApiMessage("A friend journey cannot lock dating settings"))
            if (!currentJourney.activities.all { it.done }) return@post call.respond(HttpStatusCode.Conflict, ApiMessage("Finish every journey activity first"))
            if (request.outcome == "marriage" && account.state.profile.relationshipLocked && account.state.committedPartnerId != partnerId) {
                return@post call.respond(HttpStatusCode.Forbidden, ApiMessage("Dating settings are already locked"))
            }
            val state = LocalDatabase.updateState(account.id) { current ->
                val accepted = if (request.outcome == "marriage") setOf(partnerId) else current.acceptedProfiles
                current.copy(
                    profile = if (request.outcome == "marriage") current.profile.copy(relationshipLocked = true) else current.profile,
                    committedPartnerId = if (request.outcome == "marriage") partnerId else current.committedPartnerId,
                    acceptedProfiles = accepted,
                    journeys = current.journeys + (partnerId to currentJourney.copy(outcome = request.outcome))
                )
            }
            val partner = findCandidate(account, partnerId)
                ?: return@post call.respond(HttpStatusCode.NotFound, ApiMessage("Profile not found"))
            val saved = state.journeys.getValue(partnerId)
            call.respond(Journey("journey-$partnerId", partner.name, saved.activities, saved.outcome, partnerId, saved.mode, saved.feedback))
        }
        get("/api/likes") {
            val account = call.requireAccount() ?: return@get
            call.respond(availableProfiles(account, if (account.state.profile.relationshipLocked) "friend" else "dating"))
        }
        post("/api/likes/{id}") {
            val account = call.requireAccount() ?: return@post
            val id = call.parameters["id"]
            if (id == null || findCandidate(account, id) == null) return@post call.respond(HttpStatusCode.NotFound, ApiMessage("Profile not found"))
            val text = call.receive<CommentRequest>().text.trim()
            if (text.isNotEmpty()) LocalDatabase.updateState(account.id) { current ->
                current.copy(comments = current.comments + (id!! to (current.comments[id].orEmpty() + text)))
            }
            call.respond(ApiMessage(if (text.isEmpty()) "Like recorded" else "Comment saved"))
        }
        get("/api/chats") {
            val account = call.requireAccount() ?: return@get
            val registeredUsers = LocalDatabase.discoverableUsers(account.id).associateBy { it.accountId }
            val sharedChats = LocalDatabase.connectedUsers(account.id).mapNotNull { otherAccountId ->
                val other = registeredUsers[otherAccountId]?.asCandidate() ?: return@mapNotNull null
                val lastMessage = LocalDatabase.directMessages(account.id, otherAccountId).lastOrNull()?.text ?: "Say hello"
                ChatPreview(other.id, other.name, other.photoUrl, lastMessage, 0)
            }
            call.respond(sharedChats.distinctBy { it.id })
        }
        get("/api/chats/{id}/messages") {
            val account = call.requireAccount() ?: return@get
            val id = call.parameters["id"]!!
            val otherAccountId = memberAccountId(id)
            if (otherAccountId != null) {
                if (!LocalDatabase.isConnected(account.id, otherAccountId)) return@get call.respond(HttpStatusCode.Forbidden, ApiMessage("Swipe right to start a chat"))
                return@get call.respond(LocalDatabase.directMessages(account.id, otherAccountId))
            }
            if (id !in account.state.acceptedProfiles) return@get call.respond(HttpStatusCode.Forbidden, ApiMessage("Swipe right to start a chat"))
            call.respond(account.state.messages[id].orEmpty())
        }
        post("/api/chats/{id}/messages") {
            val account = call.requireAccount() ?: return@post
            val id = call.parameters["id"]!!
            val otherAccountId = memberAccountId(id)
            if (otherAccountId != null) {
                if (!LocalDatabase.isConnected(account.id, otherAccountId)) return@post call.respond(HttpStatusCode.Forbidden, ApiMessage("Swipe right to start a chat"))
                val text = call.receive<SendMessageRequest>().text.trim()
                if (text.isBlank()) return@post call.respond(HttpStatusCode.BadRequest, ApiMessage("Message cannot be empty"))
                return@post call.respond(LocalDatabase.sendDirectMessage(account.id, otherAccountId, text))
            }
            if (id !in account.state.acceptedProfiles) return@post call.respond(HttpStatusCode.Forbidden, ApiMessage("Swipe right to start a chat"))
            val text = call.receive<SendMessageRequest>().text.trim()
            if (text.isBlank()) return@post call.respond(HttpStatusCode.BadRequest, ApiMessage("Message cannot be empty"))
            val today = LocalDate.now()
            val messageTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            val state = LocalDatabase.updateState(account.id) { current ->
                val thread = current.messages[id].orEmpty() + ChatMessage("${id}-${current.messages[id].orEmpty().size}", text, true, messageTime)
                val previous = current.lastChatDates[id]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                val streak = if (previous == today) current.streaks[id] ?: 1 else if (previous == today.minusDays(1)) (current.streaks[id] ?: 0) + 1 else 1
                current.copy(messages = current.messages + (id to thread), lastChatDates = current.lastChatDates + (id to today.toString()), streaks = current.streaks + (id to streak))
            }
            call.respond(state.messages[id].orEmpty())
        }
        get("/api/notifications") {
            val account = call.requireAccount() ?: return@get
            call.respond(LocalDatabase.unreadSwipeNotifications(account.id))
        }
        post("/api/notifications/{id}/read") {
            val account = call.requireAccount() ?: return@post
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, ApiMessage("Invalid notification"))
            LocalDatabase.markSwipeNotificationRead(account.id, id)
            call.respond(ApiMessage("Notification read"))
        }
        get("/api/profile") {
            val account = call.requireAccount() ?: return@get
            call.respond(account.state.profile)
        }
        put("/api/profile") {
            val account = call.requireAccount() ?: return@put
            val update = call.receive<ProfileUpdate>()
            val state = LocalDatabase.updateState(account.id) { current ->
                val profile = current.profile.copy(
                    name = update.name,
                    age = update.age.coerceIn(18, 100),
                    gender = update.gender,
                    sexuality = update.sexuality,
                    ageRange = if (current.profile.relationshipLocked) current.profile.ageRange else update.ageRange,
                    interests = update.interests,
                    location = update.location,
                    birthDate = update.birthDate,
                    phoneNumber = update.phoneNumber
                )
                current.copy(profile = profile)
            }
            call.respond(state.profile)
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.requireAccount(): AuthenticatedAccount? {
    val token = request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")?.trim()
    val account = LocalDatabase.authenticate(token)
    if (account != null) return account
    respond(HttpStatusCode.Unauthorized, ApiMessage("Sign in to continue"))
    return null
}