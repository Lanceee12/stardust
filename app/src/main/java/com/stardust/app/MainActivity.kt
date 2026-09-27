package com.stardust.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.roundToInt
import kotlin.random.Random

private val Ink = Color(0xFF10152E)
private val Navy = Color(0xFF222D58)
private val Blue = Color(0xFF73C4EB)
private val Pink = Color(0xFFF2AFC9)
private val Gold = Color(0xFFFFD681)
private val Paper = Color(0xFFFFF9F5)
private val Glass = Color(0xCC25385E)

private val fallbackActivities = listOf(
    JourneyActivity("coffee", "Find your little coffee spot", "Pick a cozy cafe and trade the stories behind your favorite comfort drinks."),
    JourneyActivity("walk", "Take the scenic route", "Choose a new neighborhood walk. Each of you picks one stop along the way."),
    JourneyActivity("playlist", "Build a two-person playlist", "Add three songs each, then listen together and share the memories they bring back.")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16, 21, 46)
        window.navigationBarColor = android.graphics.Color.rgb(16, 21, 46)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Pink, secondary = Blue, background = Ink, surface = Navy)) {
                StardustApp()
            }
        }
    }
}

private enum class Tab(val label: String) { Swipe("Swipe"), Journey("Journey"), Likes("Likes"), Chat("Chat"), Profile("Profile") }

