# README — Remote Gallery (Android Client)

## Overview
`Remote Gallery` is a Kotlin/Jetpack Compose Android client for the Remote Gallery Management Platform. It allows a device owner to explicitly pair/register a device, grant consent, index accessible photos/videos via Android `MediaStore`, synchronize metadata with the backend, receive authorized remote operation requests, and report operation outcomes.

Package: `com.zenox.remotegallery`  
Min SDK: 26 | Target/Compile SDK: 36

## Key capabilities implemented
- **Onboarding**: Welcome/privacy, permissions explanation/request, account registration/pairing, explicit remote-access consent opt-in.
- **Permissions (Android-aware)**: requests media permissions by API level, supports Android 13 (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`), Android 14+ selected visual media (`READ_MEDIA_VISUAL_USER_SELECTED`) and full access, and legacy storage permission for older supported devices. Permission state is surfaced in UI and indexing respects granted scope.
- **Automatic MediaStore indexing**: discovers accessible images/videos, stores local metadata index in Room, performs incremental re-indexing with dirty tracking, detects added/modified/deleted items, and syncs metadata to backend (`POST /api/device/media/sync`). No full media file upload during indexing.
- **Backend integration** (adapted to actual backend): device register/auth, heartbeat/status, media metadata sync, pending operation polling/ack/status updates, authenticated WebSocket (STOMP over `/ws`).
- **Remote operations**: consent-gated handling of `PREVIEW`, `EDIT`, `DELETE` requests with operation acknowledgment/status reporting. `DELETE`/write operations request Android system approval via `MediaStore.createDeleteRequest()` / `createWriteRequest()` when required (API 30+), with best-effort fallback handling on older APIs.
- **Realtime/background**: STOMP WebSocket with reconnect/heartbeat and topic subscriptions, WorkManager periodic sync/op checks and retry behavior, foreground service with persistent notification while remote access is enabled.
- **UI (Material 3, dark/light/system)**: Home dashboard, Gallery, Device Connection, Remote Access, Settings.
- **Security**: encrypted token storage (Android Keystore-backed `EncryptedSharedPreferences`), HTTPS production URL config, no token logging in app logs/interceptor redaction, explicit consent controls, disconnect/revoke/logout flows.

## Project structure
- `app/src/main/java/com/zenox/remotegallery/core` — app container/config/results
- `data/local` — Room DB/entities/DAOs, secure token store, DataStore settings
- `data/remote` — Retrofit APIs, DTOs, OkHttp/Auth interceptor, STOMP WebSocket manager
- `data/repository` — device/gallery/media indexing/operation processing
- `worker` — WorkManager sync/poll workers & scheduler
- `service` — foreground remote-access service
- `ui` — Compose screens, navigation, theme, ViewModels
- `app/src/test` — unit tests

## Build outputs
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK (debug-signed for local install unless replaced with release keystore): `app/build/outputs/apk/release/app-release.apk`

## Environment/config
Server URLs are configured in `app/build.gradle.kts` (`BuildConfig`):
- Dev: `http://10.0.2.2:8080` (Android emulator -> local backend)
- Prod: `https://YOUR-RENDER-BACKEND`
- Runtime override: Settings screen “Server URL override” (stored locally)

## Known backend-adaptation notes
The client is adapted to the inspected backend contracts. See `ANDROID_API_INTEGRATION.md` and `TEST_REPORT.md` for contract gaps (e.g. no pairing-code issuance endpoint, no binary media relay/upload endpoint, device refresh-token rotation not provided) and how the app behaves.

## Physical-device validation required
MediaStore indexing, permission grant flows (including selected-photos), system delete/write approval dialogs, foreground service behavior, and real backend/WebSocket connectivity require an Android device/emulator and running backend for full end-to-end validation. Local automated tests and APK build were executed in this environment.
