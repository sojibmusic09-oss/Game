# Build & Setup Instructions

## Prerequisites
- Android SDK / Gradle 9.3.1 (pre-configured in AI Studio; uses `ANDROID_SDK_ROOT`, no `local.properties`).
- Firebase configuration already provisioned in `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml` (project `gen-lang-client-0034638007`).

## Commands
- **Debug Kotlin Compile**: `gradle :app:compileDebugKotlin`
- **Unit & Robolectric Tests**: `gradle :app:testDebugUnitTest --max-workers=1`
- **Debug APK Build**: `gradle :app:assembleDebug` (or `compile_applet` in AI Studio)
- **Firestore Rules Emulator Tests**:
  1. Start emulator: `firebase emulators:start --only firestore`
  2. Run suite: `FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js`
