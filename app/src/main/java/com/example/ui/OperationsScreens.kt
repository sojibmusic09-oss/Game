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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.aircraft.AircraftCatalog
import com.example.aircraft.AircraftSpec
import com.example.airline.AirlineCycleReport
import com.example.airline.AirlineEconomyEngine
import com.example.core.AppLanguage
import com.example.core.BdasStrings
import com.example.data.*
import com.example.missions.MissionCatalog
import com.example.missions.MissionDefinition
import com.example.missions.PilotRank
import com.example.ui.theme.*
import com.example.world.BangladeshGeoData
import kotlin.math.roundToLong

@Composable
fun CareerScreen(
    language: AppLanguage,
    profile: PilotProfile?,
    flightLogs: List<FlightLogDoc>,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit,
    onStartMission: (MissionDefinition) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val xp = profile?.xp ?: 0L
    val currentRank = PilotRank.forXp(xp)
    val nextRank = PilotRank.entries.getOrNull(currentRank.ordinal + 1)
    val progress = if (nextRank == null) 1f else {
        ((xp - currentRank.minXp).toFloat() / (nextRank.minXp - currentRank.minXp).coerceAtLeast(1L)).coerceIn(0f, 1f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitVoid)
    ) {
        BdasTopBar(
            title = BdasStrings.pilotCareer(language),
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
            // Left: Rank Progression & Missions
            LazyColumn(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Surface(
                        color = InstrumentSlate,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, InstrumentBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = currentRank.title(language),
                                style = MaterialTheme.typography.headlineMedium,
                                color = BangladeshEmerald
                            )
                            Text(
                                text = if (isBn) {
                                    "মোট ফ্লাইট: ${profile?.totalFlights ?: 0} • সময়: ${"%.2f".format(profile?.flightHours ?: 0.0)} ঘণ্টা • $xp XP"
                                } else {
                                    "Flights: ${profile?.totalFlights ?: 0} • Hours: ${"%.2f".format(profile?.flightHours ?: 0.0)}h • $xp XP"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextInstrumentWhite
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                color = HudCyan,
                                trackColor = CockpitDeepNavy,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (nextRank != null) {
                                Text(
                                    text = if (isBn) "পরবর্তী ধাপ: ${nextRank.title(language)} (${nextRank.minXp} XP)" else "Next Rank: ${nextRank.title(language)} (${nextRank.minXp} XP)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextMutedSlate
                                )
                            }
                        }
                    }
                }

                item {
                    SectionTitle(if (isBn) "প্রশিক্ষণ ও ক্যারিয়ার পরীক্ষা" else "Training & License Exams")
                }

                items(MissionCatalog.trainingAndCareerMissions) { mission ->
                    Surface(
                        color = InstrumentSlate,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, InstrumentBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = WarningAmber)
                            Column(Modifier.weight(1f)) {
                                Text(mission.title.get(language), style = MaterialTheme.typography.titleMedium, color = TextInstrumentWhite)
                                Text(mission.briefing.get(language), style = MaterialTheme.typography.bodyMedium, color = TextMutedSlate)
                                Text("+${mission.rewardXp} XP • +${mission.rewardCoins}c", style = MaterialTheme.typography.labelLarge, color = BangladeshEmerald)
                            }
                            Button(
                                onClick = { onStartMission(mission) },
                                colors = ButtonDefaults.buttonColors(containerColor = BangladeshEmerald),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("career_launch_${mission.id}")
                            ) {
                                Text(if (isBn) "উড্ডয়ন" else "Fly", color = OnEmerald)
                            }
                        }
                    }
                }
            }

            // Right: Flight Log History
            Surface(
                color = InstrumentSlate,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, InstrumentBorder),
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
            ) {
                Column(Modifier.padding(14.dp)) {
                    SectionTitle(if (isBn) "ফ্লাইট লগ ইতিহাস" else "Recent Cloud Flight Logs")
                    Spacer(Modifier.height(6.dp))
                    if (flightLogs.isEmpty()) {
                        Text(
                            text = if (isBn) "এখনো কোনো ফ্লাইট রেকর্ড করা হয়নি।" else "No flights logged yet. Complete a flight or exam to build your logbook.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMutedSlate
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(flightLogs, key = { it.id }) { log ->
                                Surface(
                                    color = CockpitDeepNavy,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, InstrumentBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("${log.originIcao} -> ${log.destinationIcao} (${log.aircraftId.uppercase()})", style = MaterialTheme.typography.titleMedium, color = TextInstrumentWhite)
                                            Text("${log.mode} • ${log.weatherPreset} • ${log.landingFpm} FPM", style = MaterialTheme.typography.bodyMedium, color = TextMutedSlate)
                                        }
                                        Text("+${log.coinsEarned}c", style = MaterialTheme.typography.labelLarge, color = WarningAmber)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AirlineScreen(
    language: AppLanguage,
    profile: PilotProfile?,
    airlines: List<AirlineDoc>,
    fleet: List<FleetAircraftDoc>,
    routes: List<AirlineRouteDoc>,
    transactions: List<EconomyTransactionDoc>,
    lastCycleReport: AirlineCycleReport?,
    bannerMessage: String?,
    onDismissBanner: () -> Unit,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit,
    onSaveAirline: (name: String, iata: String, logoId: String, primaryHex: String, secondaryHex: String, pilotsDelta: Long, crewDelta: Long) -> Unit,
    onBuyFleet: (AircraftSpec, String) -> Unit,
    onOpenRoute: (origin: String, dest: String, priceCoins: Long) -> Unit,
    onRunCycle: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val existing = airlines.firstOrNull()

    var airlineName by remember(existing?.id) { mutableStateOf(existing?.name ?: "Bengal Horizon Air") }
    var iataCode by remember(existing?.id) { mutableStateOf(existing?.iataCode ?: "BHA") }
    var selectedOrigin by remember { mutableStateOf("VGHS") }
    var selectedDest by remember { mutableStateOf("VGEG") }
    var ticketPrice by remember { mutableFloatStateOf(4200f) }

    val estimatedDemand = remember(selectedOrigin, selectedDest, ticketPrice, existing?.reputation) {
        AirlineEconomyEngine.computeDemandScore(
            selectedOrigin,
            selectedDest,
            ticketPrice.roundToLong(),
            existing?.reputation ?: 68L
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitVoid)
    ) {
        BdasTopBar(
            title = BdasStrings.airlineMgmt(language),
            language = language,
            profile = profile,
            onBack = onBack,
            onToggleLanguage = onToggleLanguage
        )
        BannerAlert(message = bannerMessage, onDismiss = onDismissBanner)

        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: Airline HQ, Staff & Route Planner
            LazyColumn(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Surface(
                        color = InstrumentSlate,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, InstrumentBorder)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle(if (isBn) "এয়ারলাইন পরিচিতি ও কর্মী" else "Airline Identity & Crew")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = airlineName,
                                    onValueChange = { airlineName = it },
                                    label = { Text(if (isBn) "এয়ারলাইন নাম" else "Airline Name") },
                                    singleLine = true,
                                    modifier = Modifier.weight(2f).testTag("airline_name_input")
                                )
                                OutlinedTextField(
                                    value = iataCode,
                                    onValueChange = { iataCode = it.uppercase().take(4) },
                                    label = { Text("IATA") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("airline_iata_input")
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onSaveAirline(airlineName, iataCode, "emerald_sun", "#00A86B", "#EF5350", 0L, 0L) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BangladeshEmerald),
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("save_airline_button")
                                ) {
                                    Text(if (existing == null) (if (isBn) "এয়ারলাইন খুলুন" else "Found Airline") else (if (isBn) "সংরক্ষণ" else "Save HQ"), color = OnEmerald)
                                }
                                if (existing != null) {
                                    OutlinedButton(
                                        onClick = { onSaveAirline(airlineName, iataCode, existing.logoId, existing.primaryColorHex, existing.secondaryColorHex, 1L, 0L) },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("hire_pilot_button")
                                    ) { Text("+1 Pilot (${existing.pilotsHired})") }
                                    OutlinedButton(
                                        onClick = { onSaveAirline(airlineName, iataCode, existing.logoId, existing.primaryColorHex, existing.secondaryColorHex, 0L, 2L) },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("hire_crew_button")
                                    ) { Text("+2 Crew (${existing.cabinCrewHired})") }
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        color = InstrumentSlate,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, InstrumentBorder)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle(if (isBn) "নতুন রুট ও টিকিট মূল্য" else "Open Route & Ticket Pricing")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(BangladeshGeoData.airports) { ap ->
                                    FilterChip(
                                        selected = selectedOrigin == ap.icao,
                                        onClick = { selectedOrigin = ap.icao },
                                        label = { Text("From ${ap.icao}") }
                                    )
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(BangladeshGeoData.airports.filter { it.icao != selectedOrigin }) { ap ->
                                    FilterChip(
                                        selected = selectedDest == ap.icao,
                                        onClick = { selectedDest = ap.icao },
                                        label = { Text("To ${ap.icao}") }
                                    )
                                }
                            }
                            Text(
                                text = "Ticket Price: ${ticketPrice.roundToLong()}c • Forecast Demand: $estimatedDemand%",
                                style = MaterialTheme.typography.labelLarge,
                                color = HudCyan
                            )
                            Slider(
                                value = ticketPrice,
                                onValueChange = { ticketPrice = it },
                                valueRange = 1200f..12000f,
                                modifier = Modifier.testTag("ticket_price_slider")
                            )
                            Button(
                                onClick = { onOpenRoute(selectedOrigin, selectedDest, ticketPrice.roundToLong()) },
                                colors = ButtonDefaults.buttonColors(containerColor = HudCyan),
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("open_route_button")
                            ) {
                                Text(if (isBn) "রুট চালু করুন ($selectedOrigin -> $selectedDest)" else "Launch Route ($selectedOrigin -> $selectedDest)", color = CockpitVoid)
                            }
                        }
                    }
                }
            }

            // Right: Fleet Purchase, Active Routes, Operating Cycle & Ledger
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Surface(
                        color = InstrumentSlate,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, InstrumentBorder)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                SectionTitle(if (isBn) "দৈনিক অপারেশন ও ফ্লিট (${fleet.size})" else "Daily Operations & Fleet (${fleet.size})")
                                Button(
                                    onClick = onRunCycle,
                                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("run_cycle_button")
                                ) {
                                    Text(if (isBn) "দিন চালান" else "Run Ops Cycle", color = CockpitVoid)
                                }
                            }
                            if (lastCycleReport != null) {
                                Text(
                                    text = "Pax: ${lastCycleReport.passengersCarried} | Rev: +${lastCycleReport.grossRevenueCoins}c | Net: ${lastCycleReport.netProfitCoins}c",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (lastCycleReport.netProfitCoins >= 0) BangladeshEmerald else CautionCrimson
                                )
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(AircraftCatalog.aircraft) { spec ->
                                    val price = spec.purchasePriceCoins.coerceAtLeast(8000L)
                                    OutlinedButton(
                                        onClick = { onBuyFleet(spec, "Emerald Sun") },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("buy_fleet_${spec.id}")
                                    ) {
                                        Text("+ ${spec.id.uppercase()} (${price}c)")
                                    }
                                }
                            }
                            Text(
                                text = "Active Routes: ${routes.size} (${routes.joinToString { "${it.originIcao}-${it.destinationIcao}" }.ifBlank { "None" }})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMutedSlate
                            )
                        }
                    }
                }

                item { SectionTitle(if (isBn) "আর্থিক লেনদেন" else "Recent Economy Ledger") }

                items(transactions.take(8), key = { it.id }) { tx ->
                    Surface(
                        color = CockpitDeepNavy,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, InstrumentBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tx.summary, style = MaterialTheme.typography.bodyMedium, color = TextInstrumentWhite, modifier = Modifier.weight(1f))
                            val prefix = if (tx.txType == "INCOME") "+" else "-"
                            val clr = if (tx.txType == "INCOME") BangladeshEmerald else CautionCrimson
                            Text("$prefix${tx.amountCoins}c", style = MaterialTheme.typography.labelLarge, color = clr)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HangarScreen(
    language: AppLanguage,
    profile: PilotProfile?,
    selectedAircraftId: String,
    selectedLiveryId: String,
    bannerMessage: String?,
    onDismissBanner: () -> Unit,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit,
    onUnlockAircraft: (AircraftSpec) -> Unit,
    onSelectForFlight: (aircraftId: String, liveryId: String) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val unlocked = profile?.unlockedAircraftIds ?: listOf("atr72")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitVoid)
    ) {
        BdasTopBar(
            title = BdasStrings.hangar(language),
            language = language,
            profile = profile,
            onBack = onBack,
            onToggleLanguage = onToggleLanguage
        )
        BannerAlert(message = bannerMessage, onDismiss = onDismissBanner)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Livery Selector Row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (isBn) "বিমানের রঙ:" else "Livery:", style = MaterialTheme.typography.titleMedium, color = HudCyan)
                AircraftCatalog.liveries.forEach { liv ->
                    FilterChip(
                        selected = selectedLiveryId == liv.id,
                        onClick = { onSelectForFlight(selectedAircraftId, liv.id) },
                        label = { Text(if (isBn) liv.nameBn else liv.nameEn) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("livery_chip_${liv.id}")
                    )
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(AircraftCatalog.aircraft, key = { it.id }) { spec ->
                    val isUnlocked = spec.id in unlocked
                    Surface(
                        color = InstrumentSlate,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (selectedAircraftId == spec.id) BangladeshEmerald else InstrumentBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(Icons.Filled.Flight, contentDescription = null, tint = HudCyan, modifier = Modifier.size(36.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(spec.displayName(language), style = MaterialTheme.typography.titleLarge, color = TextInstrumentWhite)
                                Text(spec.description.get(language), style = MaterialTheme.typography.bodyMedium, color = TextMutedSlate)
                                Text(
                                    text = "Cruise: ${spec.cruiseSpeedKts.toInt()} KTS • Stall: ${spec.stallSpeedKts.toInt()} KTS • Pax: ${spec.passengerCapacity} • Min RWY: ${spec.minRunwayMeters}m",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = HudCyan
                                )
                            }
                            if (isUnlocked) {
                                Button(
                                    onClick = { onSelectForFlight(spec.id, selectedLiveryId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BangladeshEmerald),
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("fly_hangar_${spec.id}")
                                ) {
                                    Text(if (isBn) "ফ্লাইট সেটআপ" else "Select & Fly", color = OnEmerald)
                                }
                            } else {
                                Button(
                                    onClick = { onUnlockAircraft(spec) },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("unlock_hangar_${spec.id}")
                                ) {
                                    Text("${spec.purchasePriceCoins}c", color = CockpitVoid)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
