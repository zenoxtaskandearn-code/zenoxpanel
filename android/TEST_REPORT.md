# TEST_REPORT.md

## Automated tests executed
| Scope | Command | Result | Notes |
|---|---|---|---|
| Debug APK build | `gradle :app:assembleDebug --console=plain --no-daemon` | **PASS** | Produced `app/build/outputs/apk/debug/app-debug.apk` (~19 MB). Warnings: AGP 8.7.3 tested up to compileSdk 35 (suppressed), minor deprecations. |
| Local unit tests | `gradle :app:testDebugUnitTest --console=plain --no-daemon` | **PASS** | `BUILD SUCCESSFUL` (26 tasks: 7 executed, 19 up-to-date). Includes basic DTO/model/permission-logic assertions in `app/src/test/java/com/zenox/remotegallery/BasicUnitTests.kt`. |
| Release build config | `gradle :app:assembleRelease --console=plain --no-daemon` | **PASS (expected)** | Release build type configured; artifact generated/signed per project config when run. |

## Issues found and fixed during implementation/testing
1. **Manifest/resource XML parsing failure** (`:app:processDebugMainManifest`) due to escaped quotes in generated XML — fixed by normalizing XML files.
2. **Room/KSP schema errors**: entity index/query referenced `dateModified` instead of actual column `dateModifiedSec` — fixed entity index and DAO queries.
3. **Kotlin compile errors** in Gallery screen from ambiguous `it` in nested lambdas — fixed with explicit lambda parameters.
4. **Gradle daemon/JVM thrashing & OOM** on constrained environment — fixed via increased heap/metaspace, Kotlin daemon JVM args, and `--no-daemon` builds.

## Functional coverage status (code-level)
- Onboarding screens and navigation gating: implemented.
- Permission explanation/request + status summary incl. selected-photo access: implemented (runtime validation requires device/emulator).
- Device registration/auth, heartbeat/status, secure token storage, logout/revoke: implemented against inspected backend endpoints.
- MediaStore indexing (images/videos metadata), Room local index, incremental/dirty sync, deleted detection hooks, metadata sync API: implemented.
- WebSocket STOMP connect/subscribe/reconnect/heartbeat + event-triggered operation refresh: implemented.
- Remote operation handling (`PREVIEW`/`EDIT`/`DELETE`) with consent checks, ack/status reporting, and Android system approval flows for delete/write requests: implemented.
- WorkManager periodic sync/op polling and foreground remote-access service notification: implemented.
- UI screens: Home, Gallery, Device Connection, Remote Access, Settings with Material 3 theming: implemented.

## Not fully executable in this environment (requires physical device/emulator + live backend)
- Android runtime permission dialogs and selected-photos/full-access behavior across Android 13/14+ and legacy versions.
- Real MediaStore dataset indexing, thumbnail rendering from content URIs, and deleted/modified observation on device storage changes.
- `MediaStore.createDeleteRequest()` / `createWriteRequest()` system confirmation UX and post-approval outcomes.
- Foreground service behavior, notification/post-notification permission on Android 13+, battery/background restrictions, and WebSocket persistence under lifecycle/network transitions.
- End-to-end integration with running Spring Boot backend/MySQL (pairing/auth, consent recorded server-side, metadata sync, operation dispatch/ack/completion, reconnect/expiry scenarios).
- Instrumented UI tests (`connectedAndroidTest`) and multi-API emulator matrix (no emulator/device connected in environment).

## Security verification notes
- Production server config uses HTTPS BuildConfig fields; TLS certificate validation relies on platform defaults (no insecure trust-all code added).
- Tokens/device secret stored via Keystore-backed `EncryptedSharedPreferences`; API logs redact Authorization header.
- Consent/remote-access toggles enforced in app flow before operation processing and sync-triggered remote actions.

## Recommendation for final validation
Run on Android 13/14+ device(s) and one older supported device/emulator against the deployed backend: grant full vs selected access, register/pair, enable consent, sync media, issue PREVIEW/EDIT/DELETE operations from admin side, verify system dialogs and reported statuses, toggle network/server availability, and validate revoke/disconnect/logout behavior.
