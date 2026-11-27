# Asset Manifest

## Bundled Fonts (`/app/src/main/res/font/`)
- `space_grotesk.ttf` — Display & cockpit headlines
- `plus_jakarta_sans.ttf` — Body & UI typography
- `jetbrains_mono.ttf` — Telemetry, ICAO, FPM & numeric readouts
- `noto_sans_bengali.ttf` — Full বাংলা script rendering

## Generated Visual Assets (`/app/src/main/res/drawable/` & `mipmap-*`)
- `bdas_app_icon_1795772242153.jpg` — Custom adaptive launcher source emblem (corner backdrop `#071A2B`)
- `img_bdas_hero_1795772244695.jpg` — Command Deck & Sign-In hero illustration
- `ic_launcher_foreground.xml`, `ic_launcher_background.xml`, `ic_launcher.png`, `ic_launcher_round.png` (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) — Custom adaptive & legacy launcher icons

## Procedural 3D & Audio Assets (Runtime Generated)
- Procedural OpenGL ES 3.0 meshes (`MeshBuilders.kt`): Regional turboprop, narrow-body twinjet, wide-body twinjet, Bangladesh terrain/rivers/bridges, runway/terminal/control tower, terminal hall & cabin interior, volumetric cloud puffs & rain streaks.
- Procedural PCM Audio (`AviationAudioEngine.kt`): Dynamic engine frequency + harmonic synthesis, airflow/rain noise filtering, and stall warning tone.
