package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.aircraft.AircraftCatalog
import com.example.core.AppLanguage
import com.example.data.UserSettings
import com.example.flightphysics.CameraMode
import com.example.flightphysics.FlightControlInput
import com.example.flightphysics.FlightState
import com.example.graphics3d.BdasViewport3D
import com.example.graphics3d.PeerAircraftRender
import com.example.graphics3d.Scene3DMode
import com.example.missions.MissionCatalog
import com.example.ui.theme.*
import com.example.viewmodel.FlightBriefingConfig
import com.example.viewmodel.FlightDebriefReport
import com.example.weather.WeatherState
import com.example.world.BangladeshGeoData
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun InFlightScreen(
    language: AppLanguage,
    settings: UserSettings,
    briefing: FlightBriefingConfig,
    flightState: FlightState,
    controlInput: FlightControlInput,
    weatherState: WeatherState,
    cameraMode: CameraMode,
    paused: Boolean,
    debriefReport: FlightDebriefReport?,
    peerAircraft: List<PeerAircraftRender>,
    onUpdateControls: ((FlightControlInput) -> FlightControlInput) -> Unit,
    onCycleCamera: () -> Unit,
    onTogglePause: () -> Unit,
    onEndFlightEarly: () -> Unit,
    onWatchReplay: (Long) -> Unit,
    onExitToMenu: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val haptics = LocalHapticFeedback.current
    val spec = remember(briefing.aircraftId) { AircraftCatalog.findById(briefing.aircraftId) }
    val livery = remember(briefing.liveryId) { AircraftCatalog.findLivery(briefing.liveryId) }
    val originAirport = remember(briefing.originIcao) { BangladeshGeoData.findAirport(briefing.originIcao) }
    val nearestDistrict = remember(flightState.latitude, flightState.longitude) {
        BangladeshGeoData.nearestDistrict(flightState.latitude, flightState.longitude)
    }
    val mission = remember(briefing.missionId) { MissionCatalog.findById(briefing.missionId) }

    BackHandler { onTogglePause() }

    Box(modifier = Modifier.fillMaxSize()) {
        BdasViewport3D(
            modifier = Modifier.fillMaxSize(),
            sceneMode = Scene3DMode.FLIGHT,
            flightState = flightState,
            aircraftSpec = spec,
            livery = livery,
            weatherState = weatherState,
            cameraMode = cameraMode,
            graphicsPreset = settings.graphicsPreset,
            runwayHeadingDeg = originAirport.runwayHeadingDeg,
            runwayLengthM = originAirport.runwayLengthM,
            groundElevMeters = originAirport.elevationFt * 0.3048f,
            peerAircraft = peerAircraft
        )

        // HUD Overlay respecting safe areas
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Telemetry & Warning Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = CockpitVoid.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, InstrumentBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelemetryReadout("SPD", "${flightState.airspeedKts.roundToInt()} KT", HudCyan)
                        TelemetryReadout("ALT", "${flightState.altitudeFt.roundToInt()} FT", TextInstrumentWhite)
                        TelemetryReadout("V/S", "${flightState.verticalSpeedFpm} FPM", if (flightState.verticalSpeedFpm < -600) WarningAmber else BangladeshEmerald)
                        TelemetryReadout("HDG", "${flightState.headingDeg.roundToInt()}°", TextInstrumentWhite)
                        TelemetryReadout("FUEL", "${((flightState.fuelKg / spec.maxFuelKg) * 100f).roundToInt()}%", if (flightState.lowFuelWarning) CautionCrimson else BangladeshEmerald)
                    }
                }

                // Artificial Horizon + Location District Badge
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ArtificialHorizonWidget(
                        pitchDeg = flightState.pitchDeg,
                        rollDeg = flightState.rollDeg
                    )
                    Surface(
                        color = CockpitVoid.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${if (isBn) nearestDistrict.nameBn else nearestDistrict.nameEn} • ${weatherState.turbulenceLevel.title(language)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = HudCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onCycleCamera,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("camera_cycle_button")
                    ) {
                        Icon(Icons.Filled.Videocam, contentDescription = "Camera", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(cameraMode.title(isBn))
                    }
                    FilledTonalIconButton(
                        onClick = onTogglePause,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("flight_pause_button")
                    ) {
                        Icon(Icons.Filled.Pause, contentDescription = "Pause Flight")
                    }
                }
            }

            // Center Warnings / Emergency Objective Banner
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (flightState.stallWarning) {
                    AlertBadge("STALL WARNING • NOSE DOWN & ADD THRUST", CautionCrimson)
                }
                if (flightState.gearWarning) {
                    AlertBadge("TOO LOW • EXTEND LANDING GEAR", WarningAmber)
                }
                if (flightState.engineLeftFailed) {
                    AlertBadge("LEFT ENGINE FAILURE • COUNTER YAW", CautionCrimson)
                }
                if (flightState.gearJammed) {
                    AlertBadge("GEAR JAMMED • EXECUTE SOFT BELLY LANDING", WarningAmber)
                }
                if (mission != null && flightState.elapsedSec < 12f) {
                    Surface(
                        color = CockpitDeepNavy.copy(alpha = 0.80f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, HudCyan)
                    ) {
                        Text(
                            text = mission.briefing.get(language),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextInstrumentWhite,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Bottom Flight Controls (Left-handed support!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val joystickComposable: @Composable () -> Unit = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        VirtualJoystickPad(
                            onAxisChanged = { roll, pitch ->
                                onUpdateControls { it.copy(rollInput = roll, pitchInput = pitch) }
                            },
                            onReleased = {
                                onUpdateControls { it.copy(rollInput = 0f, pitchInput = 0f) }
                            }
                        )
                        // Yaw / Rudder bar
                        Surface(
                            color = CockpitVoid.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, InstrumentBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("YAW", style = MaterialTheme.typography.labelSmall, color = TextMutedSlate)
                                Slider(
                                    value = controlInput.yawInput,
                                    onValueChange = { v -> onUpdateControls { it.copy(yawInput = v) } },
                                    onValueChangeFinished = { onUpdateControls { it.copy(yawInput = 0f) } },
                                    valueRange = -1f..1f,
                                    modifier = Modifier
                                        .width(120.dp)
                                        .testTag("yaw_slider")
                                )
                            }
                        }
                    }
                }

                val centerSystemsComposable: @Composable () -> Unit = {
                    Surface(
                        color = CockpitVoid.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, InstrumentBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (settings.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onUpdateControls { it.copy(flapsLevel = (it.flapsLevel + 1) % 4) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = InstrumentElevated),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("flaps_button")
                            ) {
                                Text("FLAPS ${controlInput.flapsLevel}/3", color = HudCyan)
                            }
                            Button(
                                onClick = {
                                    if (!flightState.gearJammed) {
                                        if (settings.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onUpdateControls { it.copy(gearDown = !it.gearDown) }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        flightState.gearJammed -> CautionCrimsonContainer
                                        controlInput.gearDown -> BangladeshEmeraldDeep
                                        else -> InstrumentElevated
                                    }
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("gear_button")
                            ) {
                                Text(
                                    text = if (flightState.gearJammed) "GEAR JAM" else if (controlInput.gearDown) "GEAR DN" else "GEAR UP",
                                    color = TextInstrumentWhite
                                )
                            }
                            Button(
                                onClick = {
                                    if (settings.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdateControls { it.copy(brakesActive = !it.brakesActive) }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (controlInput.brakesActive) CautionCrimson else InstrumentElevated
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("brake_button")
                            ) {
                                Text(if (controlInput.brakesActive) "BRK ON" else "BRAKE", color = TextInstrumentWhite)
                            }
                        }
                    }
                }

                val throttleComposable: @Composable () -> Unit = {
                    Surface(
                        color = CockpitVoid.copy(alpha = 0.80f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, InstrumentBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "THR ${(controlInput.throttle * 100f).roundToInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = WarningAmber
                            )
                            Slider(
                                value = controlInput.throttle,
                                onValueChange = { v -> onUpdateControls { it.copy(throttle = v) } },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = WarningAmber, activeTrackColor = BangladeshEmerald),
                                modifier = Modifier
                                    .width(150.dp)
                                    .testTag("throttle_slider")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { onUpdateControls { it.copy(throttle = 0f) } },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                                ) { Text("IDLE", style = MaterialTheme.typography.labelSmall) }
                                OutlinedButton(
                                    onClick = { onUpdateControls { it.copy(throttle = 1f) } },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                                ) { Text("TOGA", style = MaterialTheme.typography.labelSmall, color = BangladeshEmerald) }
                            }
                        }
                    }
                }

                if (settings.leftHandedControls) {
                    throttleComposable()
                    centerSystemsComposable()
                    joystickComposable()
                } else {
                    joystickComposable()
                    centerSystemsComposable()
                    throttleComposable()
                }
            }
        }

        // Pause Overlay
        if (paused && debriefReport == null) {
            AlertDialog(
                onDismissRequest = onTogglePause,
                title = { Text(if (isBn) "ফ্লাইট বিরতি" else "Flight Paused") },
                text = {
                    Text(
                        if (isBn) "আপনি ফ্লাইট চালিয়ে যেতে, রেকর্ড শেষ করতে বা মেনুতে ফিরতে পারেন।"
                        else "Resume flying, finish & log this flight now, or exit to the Command Deck."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onTogglePause,
                        modifier = Modifier.testTag("resume_flight_button")
                    ) { Text(if (isBn) "চালিয়ে যান" else "Resume") }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onEndFlightEarly,
                            modifier = Modifier.testTag("finish_log_flight_button")
                        ) { Text(if (isBn) "লগ ও রিপোর্ট" else "Finish & Log") }
                        TextButton(
                            onClick = onExitToMenu,
                            modifier = Modifier.testTag("exit_flight_button")
                        ) { Text(if (isBn) "মেনু" else "Exit", color = CautionCrimson) }
                    }
                }
            )
        }

        // Post-Flight Debrief Report Modal
        if (debriefReport != null) {
            AlertDialog(
                onDismissRequest = onExitToMenu,
                title = {
                    Text(
                        text = debriefReport.title,
                        color = if (debriefReport.success) BangladeshEmerald else CautionCrimson
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(debriefReport.summary)
                        Text("Score: ${debriefReport.score} | Touchdown: ${debriefReport.landingFpm} FPM", style = MaterialTheme.typography.labelLarge, color = HudCyan)
                        Text("+${debriefReport.xpEarned} XP • +${debriefReport.coinsEarned} BDAS Coins", style = MaterialTheme.typography.labelLarge, color = WarningAmber)
                        if (debriefReport.newAchievements.isNotEmpty()) {
                            Text("Achievements Unlocked: ${debriefReport.newAchievements.joinToString()}", color = BangladeshEmerald)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onExitToMenu,
                        modifier = Modifier.testTag("debrief_continue_button")
                    ) { Text(if (isBn) "সম্পন্ন" else "Continue") }
                },
                dismissButton = {
                    if (debriefReport.savedReplayId != null) {
                        OutlinedButton(
                            onClick = { onWatchReplay(debriefReport.savedReplayId) },
                            modifier = Modifier.testTag("debrief_replay_button")
                        ) { Text(if (isBn) "৩ডি রিপ্লে দেখুন" else "Watch 3D Replay") }
                    }
                }
            )
        }
    }
}

