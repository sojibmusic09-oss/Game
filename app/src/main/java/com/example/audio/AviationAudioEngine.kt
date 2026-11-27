package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import com.example.core.AppLanguage
import kotlinx.coroutines.*
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class AviationAudioEngine(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private var ttsReady = false

    private var audioJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile var masterVolume: Float = 0.8f
    @Volatile var engineVolume: Float = 0.7f
    @Volatile var cabinVolume: Float = 0.9f

    @Volatile private var targetThrottle: Float = 0f
    @Volatile private var targetAirspeedKts: Float = 0f
    @Volatile private var targetRain: Float = 0f
    @Volatile private var targetTurbulence: Float = 0f
    @Volatile private var isTurboprop: Boolean = true
    @Volatile private var stallBeep: Boolean = false
    @Volatile private var engineRunning: Boolean = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            tts?.language = Locale.US
        }
    }

    fun startFlightAudio() {
        if (audioJob?.isActive == true) return
        engineRunning = true
        audioJob = scope.launch {
            val sampleRate = 22050
            val bufSamples = 1024
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(bufSamples * 2)

            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (_: Exception) {
                return@launch
            }

            try {
                track.play()
                val pcm = ShortArray(bufSamples)
                var phase1 = 0.0
                var phase2 = 0.0
                var alertPhase = 0.0
                var noiseFilter = 0f

                while (isActive && engineRunning) {
                    val mVol = masterVolume.coerceIn(0f, 1f)
                    val eVol = engineVolume.coerceIn(0f, 1f) * mVol
                    val thr = targetThrottle.coerceIn(0f, 1f)
                    val spd = (targetAirspeedKts / 350f).coerceIn(0f, 1.2f)
                    val rain = targetRain.coerceIn(0f, 1f)
                    val turb = targetTurbulence.coerceIn(0f, 1f)

                    val baseFreq = if (isTurboprop) 52.0 + thr * 68.0 else 88.0 + thr * 145.0
                    val harmonicFreq = baseFreq * (if (isTurboprop) 2.0 else 2.5)

                    for (i in pcm.indices) {
                        phase1 += 2.0 * PI * baseFreq / sampleRate
                        phase2 += 2.0 * PI * harmonicFreq / sampleRate
                        if (phase1 > 2.0 * PI) phase1 -= 2.0 * PI
                        if (phase2 > 2.0 * PI) phase2 -= 2.0 * PI

                        val rawNoise = (Random.nextFloat() * 2f - 1f)
                        noiseFilter = noiseFilter * 0.85f + rawNoise * 0.15f

                        val engineTone = (sin(phase1) * 0.55 + sin(phase2) * 0.25) * (0.15 + thr * 0.55) * eVol
                        val windAndRain = noiseFilter * (spd * 0.28f + rain * 0.32f + turb * 0.22f) * mVol

                        var alert = 0.0
                        if (stallBeep) {
                            alertPhase += 2.0 * PI * 780.0 / sampleRate
                            if (alertPhase > 2.0 * PI) alertPhase -= 2.0 * PI
                            alert = sin(alertPhase) * 0.28 * mVol
                        }

                        val sample = ((engineTone + windAndRain + alert) * 14000.0)
                            .toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        pcm[i] = sample.toShort()
                    }
                    track.write(pcm, 0, pcm.size)
                }
            } catch (_: Exception) {
                // Ignore AudioTrack stop race
            } finally {
                runCatching { track.stop() }
                runCatching { track.release() }
            }
        }
    }

    fun updateTelemetry(
        throttle: Float,
        airspeedKts: Float,
        rainIntensity: Float,
        turbulence: Float,
        turboprop: Boolean,
        stallWarning: Boolean
    ) {
        targetThrottle = throttle
        targetAirspeedKts = airspeedKts
        targetRain = rainIntensity
        targetTurbulence = turbulence
        isTurboprop = turboprop
        stallBeep = stallWarning
    }

    fun stopFlightAudio() {
        engineRunning = false
        audioJob?.cancel()
        audioJob = null
    }

    fun speakAnnouncement(text: String, lang: AppLanguage) {
        if (!ttsReady || masterVolume <= 0.05f || cabinVolume <= 0.05f) return
        val locale = if (lang == AppLanguage.BANGLA) Locale.forLanguageTag("bn-BD") else Locale.US
        val res = tts?.setLanguage(locale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.language = Locale.US
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bdas_${System.currentTimeMillis()}")
    }

    fun shutdown() {
        stopFlightAudio()
        scope.cancel()
        runCatching { tts?.stop(); tts?.shutdown() }
        tts = null
    }
}
