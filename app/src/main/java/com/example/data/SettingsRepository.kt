package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.AppLanguage
import com.example.graphics3d.GraphicsPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.bdasDataStore: DataStore<Preferences> by preferencesDataStore(name = "bdas_settings")

data class UserSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val beginnerAssist: Boolean = true,
    val leftHandedControls: Boolean = false,
    val touchSensitivity: Float = 1.0f,
    val hapticsEnabled: Boolean = true,
    val graphicsPreset: GraphicsPreset = GraphicsPreset.HIGH,
    val masterVolume: Float = 0.85f,
    val engineVolume: Float = 0.75f,
    val cabinVolume: Float = 0.90f
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val BEGINNER_ASSIST = booleanPreferencesKey("beginner_assist")
        val LEFT_HANDED = booleanPreferencesKey("left_handed")
        val TOUCH_SENSITIVITY = floatPreferencesKey("touch_sensitivity")
        val HAPTICS = booleanPreferencesKey("haptics")
        val GRAPHICS_PRESET = stringPreferencesKey("graphics_preset")
        val MASTER_VOL = floatPreferencesKey("master_vol")
        val ENGINE_VOL = floatPreferencesKey("engine_vol")
        val CABIN_VOL = floatPreferencesKey("cabin_vol")
    }

    val settings: Flow<UserSettings> = context.bdasDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            val presetName = prefs[Keys.GRAPHICS_PRESET] ?: GraphicsPreset.HIGH.name
            val preset = GraphicsPreset.entries.firstOrNull { it.name == presetName } ?: GraphicsPreset.HIGH
            UserSettings(
                language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
                beginnerAssist = prefs[Keys.BEGINNER_ASSIST] ?: true,
                leftHandedControls = prefs[Keys.LEFT_HANDED] ?: false,
                touchSensitivity = prefs[Keys.TOUCH_SENSITIVITY] ?: 1.0f,
                hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
                graphicsPreset = preset,
                masterVolume = prefs[Keys.MASTER_VOL] ?: 0.85f,
                engineVolume = prefs[Keys.ENGINE_VOL] ?: 0.75f,
                cabinVolume = prefs[Keys.CABIN_VOL] ?: 0.90f
            )
        }

    suspend fun toggleLanguage(current: AppLanguage) {
        val next = if (current == AppLanguage.ENGLISH) AppLanguage.BANGLA else AppLanguage.ENGLISH
        context.bdasDataStore.edit { it[Keys.LANGUAGE] = next.code }
    }

    suspend fun setBeginnerAssist(enabled: Boolean) {
        context.bdasDataStore.edit { it[Keys.BEGINNER_ASSIST] = enabled }
    }

    suspend fun setLeftHanded(enabled: Boolean) {
        context.bdasDataStore.edit { it[Keys.LEFT_HANDED] = enabled }
    }

    suspend fun setTouchSensitivity(value: Float) {
        context.bdasDataStore.edit { it[Keys.TOUCH_SENSITIVITY] = value.coerceIn(0.4f, 1.8f) }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.bdasDataStore.edit { it[Keys.HAPTICS] = enabled }
    }

    suspend fun setGraphicsPreset(preset: GraphicsPreset) {
        context.bdasDataStore.edit { it[Keys.GRAPHICS_PRESET] = preset.name }
    }

    suspend fun setVolumes(master: Float, engine: Float, cabin: Float) {
        context.bdasDataStore.edit {
            it[Keys.MASTER_VOL] = master.coerceIn(0f, 1f)
            it[Keys.ENGINE_VOL] = engine.coerceIn(0f, 1f)
            it[Keys.CABIN_VOL] = cabin.coerceIn(0f, 1f)
        }
    }
}
