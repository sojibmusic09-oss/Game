package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.aircraft.AircraftCatalog
import com.example.core.AppLanguage
import com.example.data.PilotProfile
import com.example.data.UserSettings
import com.example.flightphysics.CameraMode
import com.example.flightphysics.FlightState
import com.example.graphics3d.BdasViewport3D
import com.example.graphics3d.Scene3DMode
import com.example.passenger.PassengerSessionState
import com.example.passenger.PassengerStage
import com.example.ui.theme.*
import com.example.weather.WeatherState
import com.example.world.BangladeshGeoData

@Composable
fun PassengerScreen(
    language: AppLanguage,
    settings: UserSettings,
    profile: PilotProfile?,
    passengerState: PassengerSessionState,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit,
    onMove: (forward: Float, strafe: Float) -> Unit,
    onLook: (dYaw: Float, dPitch: Float) -> Unit,
    onToggleCamera: () -> Unit,
    onSelectRoute: (String, String) -> Unit,
    onSelectSeat: (String, Int, Boolean, Boolean) -> Unit,
    onAdvanceStage: (String) -> Unit,
    onCrewService: (String) -> Unit,
    onResetJourney: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA

    Box(modifier = Modifier.fillMaxSize()) {
        // 3D Terminal or 3D Cabin Scene
        BdasViewport3D(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onLook(dragAmount.x * 0.25f, -dragAmount.y * 0.18f)
                    }
                },
            sceneMode = if (passengerState.isInCabin) Scene3DMode.PASSENGER_CABIN else Scene3DMode.PASSENGER_TERMINAL,
            flightState = FlightState(),
            aircraftSpec = AircraftCatalog.aircraft[1],
            livery = AircraftCatalog.liveries.first(),
            weatherState = WeatherState(),
            cameraMode = CameraMode.COCKPIT,
            graphicsPreset = settings.graphicsPreset,
            runwayHeadingDeg = 140f,
            runwayLengthM = 3200,
            groundElevMeters = 9.1f,
            passengerState = passengerState.renderState
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            BdasTopBar(
                title = "${if (isBn) "যাত্রী যাত্রা" else "Passenger Mode"} • ${passengerState.stage.stepNumber}/15",
                language = language,
                profile = profile,
                onBack = onBack,
                onToggleLanguage = onToggleLanguage,
                actions = {
                    OutlinedButton(
                        onClick = onToggleCamera,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("passenger_camera_toggle")
                    ) {
                        Text(if (passengerState.renderState.thirdPerson) "3RD PERSON" else "1ST PERSON", color = HudCyan)
                    }
                }
            )

            // Bottom Interactive Stage & Movement Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Virtual Walk Pad (First/Third-Person Movement)
                Surface(
                    color = CockpitVoid.copy(alpha = 0.80f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, InstrumentBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(if (isBn) "হাঁটার নিয়ন্ত্রণ (টেনে দেখুন)" else "WALK (DRAG SKY TO LOOK)", style = MaterialTheme.typography.labelSmall, color = TextMutedSlate)
                        IconButton(onClick = { onMove(1f, 0f) }, modifier = Modifier.minimumInteractiveComponentSize().testTag("pax_walk_fwd")) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Walk Forward", tint = BangladeshEmerald)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconButton(onClick = { onMove(0f, -1f) }, modifier = Modifier.minimumInteractiveComponentSize().testTag("pax_walk_left")) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Strafe Left", tint = TextInstrumentWhite)
                            }
                            IconButton(onClick = { onMove(0f, 1f) }, modifier = Modifier.minimumInteractiveComponentSize().testTag("pax_walk_right")) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Strafe Right", tint = TextInstrumentWhite)
                            }
                        }
                        IconButton(onClick = { onMove(-1f, 0f) }, modifier = Modifier.minimumInteractiveComponentSize().testTag("pax_walk_back")) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Walk Backward", tint = TextInstrumentWhite)
                        }
                    }
                }

                // Stage Interaction Card
                Surface(
                    color = InstrumentSlate.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, InstrumentBorder),
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .weight(1f)
                        .padding(start = 14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { passengerState.stage.stepNumber / 15f },
                            color = BangladeshEmerald,
                            trackColor = CockpitDeepNavy,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${passengerState.stage.stepNumber}. ${passengerState.stage.title.get(language)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = HudCyan
                        )
                        Text(
                            text = passengerState.stage.prompt.get(language),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextInstrumentWhite
                        )

                        // Context-specific Interactive Controls per Stage
                        when (passengerState.stage) {
                            PassengerStage.SELECT_FLIGHT -> {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(BangladeshGeoData.airports.filter { it.icao != "VGHS" }) { dest ->
                                        OutlinedButton(
                                            onClick = { onSelectRoute("VGHS", dest.icao) },
                                            modifier = Modifier
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("pax_dest_${dest.icao}")
                                        ) {
                                            Text("VGHS -> ${dest.icao} (${dest.cityEn})")
                                        }
                                    }
                                }
                            }
                            PassengerStage.BOARDING_PASS -> {
                                Text("Pass: ${passengerState.boardingPassCode} • Seat ${passengerState.selectedSeat}", style = MaterialTheme.typography.labelLarge, color = WarningAmber)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        Triple("6A (Window L)", 6, false),
                                        Triple("6C (Aisle L)", 6, false),
                                        Triple("6F (Window R)", 6, true)
                                    ).forEach { (label, row, right) ->
                                        val code = label.substringBefore(" ")
                                        FilterChip(
                                            selected = passengerState.selectedSeat == code,
                                            onClick = { onSelectSeat(code, row, right, label.contains("Window")) },
                                            label = { Text(label) },
                                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                        )
                                    }
                                }
                            }
                            PassengerStage.TERMINAL_SHOPS -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onAdvanceStage("Sylheti Masala Tea") },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                    ) { Text(if (isBn) "সিলেটি চা কিনুন" else "Buy Sylheti Tea") }
                                    OutlinedButton(
                                        onClick = { onAdvanceStage("Dhaka Jamdani Gift") },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                    ) { Text(if (isBn) "স্যুভেনির কিনুন" else "Buy Souvenir") }
                                }
                            }
                            PassengerStage.CABIN_CRUISE -> {
                                if (passengerState.crewGreeting.isNotBlank()) {
                                    Text(passengerState.crewGreeting, style = MaterialTheme.typography.bodyMedium, color = BangladeshEmerald)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onCrewService(if (isBn) "চা ও বিস্কুট" else "Hot Tea & Snacks") },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("crew_tea_button")
                                    ) { Text(if (isBn) "কেবিন ক্রু: চা" else "Ask Crew: Tea") }
                                    OutlinedButton(
                                        onClick = { onCrewService(if (isBn) "পানির বোতল" else "Chilled Water") },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                    ) { Text(if (isBn) "পানি" else "Ask Crew: Water") }
                                }
                            }
                            else -> {}
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (passengerState.journeyCompleted) {
                                Button(
                                    onClick = onResetJourney,
                                    colors = ButtonDefaults.buttonColors(containerColor = BangladeshEmerald),
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("pax_restart_button")
                                ) {
                                    Text(if (isBn) "নতুন যাত্রা শুরু করুন" else "Start Another Journey", color = OnEmerald)
                                }
                            } else {
                                val nextLabel = when (passengerState.stage) {
                                    PassengerStage.PURCHASE_TICKET -> if (isBn) "টিকিট কিনুন (${passengerState.ticketFareCoins}c)" else "Buy Ticket (${passengerState.ticketFareCoins}c)"
                                    PassengerStage.BAGGAGE_CLAIM -> if (isBn) "ব্যাগেজ সংগ্রহ ও সমাপ্তি" else "Claim Baggage & Finish"
                                    else -> if (isBn) "পরবর্তী ধাপ" else "Continue Step"
                                }
                                Button(
                                    onClick = { onAdvanceStage("") },
                                    colors = ButtonDefaults.buttonColors(containerColor = BangladeshEmerald),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("pax_advance_button")
                                ) {
                                    Text(nextLabel, color = OnEmerald)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
