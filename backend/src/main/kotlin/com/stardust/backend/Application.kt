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
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

private val profiles = listOf(
    Profile("joe", "Joe Black", 33, "Man", "Straight", "A good coffee, a long walk, and a little friendly competition.", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=900", listOf("Coffee", "Travel", "Games")),
    Profile("nick", "Nick Perval", 27, "Man", "Straight", "Usually planning my next trip or trying a new recipe.", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=900", listOf("Travel", "Cooking", "Music")),
    Profile("leo", "Leo Rivera", 30, "Man", "Straight", "Museum dates, midnight playlists, and terrible puns.", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=900", listOf("Art", "Music", "Books")),
    Profile("maya", "Maya Chen", 29, "Woman", "Straight", "Looking for thoughtful friends for little city adventures.", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=900", listOf("Art", "Hiking", "Food")),
    Profile("amara", "Amara James", 31, "Woman", "Straight", "A friend for weekend markets and spontaneous day trips.", "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=900", listOf("Travel", "Coffee", "Movies"))
)

private fun Profile.isInAgeRange(range: String): Boolean {
    val bounds = Regex("(\\d+)\\s*-\\s*(\\d+)").matchEntire(range.trim())?.groupValues ?: return true
    val minimum = bounds[1].toIntOrNull() ?: return true
    val maximum = bounds[2].toIntOrNull() ?: return true
    return age in minimum..maximum
}

private val completed = ConcurrentHashMap.newKeySet<String>()
private val feedback = ConcurrentHashMap<String, String>()
private val acceptedProfiles = ConcurrentHashMap.newKeySet<String>()
private val messages = ConcurrentHashMap<String, MutableList<ChatMessage>>()
private val streaks = ConcurrentHashMap<String, Int>()
private val lastChatDate = ConcurrentHashMap<String, LocalDate>()
private val comments = ConcurrentHashMap<String, MutableList<String>>()
@Volatile private var journeyOutcome: String? = null
@Volatile private var committedPartnerId: String? = null
@Volatile private var journeyActivities = listOf(
    JourneyActivity("coffee", "Find your little coffee spot", "Pick a cozy cafe and trade the stories behind your favorite comfort drinks."),
    JourneyActivity("walk", "Take the scenic route", "Choose a new neighborhood walk. Each of you picks one stop along the way."),
    JourneyActivity("playlist", "Build a two-person playlist", "Add three songs each, then listen together and share the memories they bring back.")
)
@Volatile private var userProfile = UserProfile("Alex Morgan", 28, "Woman", "Straight", "26-34", listOf("Travel", "Live music", "Cooking"))

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

private fun Application.module() {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    routing {
        get("/api/health") { call.respond(ApiMessage("Stardust API is online")) }
        get("/api/profiles") {
            val mode = call.request.queryParameters["mode"] ?: "dating"
            val pool = if (mode == "friend" || userProfile.relationshipLocked) profiles.filter { it.gender == userProfile.gender && it.sexuality == "Straight" }
            else profiles.filter { it.gender != userProfile.gender && it.sexuality == userProfile.sexuality && it.isInAgeRange(userProfile.ageRange) }
            call.respond(pool)
        }
        post("/api/swipes") {
            val request = call.receive<SwipeRequest>()
            if (profiles.none { it.id == request.profileId }) return@post call.respond(HttpStatusCode.NotFound, ApiMessage("Profile not found"))
            if (userProfile.relationshipLocked && request.accepted && profiles.first { it.id == request.profileId }.gender != userProfile.gender) {
                return@post call.respond(HttpStatusCode.Forbidden, ApiMessage("Dating discovery is locked"))
            }
            if (request.accepted) acceptedProfiles.add(request.profileId) else acceptedProfiles.remove(request.profileId)
            call.respond(ApiMessage(if (request.accepted) "Like recorded" else "Passed"))
        }
        get("/api/journey") {
            val partner = profiles.firstOrNull { acceptedProfiles.contains(it.id) }
            call.respond(Journey("first-spark", partner?.name ?: "your match", journeyActivities.map { it.copy(done = completed.contains(it.id)) }, journeyOutcome))
        }
        post("/api/journey/generate") {
            val mode = call.receive<JourneyGenerateRequest>().mode
            if (mode !in setOf("dating", "friend")) return@post call.respond(HttpStatusCode.BadRequest, ApiMessage("Choose dating or friend mode"))
            journeyActivities = JourneyPlanner.generate(mode, userProfile.interests)
            completed.clear()
            feedback.clear()
            journeyOutcome = null
            call.respond(Journey("first-spark", "your match", journeyActivities, null))
        }
        post("/api/journey/{id}/complete") {
            val id = call.parameters["id"]
            if (acceptedProfiles.isEmpty()) return@post call.respond(HttpStatusCode.Conflict, ApiMessage("Connect with a match before starting a journey"))
            if (journeyActivities.none { it.id == id }) return@post call.respond(HttpStatusCode.NotFound, ApiMessage("Activity not found"))
            completed.add(id!!)
            feedback[id] = call.receive<CompletionRequest>().feeling
            call.respond(ApiMessage("Journey check-in saved"))
        }
        post("/api/journey/outcome") {
            val outcome = call.receive<OutcomeRequest>().outcome
            if (outcome !in setOf("marriage", "friends")) return@post call.respond(HttpStatusCode.BadRequest, ApiMessage("Choose marriage or friends"))
            if (!completed.containsAll(journeyActivities.map { it.id })) return@post call.respond(HttpStatusCode.Conflict, ApiMessage("Finish every journey activity first"))
            if (acceptedProfiles.isEmpty()) return@post call.respond(HttpStatusCode.Conflict, ApiMessage("Connect with a match before choosing an outcome"))
            journeyOutcome = outcome
            if (outcome == "marriage") {
                committedPartnerId = profiles.firstOrNull { acceptedProfiles.contains(it.id) && it.gender != userProfile.gender }?.id
                acceptedProfiles.clear()
                committedPartnerId?.let(acceptedProfiles::add)
                userProfile = userProfile.copy(relationshipLocked = true)
            }
            call.respond(ApiMessage("Journey outcome saved"))
        }
        get("/api/likes") {
            val pool = if (userProfile.relationshipLocked) profiles.filter { it.gender == userProfile.gender && it.sexuality == "Straight" }
            else profiles.filter { it.gender != userProfile.gender && it.sexuality == userProfile.sexuality && it.isInAgeRange(userProfile.ageRange) }
            call.respond(pool)
        }
        post("/api/likes/{id}") {
            val id = call.parameters["id"]
            if (profiles.none { it.id == id }) return@post call.respond(HttpStatusCode.NotFound, ApiMessage("Profile not found"))
            val text = call.receive<CommentRequest>().text.trim()
            if (text.isNotEmpty()) comments.computeIfAbsent(id!!) { mutableListOf() }.add(text)
            call.respond(ApiMessage(if (text.isEmpty()) "Like recorded" else "Comment saved"))
        }
        get("/api/chats") {
            val today = LocalDate.now()
            call.respond(profiles.filter { acceptedProfiles.contains(it.id) }.map { profile ->
                val lastDate = lastChatDate[profile.id]
                val streak = if (lastDate != null && lastDate.isBefore(today.minusDays(1))) 1 else streaks[profile.id] ?: 0
                if (lastDate != null && lastDate.isBefore(today.minusDays(1))) streaks[profile.id] = 1
                ChatPreview(profile.id, profile.name, profile.photoUrl, messages[profile.id]?.lastOrNull()?.text ?: "Send a little hello", streak)
            })
        }
        get("/api/chats/{id}/messages") {
            val id = call.parameters["id"]!!
            if (!acceptedProfiles.contains(id)) return@get call.respond(HttpStatusCode.Forbidden, ApiMessage("Swipe right to start a chat"))
            call.respond(messages[id]?.toList() ?: emptyList<ChatMessage>())
        }
        post("/api/chats/{id}/messages") {
            val id = call.parameters["id"]!!
            if (!acceptedProfiles.contains(id)) return@post call.respond(HttpStatusCode.Forbidden, ApiMessage("Swipe right to start a chat"))
            val text = call.receive<SendMessageRequest>().text.trim()
            if (text.isBlank()) return@post call.respond(HttpStatusCode.BadRequest, ApiMessage("Message cannot be empty"))
            val thread = messages.computeIfAbsent(id) { mutableListOf() }
            thread.add(ChatMessage("${id}-${thread.size}", text, true, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))))
            val today = LocalDate.now()
            val previous = lastChatDate.put(id, today)
            if (previous != today) streaks[id] = if (previous == today.minusDays(1)) (streaks[id] ?: 0) + 1 else 1
            call.respond(thread.toList())
        }
        get("/api/profile") { call.respond(userProfile) }
        put("/api/profile") {
            val update = call.receive<ProfileUpdate>()
            userProfile = userProfile.copy(
                name = update.name,
                age = update.age.coerceIn(18, 100),
                gender = update.gender,
                sexuality = update.sexuality,
                ageRange = if (userProfile.relationshipLocked) userProfile.ageRange else update.ageRange,
                interests = update.interests
            )
            call.respond(userProfile)
        }
    }
}