# Bangladesh Airline Simulator (BDAS) — Development Status

**Last Verified:** 2026-11-27
**Target Runtime:** Native Android Kotlin + Jetpack Compose + OpenGL ES 3.0 (`compileSdk = 36`, `minSdk = 24`, `targetSdk = 36`, `sensorLandscape`)
**Application ID:** `com.aistudio.bdas.qmkxrp`

## Build & Verification Summary
- **Full Android Build (`compile_applet`)**: **PASSED** (`Build succeeded - the applet is compiled`).
- **Unit & Robolectric Tests (`gradle :app:testDebugUnitTest --max-workers=1`)**: **PASSED** (10 tests completed, 0 failed across `BdasEngineTests`, `BdasRoomRobolectricTest`, `ExampleRobolectricTest`, `ExampleUnitTest`).
- **Firestore Security Rules Emulator Tests (`node --test firestore.test.js`)**: **PASSED** (4/4 tests passed against local Firestore emulator on `127.0.0.1:8085`).
- **Firestore Production Rules Deployment (`fax.DeployRules`)**: **SUCCEEDED** to Firebase project `gen-lang-client-0034638007` / database `ai-studio-android-bdas-9df30ef1-3478-4357-ba91-1b818a13ed32`.

## Implemented Systems
1. **3D Engine (`graphics3d`)**: OpenGL ES 3.0 renderer (`BdasGlRenderer`, `MeshBuilders`, `ShaderUtils`, `BdasViewport3D`) supporting Cockpit, Chase, Wing, and ATC Tower cameras, procedural Bangladesh terrain/river/airport geometry, 3D terminal & cabin interiors, weather lighting/fog/rain particles, multiplayer peer aircraft, and 4 graphics presets (`LOW`, `MEDIUM`, `HIGH`, `ULTRA`).
2. **Flight Physics (`flightphysics`)**: 6-DOF aerodynamic solver (`FlightPhysicsEngine`) modeling lift, induced/parasitic drag, flaps, gear, gear-jammed belly landings, single-engine asymmetric yaw, fuel mass burn, wind drift, braking friction, and crash/touchdown FPM evaluation.
3. **Dynamic Weather (`weather`)**: 7 weather presets (`CLEAR`, `CLOUDY`, `LIGHT_RAIN`, `HEAVY_RAIN`, `THUNDERSTORM`, `FOG`, `STRONG_WIND`), 4 times of day, 3 Bangladeshi seasons, headwind/crosswind/tailwind vectors, lightning, and turbulence gusts.
4. **Aircraft & Liveries (`aircraft`)**: Modular configurations for `atr72` (BD-72), `a320` (BD-320), `b737` (BD-738), and `b777` (BD-777) with 3 custom liveries.
5. **Missions & Career (`missions`)**: 6 Pilot Ranks, Training/Career exams, all 8 Emergency Missions, and 10 Achievements.
6. **Bangladesh GIS World (`world`)**: All 8 Divisions, all 64 Districts with authentic coordinates and biomes, 3 major rivers (Padma, Jamuna, Meghna), Padma & Jamuna bridges, and 8 Bangladeshi airports (`VGHS`, `VGEG`, `VGSY`, `VGCB`, `VGRJ`, `VGJR`, `VGSD`, `VGBR`).
7. **Passenger Mode (`passenger`)**: 15-stage interactive terminal-to-cabin journey with first/third-person movement, look controls, seat selection, and bilingual cabin crew TTS.
8. **Airline Economy & Cloud Sync (`airline`, `data`)**: Demand elasticity engine, cycle P/L simulation, Google Sign-In via Jetpack Credential Manager, Firestore cloud sync with offline persistence, and online multiplayer lobbies + 1Hz telemetry sharing.
9. **Local Flight Replay & Audio (`replay`, `audio`)**: Room database recording 4Hz flight telemetry frames with 3D playback scrubbing, real-time PCM engine/wind/rain/stall `AudioTrack` synthesis, and EN/বাংলা `TextToSpeech`.