private enum class AuthScreen { Welcome, SignIn, Onboarding, ForgotPassword, NewPassword }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AuthFlow(api: StardustApi, onAuthenticated: (AuthSession) -> Unit) {
    var screen by remember { mutableStateOf(AuthScreen.Welcome) }
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Female") }
    var birthDate by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var sexuality by remember { mutableStateOf("Straight") }
    var ageRange by remember { mutableStateOf("26-34") }
    var interests by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var resetCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var demoCode by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showBirthDatePicker by remember { mutableStateOf(false) }
    val birthDatePickerState = rememberDatePickerState()
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        StarfieldBackground()
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (screen == AuthScreen.Welcome) {
                Spacer(Modifier.height(24.dp))
                LogoMark(126.dp)
                Text("Stardust", color = Paper, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Text("FIND YOUR DESTINED STAR", color = Pink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(18.dp))
                Text("Find your star", color = Paper, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Let the constellations guide you toward a meaningful connection.", color = Paper.copy(alpha = .75f), textAlign = TextAlign.Center, fontSize = 13.sp)
                Button(onClick = { screen = AuthScreen.Onboarding }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = Pink), shape = RoundedCornerShape(24.dp)) {
                    Text("Get started  →", color = Ink, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { feedback = null; screen = AuthScreen.SignIn }) { Text("Already have an account?  Sign in", color = Paper) }
            } else {
                LogoMark(58.dp)
                Text(
                    when (screen) {
                        AuthScreen.SignIn -> "Sign In To Stardust"
                        AuthScreen.Onboarding -> "Welcome To Stardust"
                        AuthScreen.ForgotPassword -> "Reset Password"
                        AuthScreen.NewPassword -> "Create New Password"
                        AuthScreen.Welcome -> "Stardust"
                    },
                    color = Paper,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                when (screen) {
                    AuthScreen.SignIn -> {
                        OutlinedTextField(email, { email = it; feedback = null }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                        OutlinedTextField(password, { password = it; feedback = null }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
                        TextButton(onClick = { feedback = null; screen = AuthScreen.ForgotPassword }, modifier = Modifier.align(Alignment.End)) { Text("Forgot password?", color = Gold) }
                        Button(
                            onClick = {
                                busy = true
                                scope.launch {
                                    runCatching { api.login(LoginRequest(email.trim(), password)) }
                                        .onSuccess(onAuthenticated)
                                        .onFailure { feedback = it.message ?: "Sign in failed." }
                                    busy = false
                                }
                            }, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Pink), shape = RoundedCornerShape(24.dp)
                        ) { Text(if (busy) "Signing in…" else "Sign In", color = Ink, fontWeight = FontWeight.Bold) }
                        TextButton(onClick = { feedback = null; screen = AuthScreen.Onboarding }) { Text("Don't have an account?  Sign up", color = Paper) }
                    }
                    AuthScreen.Onboarding -> {
                        OutlinedTextField(name, { name = it; feedback = null }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Text("Gender", color = Paper.copy(alpha = .75f), modifier = Modifier.align(Alignment.Start), fontSize = 12.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("Male", "Female", "Others").forEach { choice ->
                                Row(Modifier.weight(1f).clickable { gender = choice }, verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = gender == choice, onClick = { gender = choice })
                                    Text(choice, color = Paper, fontSize = 11.sp)
                                }
                            }
                        }
                        OutlinedTextField(
                            birthDate,
                            onValueChange = {},
                            label = { Text("Birth date") },
                            modifier = Modifier.fillMaxWidth().clickable { showBirthDatePicker = true },
                            singleLine = true,
                            readOnly = true,
                            trailingIcon = { IconButton(onClick = { showBirthDatePicker = true }) { Icon(Icons.Filled.CalendarToday, "Choose birth date", tint = Blue) } }
                        )
                        OutlinedTextField(location, { location = it; feedback = null }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(phoneNumber, { phoneNumber = it; feedback = null }, label = { Text("Phone number") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                        OutlinedTextField(sexuality, { sexuality = it }, label = { Text("Sexuality") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(ageRange, { ageRange = it }, label = { Text("Dating age range") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(interests, { interests = it }, label = { Text("Interests, separated by commas") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                        OutlinedTextField(email, { email = it; feedback = null }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                        OutlinedTextField(password, { password = it; feedback = null }, label = { Text("Password · at least 8 characters") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
                        OutlinedTextField(confirmation, { confirmation = it; feedback = null }, label = { Text("Confirm password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
                        Button(
                            onClick = {
                                if (password != confirmation) {
                                    feedback = "Your passwords do not match."
                                } else {
                                    busy = true
                                    val request = RegisterRequest(name, email.trim(), password, gender, birthDate, location, phoneNumber, sexuality, ageRange, interests.split(',').map(String::trim).filter(String::isNotEmpty))
                                    scope.launch {
                                        runCatching { api.register(request) }
                                            .onSuccess(onAuthenticated)
                                            .onFailure { feedback = it.message ?: "Registration failed." }
                                        busy = false
                                    }
                                }
                            }, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Pink), shape = RoundedCornerShape(24.dp)
                        ) { Text(if (busy) "Creating account…" else "Sign Up", color = Ink, fontWeight = FontWeight.Bold) }
                        TextButton(onClick = { feedback = null; screen = AuthScreen.SignIn }) { Text("Already have an account?  Sign in", color = Paper) }
                    }
                    AuthScreen.ForgotPassword -> {
                        OutlinedTextField(email, { email = it; feedback = null }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                        Button(
                            onClick = {
                                busy = true
                                scope.launch {
                                    runCatching { api.requestPasswordReset(email.trim()) }.onSuccess { result ->
                                        demoCode = result.demoCode
                                        feedback = result.message
                                        screen = AuthScreen.NewPassword
                                    }.onFailure { feedback = it.message ?: "Could not request a reset code." }
                                    busy = false
                                }
                            }, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Pink), shape = RoundedCornerShape(24.dp)
                        ) { Text("Send Code", color = Ink, fontWeight = FontWeight.Bold) }
                        TextButton(onClick = { feedback = null; screen = AuthScreen.SignIn }) { Text("Remember your password?  Sign in", color = Paper) }
                    }
                    AuthScreen.NewPassword -> {
                        if (demoCode != null) Text("Prototype verification code: $demoCode", color = Gold, fontSize = 12.sp)
                        OutlinedTextField(resetCode, { resetCode = it; feedback = null }, label = { Text("Verification code") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(newPassword, { newPassword = it; feedback = null }, label = { Text("New password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
                        OutlinedTextField(confirmation, { confirmation = it; feedback = null }, label = { Text("Confirm password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
                        Button(
                            onClick = {
                                if (newPassword != confirmation) feedback = "Your passwords do not match."
                                else {
                                    busy = true
                                    scope.launch {
                                        runCatching { api.resetPassword(ResetPasswordRequest(email.trim(), resetCode, newPassword)) }
                                            .onSuccess { feedback = it.message; password = ""; screen = AuthScreen.SignIn }
                                            .onFailure { feedback = it.message ?: "Password could not be changed." }
                                        busy = false
                                    }
                                }
                            }, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Pink), shape = RoundedCornerShape(24.dp)
                        ) { Text("Continue to Sign In", color = Ink, fontWeight = FontWeight.Bold) }
                    }
                    AuthScreen.Welcome -> Unit
                }
                feedback?.let { Text(it, color = Gold, fontSize = 12.sp, textAlign = TextAlign.Center) }
            }
        }
        if (showBirthDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showBirthDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        birthDatePickerState.selectedDateMillis?.let { timestamp ->
                            birthDate = Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        }
                        showBirthDatePicker = false
                    }) { Text("Choose", color = Gold) }
                },
                dismissButton = { TextButton(onClick = { showBirthDatePicker = false }) { Text("Cancel", color = Paper) } }
            ) {
                DatePicker(state = birthDatePickerState)
            }
        }
    }
}

@Composable
private fun StardustApp() {
    val context = LocalContext.current
    val api = remember { StardustApi() }
    val preferences = remember { context.getSharedPreferences("stardust_session", android.content.Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var sessionLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val token = preferences.getString("token", null)
        api.setSessionToken(token)
        if (token != null) {
            profile = runCatching { api.profile() }.getOrNull()
            if (profile == null) {
                api.setSessionToken(null)
                preferences.edit().remove("token").apply()
            }
        }
        sessionLoaded = true
    }

    when {
        !sessionLoaded -> Box(Modifier.fillMaxSize()) { StarfieldBackground() }
        profile == null -> AuthFlow(api) { session ->
            api.setSessionToken(session.token)
            preferences.edit().putString("token", session.token).apply()
            profile = session.profile
        }
        else -> AuthenticatedApp(
            api = api,
            initialUser = profile!!,
            onUserUpdated = { profile = it },
            onSignOut = {
                scope.launch {
                    runCatching { api.logout() }
                    api.setSessionToken(null)
                    preferences.edit().remove("token").apply()
                    profile = null
                }
            }
        )
    }
}

@Composable
private fun AuthenticatedApp(
    api: StardustApi,
    initialUser: UserProfile,
    onUserUpdated: (UserProfile) -> Unit,
    onSignOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val profiles = remember { mutableStateListOf<MatchProfile>() }
    val likedProfiles = remember { mutableStateListOf<MatchProfile>() }
    val chats = remember { mutableStateListOf<ChatPreview>() }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val journeyOptions = remember { mutableStateListOf<JourneySummary>() }
    var journey by remember { mutableStateOf(Journey("first-spark", "Your match", fallbackActivities)) }
    var user by remember(initialUser.email) { mutableStateOf(initialUser) }
    var tab by remember { mutableStateOf(Tab.Swipe) }
    var mode by remember { mutableStateOf("dating") }
    var profileIndex by remember { mutableIntStateOf(0) }
    var selectedChat by remember { mutableStateOf<ChatPreview?>(null) }
    var pendingActivity by remember { mutableStateOf<JourneyActivity?>(null) }
    var showOutcome by remember { mutableStateOf(false) }
    var commentTarget by remember { mutableStateOf<MatchProfile?>(null) }
    var editProfile by remember { mutableStateOf(false) }
    var apiConnected by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var swipeNotification by remember { mutableStateOf<SwipeNotification?>(null) }

    fun refreshProfiles() {
        scope.launch {
            val remote = runCatching { api.profiles(mode) }
            if (remote.isSuccess) apiConnected = true
            profiles.clear()
            profiles.addAll(remote.getOrNull().orEmpty())
            profileIndex = 0
        }
    }

    LaunchedEffect(Unit) {
        val initialMode = if (user.relationshipLocked) "friend" else "dating"
        val remoteProfiles = runCatching { api.profiles(initialMode) }
        if (remoteProfiles.isSuccess) apiConnected = true
        profiles.addAll(remoteProfiles.getOrNull().orEmpty())
        runCatching { api.likes() }.onSuccess { likedProfiles.addAll(it) }
        runCatching { api.chats() }.onSuccess { chats.addAll(it) }
        runCatching { api.journeys() }.onSuccess { options ->
            journeyOptions.addAll(options)
            options.firstOrNull()?.let { first -> runCatching { api.journey(first.partnerId) }.onSuccess { journey = it } }
        }
    }

    LaunchedEffect(tab, selectedChat?.id) {
        if (tab == Tab.Chat) {
            while (true) {
                if (selectedChat == null) {
                    runCatching { api.chats() }.onSuccess { refreshed -> chats.clear(); chats.addAll(refreshed) }
                } else {
                    runCatching { api.messages(selectedChat!!.id) }.onSuccess { refreshed ->
                        messages.clear()
                        messages.addAll(refreshed)
                    }
                }
                delay(1800)
            }
        }
    }

    LaunchedEffect(user.email) {
        while (true) {
            if (swipeNotification == null) {
                runCatching { api.notifications() }.onSuccess { unread ->
                    unread.firstOrNull()?.let { notification ->
                        swipeNotification = notification
                        runCatching { api.markNotificationRead(notification.id) }
                    }
                }
            }
            delay(3000)
        }
    }

    fun swipe(accepted: Boolean) {
        val current = profiles.getOrNull(profileIndex) ?: return
        scope.launch {
            val swipeResult = runCatching { api.submitSwipe(current.id, accepted) }
            if (swipeResult.isSuccess) apiConnected = true
            if (accepted) {
                journey = journey.copy(partnerName = current.name)
                infoMessage = "A little spark with ${current.name}. Say hello in Chat."
                if (swipeResult.isSuccess) {
                    runCatching { api.generateJourney(mode, current.id) }.onSuccess {
                        journey = it
                        apiConnected = true
                    }
                    runCatching { api.chats() }.onSuccess { refreshed -> chats.clear(); chats.addAll(refreshed) }
                    runCatching { api.journeys() }.onSuccess { refreshed -> journeyOptions.clear(); journeyOptions.addAll(refreshed) }
                } else if (chats.none { it.id == current.id }) {
                    chats.add(ChatPreview(current.id, current.name, current.photoUrl, "Send a little hello", 0))
                }
            }
            profileIndex = (profileIndex + 1) % profiles.size.coerceAtLeast(1)
        }
    }

    Box(Modifier.fillMaxSize()) {
        StarfieldBackground()
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                BottomTabs(tab, { tab = it; selectedChat = null })
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                TopBrand(apiConnected, mode, user)
                when (tab) {
                    Tab.Swipe -> SwipeScreen(
                        profile = profiles.getOrNull(profileIndex),
                        mode = mode,
                        onSwipe = ::swipe,
                        onRefresh = ::refreshProfiles,
                        committed = journey.outcome == "marriage"
                    )
                    Tab.Journey -> JourneyScreen(
                        journey = journey,
                        journeys = journeyOptions,
                        onSelectJourney = { option ->
                            scope.launch { runCatching { api.journey(option.partnerId) }.onSuccess { journey = it; apiConnected = true } }
                        },
                        onFinishActivity = { pendingActivity = it },
                        onSkipActivity = { activity ->
                            scope.launch {
                                runCatching { api.refreshActivity(activity.id, journey.partnerId) }.onSuccess { refreshed ->
                                    journey = refreshed
                                    apiConnected = true
                                    runCatching { api.journeys() }.onSuccess { journeyOptions.clear(); journeyOptions.addAll(it) }
                                }.onFailure { infoMessage = it.message ?: "Could not find another idea." }
                            }
                        },
                        onDecision = { showOutcome = true },
                        onRegenerate = {
                            val modeForJourney = journey.mode
                            scope.launch {
                                val generated = runCatching { api.generateJourney(modeForJourney, journey.partnerId) }
                                if (generated.isSuccess) {
                                    journey = generated.getOrThrow()
                                    apiConnected = true
                                } else {
                                    journey = journey.copy(activities = fallbackActivities.map { it.copy(done = false) }, outcome = null)
                                }
                            }
                        }
                    )
                    Tab.Likes -> LikesScreen(likedProfiles, onLike = { profile ->
                        scope.launch { runCatching { api.comment(profile.id, "") }.onSuccess { apiConnected = true } }
                        infoMessage = "You liked ${profile.name}'s photo."
                    }, onComment = { commentTarget = it })
                    Tab.Chat -> ChatScreen(
                        chats = chats,
                        selectedChat = selectedChat,
                        messages = messages,
                        onSelect = { chat ->
                            selectedChat = chat
                            messages.clear()
                            scope.launch { runCatching { api.messages(chat.id) }.onSuccess { messages.addAll(it); apiConnected = true } }
                        },
                        onSend = { text ->
                            val chat = selectedChat ?: return@ChatScreen
                            scope.launch {
                                val sent = runCatching { api.sendMessage(chat.id, text) }
                                if (sent.isSuccess) {
                                    messages.clear()
                                    messages.addAll(sent.getOrThrow())
                                    apiConnected = true
                                    runCatching { api.chats() }.onSuccess { updated -> chats.clear(); chats.addAll(updated) }
                                } else {
                                    messages.add(ChatMessage("local-${messages.size}", text, true, "now"))
                                }
                            }
                        },
                        onBack = { selectedChat = null }
                    )
                    Tab.Profile -> ProfileScreen(user, user.relationshipLocked, onEdit = { editProfile = true }, onSignOut = onSignOut)
                }
            }
        }
    }

    pendingActivity?.let { activity ->
        AlertDialog(
            onDismissRequest = { pendingActivity = null },
            containerColor = Navy,
            title = { Text("How did it feel together?", color = Paper) },
            text = { Text("${activity.title}\n\nYour honest check-in helps shape what comes next.", color = Paper.copy(alpha = .8f)) },
            confirmButton = {
                TextButton(onClick = {
                    journey = journey.copy(
                        activities = journey.activities.map { if (it.id == activity.id) it.copy(done = true) else it },
                        feedback = journey.feedback + (activity.id to "great")
                    )
                    scope.launch {
                        runCatching { api.completeActivity(activity.id, "great", journey.partnerId) }.onSuccess {
                            apiConnected = true
                            runCatching { api.journeys() }.onSuccess { journeyOptions.clear(); journeyOptions.addAll(it) }
                        }
                    }
                    pendingActivity = null
                    if (journey.activities.all { it.done || it.id == activity.id }) showOutcome = true
                }) { Text("It was lovely", color = Gold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    journey = journey.copy(
                        activities = journey.activities.map { if (it.id == activity.id) it.copy(done = true) else it },
                        feedback = journey.feedback + (activity.id to "not-for-us")
                    )
                    scope.launch {
                        runCatching { api.completeActivity(activity.id, "not-for-us", journey.partnerId) }.onSuccess {
                            runCatching { api.journeys() }.onSuccess { journeyOptions.clear(); journeyOptions.addAll(it) }
                        }
                    }
                    pendingActivity = null
                    if (journey.activities.all { it.done || it.id == activity.id }) showOutcome = true
                }) { Text("Not our thing", color = Pink) }
            }
        )
    }

    if (showOutcome) {
        AlertDialog(
            onDismissRequest = { showOutcome = false },
            containerColor = Navy,
            title = { Text(if (journey.mode == "friend") "Did you find a friendship to keep?" else "What feels right for you two?", color = Paper) },
            text = { Text(if (journey.mode == "friend") "Good friends make room for more small adventures." else "A good match can take many shapes. Choose what you both want next.", color = Paper.copy(alpha = .8f)) },
            confirmButton = {
                TextButton(onClick = {
                    if (journey.mode == "friend") {
                        journey = journey.copy(outcome = "friends")
                        scope.launch { runCatching { api.submitOutcome("friends", journey.partnerId) }.onSuccess { journey = it } }
                        infoMessage = "Here's to a friendship that feels like you."
                    } else {
                        journey = journey.copy(outcome = "marriage")
                        mode = "friend"
                        user = user.copy(relationshipLocked = true)
                        onUserUpdated(user)
                        scope.launch {
                            runCatching { api.submitOutcome("marriage", journey.partnerId) }.onSuccess { journey = it; apiConnected = true }
                            runCatching { api.profiles("friend") }.onSuccess { profiles.clear(); profiles.addAll(it); profileIndex = 0 }
                            runCatching { api.likes() }.onSuccess { likedProfiles.clear(); likedProfiles.addAll(it) }
                            runCatching { api.journeys() }.onSuccess { journeyOptions.clear(); journeyOptions.addAll(it) }
                        }
                        tab = Tab.Swipe
                        infoMessage = "Your dating discovery is locked. You're now finding friends."
                    }
                    showOutcome = false
                }) { Text(if (journey.mode == "friend") "Keep this friendship" else "Build toward marriage", color = Gold) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        if (journey.mode == "friend") {
                        scope.launch {
                                runCatching { api.generateJourney("friend", journey.partnerId) }.onSuccess { journey = it }
                        }
                        infoMessage = "A new friend adventure is on its way."
                        } else {
                            journey = journey.copy(outcome = "friends")
                            scope.launch { runCatching { api.submitOutcome("friends", journey.partnerId) }.onSuccess { journey = it } }
                            infoMessage = "Friendship can be its own kind of magic."
                        }
                        showOutcome = false
                    }) { Text(if (journey.mode == "friend") "Another adventure" else "Just friends", color = Blue) }
                    if (journey.mode == "dating") {
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { api.submitOutcome("still-thinking", journey.partnerId) }.onSuccess {
                                    journey = it
                                    apiConnected = true
                                    infoMessage = "Here are fresh ideas for your dating journey."
                                }.onFailure { infoMessage = it.message ?: "Could not refresh this journey." }
                            }
                            showOutcome = false
                        }) { Text("Still thinking", color = Gold) }
                    }
                }
            }
        )
    }

    swipeNotification?.let { notification ->
        AlertDialog(
            onDismissRequest = { swipeNotification = null },
            containerColor = Navy,
            title = { Text("${notification.actorName} chose you!", color = Paper) },
            text = { Text("They swiped right and would like to chat with you.", color = Paper.copy(alpha = .8f)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching { api.chats() }.onSuccess { refreshed ->
                            chats.clear()
                            chats.addAll(refreshed)
                            val chat = refreshed.firstOrNull { it.id == notification.chatId }
                            if (chat != null) {
                                selectedChat = chat
                                messages.clear()
                                runCatching { api.messages(chat.id) }.onSuccess { messages.addAll(it) }
                            }
                            tab = Tab.Chat
                        }
                        swipeNotification = null
                    }
                }) { Text("Open chat", color = Gold) }
            },
            dismissButton = { TextButton(onClick = { swipeNotification = null }) { Text("Later", color = Paper) } }
        )
    }

    commentTarget?.let { profile ->
        var comment by remember(profile.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { commentTarget = null },
            containerColor = Navy,
            title = { Text("Leave a kind note", color = Paper) },
            text = {
                Column {
                    Text("Say something about ${profile.name}'s photo.", color = Paper.copy(alpha = .75f))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Your comment") }, maxLines = 3)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { runCatching { api.comment(profile.id, comment) }.onSuccess { apiConnected = true } }
                    commentTarget = null
                    infoMessage = "Your note is on its way."
                }) { Text("Send", color = Gold) }
            },
            dismissButton = { TextButton(onClick = { commentTarget = null }) { Text("Cancel", color = Paper) } }
        )
    }

    if (editProfile) {
        EditProfileDialog(user, onDismiss = { editProfile = false }, onSave = { updated ->
            user = updated
            editProfile = false
            scope.launch { runCatching { api.updateProfile(ProfileUpdate(updated.name, updated.age, updated.gender, updated.sexuality, updated.ageRange, updated.interests)) }.onSuccess { user = it; apiConnected = true } }
        })
    }

    infoMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { infoMessage = null },
            containerColor = Navy,
            title = { Text(message, color = Paper) },
            confirmButton = { TextButton(onClick = { infoMessage = null }) { Text("Lovely", color = Gold) } }
        )
    }
}

@Composable
private fun TopBrand(apiConnected: Boolean, mode: String, user: UserProfile) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LogoMark(48.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Stardust", color = Paper, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(if (mode == "friend") "YOUR PEOPLE, WRITTEN IN THE STARS" else "FIND YOUR DESTINED STAR", color = Pink, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
        }
        Column(Modifier.width(100.dp), horizontalAlignment = Alignment.End) {
            Text(if (apiConnected) "API LIVE" else "DEMO MODE", color = if (apiConnected) Blue else Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(user.name, color = Paper.copy(alpha = .75f), fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun LogoMark(size: androidx.compose.ui.unit.Dp) {
    Image(painterResource(R.drawable.stardust_mark), contentDescription = "Stardust logo", modifier = Modifier.size(size))
}

@Composable
private fun StarfieldBackground() {
    val stars = remember { List(190) { Triple(Random(it * 31).nextFloat(), Random(it * 71 + 9).nextFloat(), Random(it * 11 + 2).nextFloat()) } }
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.linearGradient(listOf(Color(0xFF171529), Color(0xFF061A35), Color(0xFF174D8A)), start = Offset.Zero, end = Offset(size.width * .8f, size.height)))
        drawRect(Brush.radialGradient(listOf(Color(0xFF57283F).copy(alpha = .28f), Color.Transparent), center = Offset(size.width * .45f, size.height * .32f), radius = size.width * .85f))
        drawRect(Brush.radialGradient(listOf(Color(0xFF2374C6).copy(alpha = .32f), Color.Transparent), center = Offset(size.width, size.height), radius = size.width * .9f))
        stars.forEachIndexed { index, star ->
            val color = if (index % 7 == 0) Pink.copy(alpha = .8f) else if (index % 11 == 0) Gold.copy(alpha = .8f) else Blue.copy(alpha = .6f)
            drawCircle(color, radius = (.45f + star.third * 1.2f).dp.toPx(), center = Offset(star.first * size.width, star.second * size.height))
        }
    }
}

@Composable
private fun BottomTabs(selected: Tab, onSelected: (Tab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xF51F2A50)).padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(8.dp), top = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Tab.entries.forEach { tab ->
            val active = selected == tab
            val icon = when (tab) {
                Tab.Swipe -> Icons.Filled.LocalFireDepartment
                Tab.Journey -> Icons.Filled.Explore
                Tab.Likes -> Icons.Filled.Favorite
                Tab.Chat -> Icons.Filled.ChatBubble
                Tab.Profile -> Icons.Filled.Person
            }
            Column(
                Modifier.weight(1f).clickable { onSelected(tab) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(icon, contentDescription = tab.label, tint = if (active) Pink else Paper, modifier = Modifier.size(24.dp))
                Text(tab.label, color = if (active) Pink else Paper, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun PageHeading(title: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Paper, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, color = Paper.copy(alpha = .72f), fontSize = 12.sp)
        }
        trailing?.invoke()
    }
}

@Composable
private fun SwipeScreen(profile: MatchProfile?, mode: String, onSwipe: (Boolean) -> Unit, onRefresh: () -> Unit, committed: Boolean) {
    Column(Modifier.fillMaxSize()) {
        PageHeading(
            if (mode == "friend") "Find your people" else "A little cosmic chemistry",
            if (mode == "friend") "Friend mode · same-gender matches" else "One good hello can change everything",
            trailing = { IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, "Refresh", tint = Paper) } }
        )
        if (committed) {
            Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), color = Color(0xD927364F), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, null, tint = Gold, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Dating preferences are locked. You're discovering friends now.", color = Paper, fontSize = 12.sp)
                }
            }
        }
        if (profile == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No stars in this corner yet. Pull to refresh.", color = Paper) }
        } else {
            MatchCard(profile, Modifier.weight(1f).padding(horizontal = 18.dp, vertical = 8.dp), onSwipe)
            Row(Modifier.fillMaxWidth().padding(horizontal = 55.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                RoundAction("×", "Pass", Pink, onClick = { onSwipe(false) })
                RoundAction("♥", "Like", Gold, onClick = { onSwipe(true) })
            }
        }
    }
}

@Composable
private fun MatchCard(profile: MatchProfile, modifier: Modifier = Modifier, onSwipe: (Boolean) -> Unit) {
    var dragX by remember(profile.id) { mutableFloatStateOf(0f) }
    val rotation by animateFloatAsState(targetValue = dragX / 28f, label = "card-rotation")
    Card(
        modifier = modifier.offset { IntOffset(dragX.roundToInt(), 0) }.rotate(rotation)
            .pointerInput(profile.id) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount -> change.consume(); dragX += amount },
                    onDragEnd = {
                        if (dragX > 100f) onSwipe(true)
                        else if (dragX < -100f) onSwipe(false)
                        dragX = 0f
                    }
                )
            },
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(model = profile.photoUrl, contentDescription = profile.name, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color(0xEE10152E)))))
            Column(Modifier.align(Alignment.BottomStart).padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${profile.name}, ${profile.age}", color = Paper, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(profile.bio, color = Paper.copy(alpha = .94f), fontSize = 14.sp, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    profile.interests.take(3).forEach { InterestTag(it) }
                }
            }
            Text(if (dragX > 35) "A SPARK" else if (dragX < -35) "PASS" else "", color = if (dragX > 35) Gold else Pink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.TopStart).padding(22.dp))
        }
    }
}

