package com.example.weather

import com.example.core.AppLanguage
import kotlin.math.cos
import kotlin.math.sin

enum class WeatherPreset(
    val en: String,
    val bn: String,
    val rainIntensity: Float,
    val fogDensity: Float,
    val baseWindKts: Float,
    val baseTurbulence: Float,
    val cloudCoverage: Float,
    val hasLightning: Boolean
) {
    CLEAR("Clear Sky", "পরিষ্কার আকাশ", 0f, 0.002f, 6f, 0.05f, 0.15f, false),
    CLOUDY("Cloudy", "মেঘলা", 0f, 0.006f, 11f, 0.22f, 0.65f, false),
    LIGHT_RAIN("Light Rain", "হালকা বৃষ্টি", 0.35f, 0.012f, 15f, 0.38f, 0.80f, false),
    HEAVY_RAIN("Heavy Monsoon", "ভারী বর্ষা", 0.80f, 0.024f, 24f, 0.65f, 0.95f, false),
    THUNDERSTORM("Thunderstorm", "কালবৈশাখী ঝড়", 1.0f, 0.028f, 34f, 0.95f, 1.0f, true),
    FOG("Winter Fog", "ঘন কুয়াশা", 0f, 0.055f, 5f, 0.10f, 0.55f, false),
    STRONG_WIND("Strong Crosswind", "তীব্র ঝোড়ো বাতাস", 0.10f, 0.008f, 38f, 0.75f, 0.50f, false);

    fun title(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) bn else en
}

enum class TimeOfDay(
    val en: String,
    val bn: String,
    val sunElevationDeg: Float,
    val skyTopRgb: FloatArray,
    val skyHorizonRgb: FloatArray,
    val ambientLight: Float
) {
    SUNRISE("Sunrise", "সূর্যোদয়", 12f, floatArrayOf(0.14f, 0.24f, 0.45f), floatArrayOf(0.95f, 0.56f, 0.32f), 0.72f),
    DAY("Daylight", "দিন", 58f, floatArrayOf(0.11f, 0.38f, 0.74f), floatArrayOf(0.64f, 0.83f, 0.96f), 1.0f),
    SUNSET("Sunset", "সূর্যাস্ত", 8f, floatArrayOf(0.16f, 0.16f, 0.38f), floatArrayOf(0.92f, 0.38f, 0.24f), 0.68f),
    NIGHT("Night", "রাত", -18f, floatArrayOf(0.02f, 0.04f, 0.09f), floatArrayOf(0.06f, 0.11f, 0.20f), 0.28f);

    fun title(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) bn else en
}

enum class SeasonType(
    val en: String,
    val bn: String,
    val turbulenceMultiplier: Float,
    val fogBoost: Float
) {
    SUMMER_MONSOON("Monsoon Season", "বর্ষাকাল", 1.25f, 0.005f),
    AUTUMN_CLEAR("Autumn", "শরৎকাল", 0.85f, 0.0f),
    WINTER_FOG("Winter", "শীতকাল", 0.90f, 0.018f);

    fun title(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) bn else en
}

enum class WindDirectionMode(val en: String, val bn: String) {
    HEADWIND("Headwind", "সম্মুখ বায়ু"),
    CROSSWIND("Crosswind", "পার্শ্ব বায়ু"),
    TAILWIND("Tailwind", "পশ্চাৎ বায়ু");

    fun title(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) bn else en
}

enum class TurbulenceLevel(val en: String, val bn: String) {
    CALM("Calm", "শান্ত"),
    LIGHT("Light Turbulence", "হালকা ঝাঁকুনি"),
    MODERATE("Moderate Turbulence", "মাঝারি ঝাঁকুনি"),
    SEVERE("Severe Turbulence", "তীব্র ঝাঁকুনি");

    fun title(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) bn else en
}

