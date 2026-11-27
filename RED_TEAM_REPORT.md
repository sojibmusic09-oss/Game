# Red Team & Engineering Audit Report

## Findings & Fixes Applied During Development
1. **BD-72 Turboprop Takeoff Thrust & Flap Lift Calibration** (`FlightPhysicsEngine.kt`, `AircraftCatalog.kt`)
   - *Finding*: Automated physics test revealed the regional BD-72 (`atr72`) took excessive runway distance before achieving `liftN > weightN` with `52000 N` thrust and `0.24` flap CL bonus.
   - *Fix*: Increased BD-72 static takeoff thrust to `74000 N` and calibrated high-lift flap aerodynamics (`flapsLiftBonus = flapsLevel * 0.34f`, `clBase = 0.40f + flapsLiftBonus + aoaDeg * 0.095f`) plus responsive ground rotation authority. Re-tested and verified liftoff + positive climb.
2. **AudioTrack Format Builder Compilation Defect** (`AviationAudioEngine.kt`)
   - *Finding*: `AudioFormat.AudioFormatProperties.Builder()` failed Kotlin compilation on Android SDK 36.
   - *Fix*: Replaced with standard `android.media.AudioFormat.Builder()` and re-verified compilation.
3. **Robolectric Concurrent Artifact Download Rate Limit (HTTP 429)** (`BdasEngineTests.kt`, `BdasRoomRobolectricTest.kt`)
   - *Finding*: Parallel Robolectric workers triggered Maven Central HTTP 429 during sandbox initialization.
   - *Fix*: Separated pure-JVM engine tests into `BdasEngineTests.kt` and Room Android-context persistence into `BdasRoomRobolectricTest.kt`, executed with `--max-workers=1`. All 10/10 tests passed.
4. **Zero-Trust Auth & Firestore Hygiene Audit**
   - *Check*: `grep` scan across `app/src/main/` confirmed 0 matches for `signInAnonymously`, `signInWithEmailAndPassword`, `.apps.googleusercontent.com`, or parameterless `FirebaseFirestore.getInstance()`.
   - *Check*: `firestore.rules` audited 1:1 against `CloudSyncRepository.kt` and `MultiplayerRepository.kt` (including avoiding `is`-prefixed Kotlin Booleans via `active` on `AirlineRouteDoc`). Rules deployed cleanly via `fax.DeployRules`.

## Known Runtime Limitations (No Emulator / ADB in Container)
- Interactive touch gestures, OpenGL ES 3.0 visual frame output, real Google Credential Manager sign-in sheet, and device TTS/AudioTrack playback require a physical Android device or the streaming emulator outside the headless build container and were not executed via `adb` inside this environment.
