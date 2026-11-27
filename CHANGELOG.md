# Changelog

## v1.0.0 — Full Vertical Slice Release (2026-11-27)
- Provisioned Firebase Auth (Google Sign-In via Credential Manager), Cloud Firestore (`ai-studio-android-bdas-9df30ef1-3478-4357-ba91-1b818a13ed32`), and hardened zero-trust `firestore.rules`.
- Built OpenGL ES 3.0 3D engine (`BdasGlRenderer`, `MeshBuilders`, `BdasViewport3D`) for flight, 3D terminal, and 3D cabin scenes with 4 performance presets.
- Implemented 6-DOF `FlightPhysicsEngine`, `DynamicWeatherController`, 4 aircraft (`atr72`, `a320`, `b737`, `b777`), 8 Emergency Missions, Career Ranks, and 10 Achievements.
- Implemented complete Bangladesh GIS dataset (8 divisions, 64 districts, 3 rivers, 2 bridges, 8 airports) with interactive vector map.
- Built 15-stage interactive 3D Passenger Simulator, Airline Management & Economy simulator, Online Multiplayer lobbies/telemetry, Room 3D Flight Replay, EN/বাংলা bilingual UI, and real-time audio synthesis.
- Verified full APK build (`compile_applet`), 10/10 JVM/Robolectric tests, and 4/4 Firestore rules emulator tests.