data class WeatherState(
    val preset: WeatherPreset = WeatherPreset.CLEAR,
    val timeOfDay: TimeOfDay = TimeOfDay.DAY,
    val season: SeasonType = SeasonType.AUTUMN_CLEAR,
    val windMode: WindDirectionMode = WindDirectionMode.CROSSWIND,
    val windSpeedKts: Float = 8f,
    val windDirectionDeg: Float = 210f,
    val turbulenceIntensity: Float = 0.08f,
    val gustPitchOffset: Float = 0f,
    val gustRollOffset: Float = 0f,
    val gustYawOffset: Float = 0f,
    val cameraShakeAmount: Float = 0f,
    val lightningFlash: Float = 0f,
    val passengerAnxiety: Float = 0f
) {
    val turbulenceLevel: TurbulenceLevel
        get() = when {
            turbulenceIntensity < 0.15f -> TurbulenceLevel.CALM
            turbulenceIntensity < 0.40f -> TurbulenceLevel.LIGHT
            turbulenceIntensity < 0.70f -> TurbulenceLevel.MODERATE
            else -> TurbulenceLevel.SEVERE
        }
}

class DynamicWeatherController {
    private var simClock = 0f
    private var lightningTimer = 0f

    fun createInitialState(
        preset: WeatherPreset,
        timeOfDay: TimeOfDay,
        season: SeasonType,
        windMode: WindDirectionMode,
        runwayHeadingDeg: Float
    ): WeatherState {
        val windDir = when (windMode) {
            WindDirectionMode.HEADWIND -> runwayHeadingDeg
            WindDirectionMode.CROSSWIND -> (runwayHeadingDeg + 90f) % 360f
            WindDirectionMode.TAILWIND -> (runwayHeadingDeg + 180f) % 360f
        }
        val turb = (preset.baseTurbulence * season.turbulenceMultiplier).coerceIn(0f, 1.2f)
        return WeatherState(
            preset = preset,
            timeOfDay = timeOfDay,
            season = season,
            windMode = windMode,
            windSpeedKts = preset.baseWindKts,
            windDirectionDeg = windDir,
            turbulenceIntensity = turb
        )
    }

    fun step(state: WeatherState, dt: Float, altitudeFt: Float, airspeedKts: Float): WeatherState {
        simClock += dt
        val altFactor = if (altitudeFt < 6000f) 1.0f else 0.65f
        val speedFactor = (airspeedKts / 220f).coerceIn(0.25f, 1.3f)
        val rawTurb = (state.preset.baseTurbulence * state.season.turbulenceMultiplier * altFactor).coerceIn(0f, 1.2f)

        val wave1 = sin(simClock * 3.1f) * cos(simClock * 1.7f)
        val wave2 = sin(simClock * 5.3f + 1.2f)
        val wave3 = cos(simClock * 4.4f + 2.1f)

        val gustScale = rawTurb * speedFactor
        val pitchGust = wave1 * gustScale * 3.8f
        val rollGust = wave2 * gustScale * 6.5f
        val yawGust = wave3 * gustScale * 2.4f
        val shake = if (airspeedKts > 20f) (rawTurb * 0.85f * speedFactor).coerceIn(0f, 1f) else 0f

        var flash = (state.lightningFlash - dt * 2.8f).coerceAtLeast(0f)
        if (state.preset.hasLightning) {
            lightningTimer -= dt
            if (lightningTimer <= 0f) {
                flash = 1.0f
                lightningTimer = 4.5f + ((simClock * 7.3f) % 5.0f)
            }
        }

        val windGust = state.preset.baseWindKts + sin(simClock * 0.45f) * (state.preset.baseWindKts * 0.25f)
        val anxiety = (rawTurb * 0.7f + if (state.preset.hasLightning) 0.25f else 0f).coerceIn(0f, 1f)

        return state.copy(
            windSpeedKts = windGust.coerceAtLeast(0f),
            turbulenceIntensity = rawTurb,
            gustPitchOffset = pitchGust,
            gustRollOffset = rollGust,
            gustYawOffset = yawGust,
            cameraShakeAmount = shake,
            lightningFlash = flash,
            passengerAnxiety = anxiety
        )
    }
}
