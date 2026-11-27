package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.BdasStrings
import com.example.data.PilotProfile
import com.example.data.UserSettings
import com.example.graphics3d.GraphicsPreset
import com.example.missions.MissionCatalog
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun AchievementsScreen(
    language: AppLanguage,
    profile: PilotProfile?,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit
) {
    val unlocked = profile?.unlockedAchievementIds ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitVoid)
    ) {
        BdasTopBar(
            title = BdasStrings.achievements(language),
            language = language,
            profile = profile,
            onBack = onBack,
            onToggleLanguage = onToggleLanguage
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(14.dp)
        ) {
            items(MissionCatalog.achievements, key = { it.id }) { ach ->
                val isDone = ach.id in unlocked
                Surface(
                    color = if (isDone) BangladeshEmeraldDeep.copy(alpha = 0.65f) else InstrumentSlate,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isDone) BangladeshEmerald else InstrumentBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = if (isDone) BangladeshEmerald else WarningAmber,
                            modifier = Modifier.size(30.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(ach.title.get(language), style = MaterialTheme.typography.titleMedium, color = TextInstrumentWhite)
                            Text(ach.description.get(language), style = MaterialTheme.typography.bodyMedium, color = TextMutedSlate)
                        }
                        if (!isDone) {
                            Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = TextMutedSlate, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    language: AppLanguage,
    profile: PilotProfile?,
    settings: UserSettings,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit,
    onUpdateSettings: (
        beginnerAssist: Boolean?,
        leftHanded: Boolean?,
        sensitivity: Float?,
        haptics: Boolean?,
        graphicsPreset: GraphicsPreset?,
        masterVol: Float?,
        engineVol: Float?,
        cabinVol: Float?
    ) -> Unit,
    onSignOut: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitVoid)
    ) {
        BdasTopBar(
            title = BdasStrings.settings(language),
            language = language,
            profile = profile,
            onBack = onBack,
            onToggleLanguage = onToggleLanguage
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(
                    color = InstrumentSlate,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, InstrumentBorder)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle(if (isBn) "কন্ট্রোল ও ভাষা" else "Flight Controls & Language")
                        SettingToggleRow(
                            title = if (isBn) "বিগিনার অ্যাসিস্ট (অটো-স্ট্যাবিলাইজার)" else "Beginner Assist (Auto-Leveling & Stability)",
                            checked = settings.beginnerAssist,
                            tag = "setting_beginner_assist"
                        ) { onUpdateSettings(it, null, null, null, null, null, null, null) }
                        SettingToggleRow(
                            title = if (isBn) "বাম-হাতের কন্ট্রোল লেআউট" else "Left-Handed Controls Mirror",
                            checked = settings.leftHandedControls,
                            tag = "setting_left_handed"
                        ) { onUpdateSettings(null, it, null, null, null, null, null, null) }
                        SettingToggleRow(
                            title = if (isBn) "হ্যাপটিক ভাইব্রেশন ফিডব্যাক" else "Haptic Tactile Feedback",
                            checked = settings.hapticsEnabled,
                            tag = "setting_haptics"
                        ) { onUpdateSettings(null, null, null, it, null, null, null, null) }
                        Text(
                            text = "${if (isBn) "জয়স্টিক সংবেদনশীলতা" else "Control Sensitivity"}: ${"%.1f".format(settings.touchSensitivity)}x",
                            style = MaterialTheme.typography.labelLarge,
                            color = HudCyan
                        )
                        Slider(
                            value = settings.touchSensitivity,
                            onValueChange = { onUpdateSettings(null, null, it, null, null, null, null, null) },
                            valueRange = 0.4f..1.8f,
                            modifier = Modifier.testTag("setting_sensitivity_slider")
                        )
                    }
                }
            }

            item {
                Surface(
                    color = InstrumentSlate,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, InstrumentBorder)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle(if (isBn) "৩ডি গ্রাফিক্স ও অডিও" else "3D Graphics Quality & Audio Mix")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GraphicsPreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = settings.graphicsPreset == preset,
                                    onClick = { onUpdateSettings(null, null, null, null, preset, null, null, null) },
                                    label = { Text(preset.name) },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("gfx_preset_${preset.name.lowercase()}")
                                )
                            }
                        }
                        Text("Master Volume: ${(settings.masterVolume * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = TextInstrumentWhite)
                        Slider(
                            value = settings.masterVolume,
                            onValueChange = { onUpdateSettings(null, null, null, null, null, it, null, null) },
                            valueRange = 0f..1f
                        )
                        Text("Engine Audio: ${(settings.engineVolume * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = TextInstrumentWhite)
                        Slider(
                            value = settings.engineVolume,
                            onValueChange = { onUpdateSettings(null, null, null, null, null, null, it, null) },
                            valueRange = 0f..1f
                        )
                        Text("Cabin TTS Announcements: ${(settings.cabinVolume * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = TextInstrumentWhite)
                        Slider(
                            value = settings.cabinVolume,
                            onValueChange = { onUpdateSettings(null, null, null, null, null, null, null, it) },
                            valueRange = 0f..1f
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = CautionCrimsonContainer),
                    border = BorderStroke(1.dp, CautionCrimson),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp).testTag("settings_sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = CautionCrimson)
                    Spacer(Modifier.width(8.dp))
                    Text(BdasStrings.signOut(language), color = CautionCrimson)
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = TextInstrumentWhite, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
