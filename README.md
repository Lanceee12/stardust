# Stardust

Stardust is a native Android dating and friendship app built with Kotlin, Jetpack Compose, and a Kotlin/Ktor REST backend. The UI uses a drawn Stardust mark and a procedural starfield inspired by the supplied references.

## Run locally

Requirements: JDK 17, Android SDK Platform 35, and Android Build Tools 35.0.0.

1. Start the API: `./gradlew :backend:run`
2. Open the project in Android Studio, select the `app` run configuration, and launch an emulator.
3. Or build the APK: `./gradlew :app:assembleDebug`

The Android emulator connects to the API at `http://10.0.2.2:8080/api`. For a physical device, change `baseUrl` in `StardustApi.kt` to the development machine's LAN address. The app keeps a small local demo experience available if the backend cannot be reached. Since this workspace path contains a colon, Gradle automatically places app and backend build outputs under `/tmp/stardust-app-build` and `/tmp/stardust-backend-build` to keep compiler classpaths valid.

To enable LLM-generated journey ideas, set `OPENAI_API_KEY` before starting the backend. `OPENAI_MODEL` is optional and defaults to `gpt-4o-mini`. With no key, the backend generates interest-aware dating or friendship activities from local templates.

## App flow

- Swipe cards accept horizontal gestures or the pass/like controls. Accepted profiles become available in Chat.
- Journey activities require a connected profile. Each completion records a positive or negative check-in; finishing the full path allows the pair to choose marriage or friendship.
- Choosing marriage locks dating age preferences, switches discovery to same-gender straight friends, and keeps the committed partner chat.
- Likes provides photo likes and comments. Chat messages update the daily streak pet. Profile details are editable.

## API

- `GET /api/health`
- `GET /api/profiles?mode=dating|friend`, `POST /api/swipes`
- `GET /api/journey`, `POST /api/journey/generate`, `POST /api/journey/{id}/complete`, `POST /api/journey/outcome`
- `GET /api/likes`, `POST /api/likes/{id}`
- `GET /api/chats`, `GET /api/chats/{id}/messages`, `POST /api/chats/{id}/messages`
- `GET /api/profile`, `PUT /api/profile`

## Prototype notes

The backend currently stores demo users, matches, chat history, and streak state in memory; restarting it resets that data. It has no authentication, moderation, push notifications, or durable database yet, so it is a functional local prototype rather than a production dating service. OpenAI credentials stay on the backend and are never bundled into the APK.