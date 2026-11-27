package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.aircraft.AircraftCatalog
import com.example.core.AppLanguage
import com.example.core.BdasStrings
import com.example.data.PilotProfile
import com.example.missions.FlightModeType
import com.example.missions.MissionCatalog
import com.example.missions.MissionDefinition
import com.example.ui.theme.*
import com.example.viewmodel.FlightBriefingConfig
import com.example.weather.SeasonType
import com.example.weather.TimeOfDay
import com.example.weather.WeatherPreset
import com.example.weather.WindDirectionMode
import com.example.world.BangladeshGeoData

@Composable
fun FlightSetupScreen(
    language: AppLanguage,
    profile: PilotProfile?,
    briefing: FlightBriefingConfig,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit,
    onUpdateBriefing: ((FlightBriefingConfig) -> FlightBriefingConfig) -> Unit,
    onSelectMission: (MissionDefinition) -> Unit,
    onLaunchFlight: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val unlockedAircraft = profile?.unlockedAircraftIds ?: listOf("atr72")
    val unlockedAirports = profile?.unlockedAirportIds ?: listOf("VGHS", "VGEG")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitVoid)
    ) {
        BdasTopBar(
            title = if (isBn) "ফ্লাইট ব্রিফিং ও প্রস্তুতি" else "Flight Briefing & Weather Setup",
            language = language,
            profile = profile,
            onBack = onBack,
            onToggleLanguage = onToggleLanguage
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: Mode, Aircraft, Airports
            LazyColumn(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    SectionTitle(if (isBn) "১. ফ্লাইট মোড" else "1. Flight Mode")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val modes = listOf(
                            FlightModeType.FREE_FLIGHT to (if (isBn) "ফ্রি ফ্লাইট" else "Free Flight"),
                            FlightModeType.TRAINING to (if (isBn) "প্রশিক্ষণ" else "Training"),
                            FlightModeType.CAREER to (if (isBn) "ক্যারিয়ার" else "Career Exam"),
                            FlightModeType.EMERGENCY to (if (isBn) "জরুরি মিশন" else "Emergency (8)")
                        )
                        items(modes) { (mode, label) ->
                            FilterChip(
                                selected = briefing.mode == mode,
                                onClick = {
                                    onUpdateBriefing { it.copy(mode = mode, missionId = "") }
                                },
                                label = { Text(label) },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("mode_chip_${mode.name.lowercase()}")
                            )
                        }
                    }
                }

                if (briefing.mode == FlightModeType.EMERGENCY || briefing.mode == FlightModeType.TRAINING || briefing.mode == FlightModeType.CAREER) {
                    val list = if (briefing.mode == FlightModeType.EMERGENCY) {
                        MissionCatalog.emergencyMissions
                    } else {
                        MissionCatalog.trainingAndCareerMissions.filter { it.mode == briefing.mode }
                    }
                    item {
                        SectionTitle(if (isBn) "মিশন নির্বাচন করুন" else "Select Scenario / Mission")
                    }
                    items(list) { mission ->
                        val selected = briefing.missionId == mission.id
                        Surface(
                            color = if (selected) CautionCrimsonContainer else InstrumentSlate,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (selected) CautionCrimson else InstrumentBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectMission(mission) }
                                .testTag("mission_card_${mission.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = if (selected) CautionCrimson else WarningAmber)
                                Column(Modifier.weight(1f)) {
                                    Text(mission.title.get(language), style = MaterialTheme.typography.titleMedium, color = TextInstrumentWhite)
                                    Text(mission.briefing.get(language), style = MaterialTheme.typography.bodyMedium, color = TextMutedSlate)
                                }
                                Text("+${mission.rewardCoins}c", style = MaterialTheme.typography.labelLarge, color = WarningAmber)
                            }
                        }
                    }
                }

                item {
                    SectionTitle(if (isBn) "২. বিমান নির্বাচন" else "2. Select Aircraft")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AircraftCatalog.aircraft) { spec ->
                            val unlocked = spec.id in unlockedAircraft
                            val selected = briefing.aircraftId == spec.id
                            Surface(
                                color = if (selected) BangladeshEmeraldDeep else InstrumentSlate,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (selected) BangladeshEmerald else InstrumentBorder),
                                modifier = Modifier
                                    .width(195.dp)
                                    .clickable(enabled = unlocked) {
                                        onUpdateBriefing { it.copy(aircraftId = spec.id) }
                                    }
                                    .testTag("select_aircraft_${spec.id}")
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(spec.displayName(language), style = MaterialTheme.typography.titleMedium, color = TextInstrumentWhite, modifier = Modifier.weight(1f))
                                        if (!unlocked) Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = WarningAmber, modifier = Modifier.size(16.dp))
                                    }
                                    Text("${spec.cruiseSpeedKts.toInt()} KTS • ${spec.passengerCapacity} PAX", style = MaterialTheme.typography.labelSmall, color = HudCyan)
                                }
                            }
                        }
                    }
                }

                item {
                    var showDivisionMap by remember { mutableStateOf(false) }
                    var setupDivision by remember { mutableStateOf<com.example.world.BdDivision?>(null) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SectionTitle(if (isBn) "৩. প্রস্থান ও গন্তব্য (${briefing.originIcao} ➔ ${briefing.destinationIcao})" else "3. Route (${briefing.originIcao} ➔ ${briefing.destinationIcao})")
                        OutlinedButton(
                            onClick = { showDivisionMap = true },
                            modifier = Modifier.defaultMinSize(minHeight = 40.dp).testTag("open_division_route_map_button")
                        ) {
                            Icon(Icons.Filled.Map, contentDescription = null, tint = HudCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isBn) "৮-বিভাগ মানচিত্র" else "8-Division Map", color = HudCyan)
                        }
                    }

                    if (showDivisionMap) {
                        androidx.compose.ui.window.Dialog(
                            onDismissRequest = { showDivisionMap = false },
                            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            Surface(
                                color = CockpitVoid,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BangladeshEmerald),
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .fillMaxHeight(0.88f)
                            ) {
                                Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isBn) "বাংলাদেশের ৮ বিভাগে ফ্লাইট রুট নির্বাচন করুন" else "Select Flight Route Across 8 Divisions of Bangladesh",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextInstrumentWhite
                                        )
                                        Button(
                                            onClick = { showDivisionMap = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = BangladeshEmerald),
                                            modifier = Modifier.defaultMinSize(minHeight = 44.dp).testTag("confirm_division_route_button")
                                        ) {
                                            Text(if (isBn) "নিশ্চিত করুন" else "Confirm Route", color = OnEmerald)
                                        }
                                    }
                                    BangladeshDivisionRouteMap(
                                        language = language,
                                        selectedOriginIcao = briefing.originIcao,
                                        selectedDestinationIcao = briefing.destinationIcao,
                                        selectedDivision = setupDivision,
                                        unlockedAirports = unlockedAirports,
                                        onDivisionSelected = { setupDivision = it },
                                        onOriginSelected = { icao -> onUpdateBriefing { it.copy(originIcao = icao) } },
                                        onDestinationSelected = { icao -> onUpdateBriefing { it.copy(destinationIcao = icao) } },
                                        modifier = Modifier.weight(1f).fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BangladeshGeoData.airports) { ap ->
                            val unlocked = ap.icao in unlockedAirports || ap.unlockCostCoins == 0L
                            val isOrig = briefing.originIcao == ap.icao
                            val isDest = briefing.destinationIcao == ap.icao
                            Surface(
                                color = when {
                                    isOrig -> BangladeshEmeraldDeep
                                    isDest -> HudCyanContainer
                                    else -> InstrumentSlate
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isOrig) BangladeshEmerald else if (isDest) HudCyan else InstrumentBorder),
                                modifier = Modifier
                                    .clickable(enabled = unlocked) {
                                        onUpdateBriefing { cur ->
                                            when {
                                                cur.originIcao == ap.icao -> cur
                                                cur.destinationIcao == ap.icao -> cur.copy(originIcao = ap.icao, destinationIcao = cur.originIcao)
                                                else -> cur.copy(destinationIcao = ap.icao)
                                            }
                                        }
                                    }
                                    .testTag("airport_chip_${ap.icao}")
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(ap.icao, style = MaterialTheme.typography.labelLarge, color = TextInstrumentWhite)
                                        if (!unlocked) Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = WarningAmber, modifier = Modifier.size(14.dp))
                                    }
                                    Text(if (isBn) ap.nameBn else ap.cityEn, style = MaterialTheme.typography.bodyMedium, color = TextMutedSlate)
                                }
                            }
                        }
                    }
                }
            }

            // Right: Weather, Time of Day, Wind, Livery, Launch
            Surface(
                color = InstrumentSlate,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, InstrumentBorder),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle(if (isBn) "৪. আবহাওয়া ও সময়" else "4. Dynamic Weather & Environment")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(WeatherPreset.entries) { wp ->
                                FilterChip(
                                    selected = briefing.weatherPreset == wp,
                                    onClick = { onUpdateBriefing { it.copy(weatherPreset = wp) } },
                                    label = { Text(wp.title(language)) },
                                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                                )
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(TimeOfDay.entries) { tod ->
                                FilterChip(
                                    selected = briefing.timeOfDay == tod,
                                    onClick = { onUpdateBriefing { it.copy(timeOfDay = tod) } },
                                    label = { Text(tod.title(language)) }
                                )
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(WindDirectionMode.entries) { wm ->
                                FilterChip(
                                    selected = briefing.windMode == wm,
                                    onClick = { onUpdateBriefing { it.copy(windMode = wm) } },
                                    label = { Text(wm.title(language)) }
                                )
                            }
                            items(SeasonType.entries) { st ->
                                FilterChip(
                                    selected = briefing.season == st,
                                    onClick = { onUpdateBriefing { it.copy(season = st) } },
                                    label = { Text(st.title(language)) }
                                )
                            }
                        }
                        Surface(
                            color = CockpitDeepNavy,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, InstrumentBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                val ap = BangladeshGeoData.findAirport(briefing.originIcao)
                                Text(
                                    text = "${ap.icao} • RWY ${(ap.runwayHeadingDeg / 10).toInt()} • ${ap.runwayLengthM}m",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = HudCyan
                                )
                                Text(
                                    text = "Wind ${briefing.weatherPreset.baseWindKts.toInt()} KTS (${briefing.windMode.title(language)}) • Turb ${(briefing.weatherPreset.baseTurbulence * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMutedSlate
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onLaunchFlight,
                        colors = ButtonDefaults.buttonColors(containerColor = BangladeshEmerald),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 54.dp)
                            .testTag("launch_flight_button")
                    ) {
                        Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = OnEmerald)
                        Spacer(Modifier.width(8.dp))
                        Text(BdasStrings.launchFlight(language), style = MaterialTheme.typography.titleMedium, color = OnEmerald)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = HudCyan,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