@Composable
private fun InterestTag(label: String) {
    Surface(color = Color(0x88313D60), shape = RoundedCornerShape(20.dp), modifier = Modifier.border(1.dp, Paper.copy(alpha = .35f), RoundedCornerShape(20.dp))) {
        Text(label, color = Paper, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

@Composable
private fun RoundAction(symbol: String, label: String, tint: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(Modifier.size(52.dp).clickable(onClick = onClick), color = Color(0xDDFAF6F4), shape = CircleShape, shadowElevation = 8.dp) {
            Box(contentAlignment = Alignment.Center) { Text(symbol, color = tint, fontSize = 27.sp, fontWeight = FontWeight.Bold) }
        }
        Text(label, color = Paper, fontSize = 10.sp)
    }
}

@Composable
private fun JourneyScreen(
    journey: Journey,
    journeys: List<JourneySummary>,
    onSelectJourney: (JourneySummary) -> Unit,
    onFinishActivity: (JourneyActivity) -> Unit,
    onSkipActivity: (JourneyActivity) -> Unit,
    onDecision: () -> Unit,
    onRegenerate: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        PageHeading("Your journey", "Little moments, made for ${journey.partnerName}", trailing = {
            if (journeys.isNotEmpty()) IconButton(onClick = onRegenerate) { Icon(Icons.Filled.Refresh, "New journey", tint = Paper) }
        })
        if (journeys.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                journeys.forEach { option ->
                    val selected = option.partnerId == journey.partnerId
                    Surface(
                        Modifier.clip(RoundedCornerShape(16.dp)).clickable { onSelectJourney(option) },
                        color = if (selected) Color(0xDD526D9D) else Color(0xAA202A46),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(horizontal = 13.dp, vertical = 8.dp)) {
                            Text(option.partnerName, color = Paper, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("${option.mode} · ${option.completedActivities}/${option.totalActivities}", color = Paper.copy(alpha = .68f), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        if (journeys.isEmpty()) {
            Box(Modifier.weight(1f).padding(28.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.Explore, null, tint = Gold, modifier = Modifier.size(42.dp))
                    Text("Your journey starts with a spark", color = Paper, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Swipe right on someone first. Then your shared activities will appear here.", color = Paper.copy(alpha = .72f), fontSize = 13.sp)
                }
            }
        } else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = Glass), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, tint = Gold)
                        Spacer(Modifier.width(8.dp))
                        Text("THE FIRST SPARK", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    }
                    Text("Make a story together", color = Paper, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Your shared path grows one real-world moment at a time.", color = Paper.copy(alpha = .8f), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(journey.activities.size) { index ->
                            Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(if (journey.activities.getOrNull(index)?.done == true) Gold else Paper.copy(alpha = .22f)))
                        }
                    }
                }
            }
            journey.activities.forEachIndexed { index, activity ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xD91B2947)), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.Top) {
                        Surface(color = if (activity.done) Gold else Blue, shape = CircleShape, modifier = Modifier.size(30.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text(if (activity.done) "✓" else "${index + 1}", color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(activity.title, color = Paper, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(activity.detail, color = Paper.copy(alpha = .72f), fontSize = 12.sp, lineHeight = 17.sp)
                            if (!activity.done) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { onFinishActivity(activity) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("Mark complete  →", color = Gold, fontSize = 12.sp) }
                                    TextButton(onClick = { onSkipActivity(activity) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("Skip idea", color = Blue, fontSize = 12.sp) }
                                }
                            }
                            else Text("CHECKED IN", color = Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (journey.outcome != null) {
                OutlinedButton(onClick = onRegenerate, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Text("Generate another set of ideas", color = Paper)
                }
            } else if (journey.activities.isNotEmpty() && journey.activities.all { it.done }) {
                Button(onClick = onDecision, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Pink), shape = RoundedCornerShape(16.dp)) {
                    Text("Choose what comes next", color = Ink, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun LikesScreen(profiles: List<MatchProfile>, onLike: (MatchProfile) -> Unit, onComment: (MatchProfile) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageHeading("Little heart gallery", "Like a photo or leave a thoughtful comment")
        if (profiles.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Your gallery is finding its stars.", color = Paper) }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xCC212943))) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(220.dp)) {
                                AsyncImage(profile.photoUrl, profile.name, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                Surface(Modifier.align(Alignment.TopEnd).padding(8.dp), color = Color(0x9910152E), shape = CircleShape) {
                                    IconButton(onClick = { onLike(profile) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Favorite, "Like ${profile.name}", tint = Pink, modifier = Modifier.size(19.dp)) }
                                }
                            }
                            Text("${profile.name}, ${profile.age}", color = Paper, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp, top = 8.dp))
                            TextButton(onClick = { onComment(profile) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 8.dp, end = 8.dp, bottom = 4.dp, top = 0.dp)) { Text("Add a comment", color = Blue, fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatScreen(
    chats: List<ChatPreview>,
    selectedChat: ChatPreview?,
    messages: List<ChatMessage>,
    onSelect: (ChatPreview) -> Unit,
    onSend: (String) -> Unit,
    onBack: () -> Unit
) {
    var messageText by remember(selectedChat?.id) { mutableStateOf("") }
    if (selectedChat == null) {
        Column(Modifier.fillMaxSize()) {
            PageHeading("Your constellation", "A daily hello keeps your streak pet glowing")
            if (chats.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.ChatBubble, null, tint = Pink, modifier = Modifier.size(42.dp))
                        Text("Your chats start with a mutual yes", color = Paper, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Swipe right on someone, then come back here to say hello.", color = Paper.copy(alpha = .72f), fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(chats.size) { index ->
                        val chat = chats[index]
                        Card(Modifier.fillMaxWidth().clickable { onSelect(chat) }, colors = CardDefaults.cardColors(containerColor = Glass), shape = RoundedCornerShape(18.dp)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(chat.photoUrl, chat.name, Modifier.size(54.dp).clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(chat.name, color = Paper, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text(chat.lastMessage, color = Paper.copy(alpha = .65f), fontSize = 12.sp, maxLines = 1)
                                }
                                StreakBadge(chat.streak)
                            }
                        }
                    }
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("‹ Back", color = Blue) }
                AsyncImage(selectedChat.photoUrl, selectedChat.name, Modifier.size(38.dp).clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(selectedChat.name, color = Paper, fontWeight = FontWeight.Bold)
                    Text("Your little daily ritual", color = Paper.copy(alpha = .65f), fontSize = 11.sp)
                }
                StreakBadge(selectedChat.streak)
            }
            Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xDD293853)), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Pets, null, tint = Gold, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Your streak pet", color = Paper, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Chat today to keep the flame alive · Day ${selectedChat.streak}", color = Gold, fontSize = 11.sp)
                    }
                }
            }
            LazyColumn(Modifier.weight(1f), reverseLayout = true, verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)) {
                items(messages.reversed().size) { index ->
                    val msg = messages.reversed()[index]
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.mine) Arrangement.End else Arrangement.Start) {
                        Surface(color = if (msg.mine) Color(0xFF526D9D) else Color(0xCC293853), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.width(200.dp).padding(10.dp)) {
                                Text(msg.text, color = Paper, fontSize = 13.sp)
                                Text(msg.time, color = Paper.copy(alpha = .55f), fontSize = 9.sp, modifier = Modifier.align(Alignment.End))
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = messageText, onValueChange = { messageText = it }, modifier = Modifier.weight(1f), placeholder = { Text("Send a little hello") }, maxLines = 3, shape = RoundedCornerShape(18.dp))
                Spacer(Modifier.width(8.dp))
                Button(onClick = { if (messageText.isNotBlank()) { onSend(messageText.trim()); messageText = "" } }, colors = ButtonDefaults.buttonColors(containerColor = Pink), shape = CircleShape) { Text("↑", color = Ink, fontSize = 19.sp) }
            }
        }
    }
}

@Composable
private fun StreakBadge(streak: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.LocalFireDepartment, null, tint = Gold, modifier = Modifier.size(23.dp))
        Text("$streak", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProfileScreen(user: UserProfile, committed: Boolean, onEdit: () -> Unit, onSignOut: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageHeading("Your profile", "A little of the person behind the stars")
        Card(colors = CardDefaults.cardColors(containerColor = Glass), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(Modifier.size(86.dp), color = Color(0xFF415887), shape = CircleShape) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Person, null, tint = Paper, modifier = Modifier.size(46.dp)) }
                }
                Spacer(Modifier.height(10.dp))
                Text("${user.name}, ${user.age}", color = Paper, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("${user.gender} · ${user.sexuality}", color = Pink, fontSize = 13.sp)
                if (committed) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Lock, null, tint = Gold, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp)); Text("COMMITTED · FRIEND MODE", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
        ProfileDetail("Looking for", if (committed) "Friends like me" else "Ages ${user.ageRange}", Icons.Filled.Tune)
        ProfileDetail("Into lately", user.interests.joinToString("  ·  "), Icons.Filled.Star)
        ProfileDetail("Your setting", if (committed) "Dating preferences locked" else "Dating discovery", if (committed) Icons.Filled.Lock else Icons.Filled.Settings)
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD34436B)), shape = RoundedCornerShape(15.dp)) {
            Text("Edit my details", color = Paper, fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out", color = Pink) }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ProfileDetail(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC202A46)), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Blue, modifier = Modifier.size(20.dp))
            Column(Modifier.padding(start = 11.dp)) {
                Text(title, color = Paper.copy(alpha = .6f), fontSize = 10.sp)
                Text(value, color = Paper, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(user: UserProfile, onDismiss: () -> Unit, onSave: (UserProfile) -> Unit) {
    var name by remember { mutableStateOf(user.name) }
    var age by remember { mutableStateOf(user.age.toString()) }
    var gender by remember { mutableStateOf(user.gender) }
    var sexuality by remember { mutableStateOf(user.sexuality) }
    var ageRange by remember { mutableStateOf(user.ageRange) }
    var interests by remember { mutableStateOf(user.interests.joinToString(", ")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Navy,
        title = { Text("Your details", color = Paper) },
        text = {
            Column(Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(age, { age = it.filter(Char::isDigit).take(3) }, label = { Text("Age") }, singleLine = true)
                OutlinedTextField(gender, { gender = it }, label = { Text("Gender") }, singleLine = true)
                OutlinedTextField(sexuality, { sexuality = it }, label = { Text("Sexuality") }, singleLine = true)
                OutlinedTextField(ageRange, { ageRange = it }, label = { Text("Dating age range") }, singleLine = true, enabled = !user.relationshipLocked)
                OutlinedTextField(interests, { interests = it }, label = { Text("Interests, separated by commas") }, maxLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(user.copy(name = name.ifBlank { user.name }, age = age.toIntOrNull() ?: user.age, gender = gender, sexuality = sexuality, ageRange = if (user.relationshipLocked) user.ageRange else ageRange, interests = interests.split(',').map(String::trim).filter(String::isNotEmpty))) }) { Text("Save", color = Gold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Paper) } }
    )
}