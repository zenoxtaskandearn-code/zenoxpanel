# BUILD_INSTRUCTIONS.md

## Prerequisites
- Windows/macOS/Linux with Android SDK installed
- JDK 17 (tested with Microsoft JDK 17)
- Android SDK Platform 36, Build-Tools (installed/available), Platform-Tools
- Internet access for Gradle dependency resolution

## Project configuration
1. Ensure `local.properties` points to your SDK, e.g.:
   - `sdk.dir=C\:\\Users\\<user>\\AppData\\Local\\Android\\Sdk`
2. (Optional) Update backend URLs in `app/build.gradle.kts` `BuildConfigField`s:
   - `DEV_BASE_URL`, `PROD_BASE_URL`, `DEV_WS_URL`, `PROD_WS_URL`
   - or set runtime override in app Settings.

## Build (CLI)
From `android/` project root:

```powershell
# Debug APK
.\gradlew.bat :app:assembleDebug

# Release APK (currently configured to use debug signing for local install)
.\gradlew.bat :app:assembleRelease

# Unit tests
.\gradlew.bat :app:testDebugUnitTest
```

If wrapper execution is unavailable in your environment, use a locally installed Gradle 8.9+ compatible with AGP 8.7.x:
```powershell
gradle :app:assembleDebug
```

## Outputs
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`
- Unit test reports: `app/build/reports/tests/testDebugUnitTest/index.html`

## Signed APK (release) instructions
Current `release` build type sets `signingConfig = signingConfigs.getByName("debug")` to allow generating an installable artifact in this environment. For production signing:

1. Generate a keystore:
```bash
keytool -genkeypair -v -keystore remote-gallery-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias remotegallery
```
2. Create `keystore.properties` (do **not** commit secrets):
```properties
storeFile=../remote-gallery-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=remotegallery
keyPassword=YOUR_KEY_PASSWORD
```
3. Update `app/build.gradle.kts`:
- Load `keystore.properties`
- Add `signingConfigs { create("release") { ... } }`
- Set `buildTypes.release.signingConfig = signingConfigs.getByName("release")`
- Enable minification/shrinking as desired (`isMinifyEnabled = true`) and validate ProGuard/R8 rules
4. Build:
```powershell
.\gradlew.bat :app:assembleRelease
# or bundle for Play/internal distribution
.\gradlew.bat :app:bundleRelease
```

## Notes
- `usesCleartextTraffic` is enabled to support local dev (`10.0.2.2`) over HTTP; production deployments should use HTTPS Render backend URLs.
- Some Compose APIs emit deprecation warnings; build remains successful.