@Composable
private fun TelemetryReadout(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMutedSlate)
        Text(value, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun AlertBadge(text: String, color: Color) {
    Surface(
        color = CockpitVoid.copy(alpha = 0.88f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.5.dp, color)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ArtificialHorizonWidget(pitchDeg: Float, rollDeg: Float) {
    Surface(
        color = CockpitVoid.copy(alpha = 0.78f),
        shape = CircleShape,
        border = BorderStroke(1.5.dp, InstrumentBorder),
        modifier = Modifier.size(72.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val pitchOffset = (pitchDeg / 45f) * (size.height * 0.35f)
            rotate(-rollDeg, pivot = Offset(cx, cy)) {
                drawLine(
                    color = HudCyan,
                    start = Offset(6f, cy + pitchOffset),
                    end = Offset(size.width - 6f, cy + pitchOffset),
                    strokeWidth = 3f
                )
            }
            drawLine(
                color = WarningAmber,
                start = Offset(cx - 14f, cy),
                end = Offset(cx + 14f, cy),
                strokeWidth = 3f
            )
        }
    }
}

@Composable
private fun VirtualJoystickPad(
    onAxisChanged: (roll: Float, pitch: Float) -> Unit,
    onReleased: () -> Unit
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadiusPx = 56f

    Surface(
        color = CockpitVoid.copy(alpha = 0.74f),
        shape = CircleShape,
        border = BorderStroke(1.5.dp, InstrumentBorder),
        modifier = Modifier
            .size(124.dp)
            .testTag("virtual_joystick")
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onReleased()
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onReleased()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val raw = knobOffset + dragAmount
                        val dist = hypot(raw.x, raw.y)
                        knobOffset = if (dist > maxRadiusPx) {
                            Offset(raw.x / dist * maxRadiusPx, raw.y / dist * maxRadiusPx)
                        } else {
                            raw
                        }
                        val roll = (knobOffset.x / maxRadiusPx).coerceIn(-1f, 1f)
                        val pitch = (-knobOffset.y / maxRadiusPx).coerceIn(-1f, 1f)
                        onAxisChanged(roll, pitch)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawCircle(color = InstrumentBorder.copy(alpha = 0.4f), radius = size.minDimension * 0.42f, center = Offset(cx, cy))
            drawCircle(
                color = BangladeshEmerald,
                radius = 22.dp.toPx(),
                center = Offset(cx + knobOffset.x, cy + knobOffset.y)
            )
        }
    }
}
