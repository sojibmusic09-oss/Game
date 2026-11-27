package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.aircraft.AircraftCatalog
import com.example.core.AppLanguage
import com.example.core.BdasStrings
import com.example.data.*
import com.example.flightphysics.FlightState
import com.example.graphics3d.BdasViewport3D
import com.example.graphics3d.Scene3DMode
import com.example.replay.FlightReplayEntity
import com.example.ui.theme.*
import com.example.viewmodel.ReplayPlaybackState
import com.example.weather.WeatherPreset
import com.example.weather.WeatherState
import com.example.world.BangladeshGeoData
import com.example.world.BdAirport
import com.example.world.BdDistrict
import com.example.world.BdDivision
import kotlin.math.roundToInt

@Composable
fun WorldMapScreen(
    language: AppLanguage,
    profile: PilotProfile?,
    bannerMessage: String?,
    onDismissBanner: () -> Unit,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit,
    onUnlockAirport: (icao: String, cost: Long) -> Unit,
    onFlyRoute: (originIcao: String, destIcao: String) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    var selectedDivision by remember { mutableStateOf<BdDivision?>(null) }
    var selectedDistrict by remember { mutableStateOf<BdDistrict>(BangladeshGeoData.districts.first()) }
    var selectedAirport by remember { mutableStateOf<BdAirport>(BangladeshGeoData.airports.first()) }
    var originIcao by remember { mutableStateOf("VGHS") }
    var destIcao by remember { mutableStateOf("VGEG") }
    val unlockedAirports = profile?.unlockedAirportIds ?: listOf("VGHS", "VGEG")

    val filteredDistricts = remember(selectedDivision) {
        if (selectedDivision == null) BangladeshGeoData.districts
        else BangladeshGeoData.districts.filter { it.division == selectedDivision }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitVoid)
    ) {
        BdasTopBar(
            title = BdasStrings.worldMap(language),
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
            // Left: Interactive 8-Division & Flight Route Map of Bangladesh
            BangladeshDivisionRouteMap(
                language = language,
                selectedOriginIcao = originIcao,
                selectedDestinationIcao = destIcao,
                selectedDivision = selectedDivision,
                unlockedAirports = unlockedAirports,
                onDivisionSelected = { div ->
                    selectedDivision = div
                    if (div != null) {
                        BangladeshGeoData.districts.firstOrNull { it.division == div }?.let {
                            selectedDistrict = it
                        }
                    }
                },
                onOriginSelected = { icao -> originIcao = icao },
                onDestinationSelected = { icao -> destIcao = icao },
                onAirportFocused = { ap -> selectedAirport = ap },
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
            )

            // Right: Selected Route Launch, Unlock Airport, & 64 Districts Explorer
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val airportUnlocked = selectedAirport.icao in unlockedAirports || selectedAirport.unlockCostCoins == 0L
                Surface(
                    color = InstrumentSlate,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BangladeshEmerald)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("${selectedAirport.icao} • ${if (isBn) selectedAirport.nameBn else selectedAirport.nameEn}", style = MaterialTheme.typography.titleMedium, color = TextInstrumentWhite)
                                Text(
                                    text = "${if (isBn) selectedDistrict.nameBn else selectedDistrict.nameEn} (${if (isBn) selectedDistrict.division.bn else selectedDistrict.division.en})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HudCyan
                                )
                            }
                            if (!airportUnlocked) {
                                Button(
                                    onClick = { onUnlockAirport(selectedAirport.icao, selectedAirport.unlockCostCoins) },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("unlock_airport_${selectedAirport.icao}")
                                ) { Text("Unlock ${selectedAirport.unlockCostCoins}c", color = CockpitVoid) }
                            }
                        }
                        Button(
                            onClick = { onFlyRoute(originIcao, destIcao) },
                            colors = ButtonDefaults.buttonColors(containerColor = BangladeshEmerald),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("fly_route_button")
                        ) {
                            Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = OnEmerald)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "রুট উড্ডয়ন ($originIcao ➔ $destIcao)" else "Fly Route ($originIcao ➔ $destIcao)",
                                color = OnEmerald
                            )
                        }
                    }
                }

                // Airports & Districts Lists
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    item { SectionTitle(if (isBn) "৮টি বিমানবন্দর (ট্যাপ করে নির্বাচন করুন)" else "8 Bangladesh Airports") }
                    items(BangladeshGeoData.airports, key = { it.icao }) { ap ->
                        val unlocked = ap.icao in unlockedAirports || ap.unlockCostCoins == 0L
                        Surface(
                            color = if (selectedAirport.icao == ap.icao) BangladeshEmeraldDeep else InstrumentSlate,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, InstrumentBorder),
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedAirport = ap
                                if (unlocked && ap.icao != originIcao) {
                                    destIcao = ap.icao
                                }
                            }
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${ap.icao} - ${if (isBn) ap.nameBn else ap.nameEn}", color = TextInstrumentWhite, modifier = Modifier.weight(1f))
                                if (!unlocked) Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = WarningAmber, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    item { SectionTitle(if (isBn) "জেলাসমূহ (${filteredDistricts.size})" else "Districts (${filteredDistricts.size})") }
                    items(filteredDistricts, key = { it.id }) { dist ->
                        Surface(
                            color = if (selectedDistrict.id == dist.id) HudCyanContainer else InstrumentSlate,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedDistrict = dist
                                selectedDivision = dist.division
                            }
                        ) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isBn) dist.nameBn else dist.nameEn, color = TextInstrumentWhite)
                                Text(if (isBn) dist.division.bn else dist.division.en, color = TextMutedSlate)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MultiplayerScreen(
    language: AppLanguage,
    profile: PilotProfile?,
    activeSessions: List<MultiplayerSessionDoc>,
    joinedSessionId: String?,
    friends: List<FriendEntryDoc>,
    bannerMessage: String?,
    onDismissBanner: () -> Unit,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit,
    onCreateSession: (title: String, airportIcao: String, weather: WeatherPreset) -> Unit,
    onJoinSession: (MultiplayerSessionDoc) -> Unit,
    onStartSession: () -> Unit,
    onLeaveSession: () -> Unit,
    onLaunchOnlineFlight: () -> Unit,
    onAddFriend: (friendUid: String, callsign: String) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    var lobbyTitle by remember { mutableStateOf("Dhaka Group Flight") }
    var selectedAirport by remember { mutableStateOf("VGHS") }
    var friendUidInput by remember { mutableStateOf("") }
    var friendCallsignInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitVoid)
    ) {
        BdasTopBar(
            title = BdasStrings.multiplayer(language),
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
            // Left: Create Lobby & Live Lobbies
            LazyColumn(
                modifier = Modifier
                    .weight(1.2f)
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
                            SectionTitle(if (isBn) "নতুন ফ্লাইট লবি তৈরি করুন" else "Host Multiplayer Flight Lobby")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = lobbyTitle,
                                    onValueChange = { lobbyTitle = it },
                                    label = { Text(if (isBn) "লবির নাম" else "Lobby Title") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.5f).testTag("mp_lobby_title_input")
                                )
                                Button(
                                    onClick = { onCreateSession(lobbyTitle, selectedAirport, WeatherPreset.CLEAR) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BangladeshEmerald),
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("mp_create_lobby_button")
                                ) { Text(if (isBn) "তৈরি করুন" else "Create", color = OnEmerald) }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(BangladeshGeoData.airports.take(4)) { ap ->
                                    FilterChip(
                                        selected = selectedAirport == ap.icao,
                                        onClick = { selectedAirport = ap.icao },
                                        label = { Text(ap.icao) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (!joinedSessionId.isNullOrBlank()) {
                    item {
                        Surface(
                            color = BangladeshEmeraldDeep,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BangladeshEmerald)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Connected to Lobby", color = TextInstrumentWhite, modifier = Modifier.weight(1f))
                                OutlinedButton(onClick = onStartSession, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) { Text("Start") }
                                Button(
                                    onClick = onLaunchOnlineFlight,
                                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("mp_launch_flight_button")
                                ) { Text("Fly Online", color = CockpitVoid) }
                                TextButton(onClick = onLeaveSession) { Text("Leave", color = CautionCrimson) }
                            }
                        }
                    }
                }

                item { SectionTitle(if (isBn) "সক্রিয় অনলাইন লবি (${activeSessions.size})" else "Active Online Lobbies (${activeSessions.size})") }
                items(activeSessions, key = { it.id }) { sess ->
                    Surface(
                        color = InstrumentSlate,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, InstrumentBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(sess.title, style = MaterialTheme.typography.titleMedium, color = TextInstrumentWhite)
                                Text("Host: ${sess.hostCallsign} • ${sess.airportIcao} • ${sess.playerCount}/${sess.maxPlayers} Pilots", style = MaterialTheme.typography.bodyMedium, color = HudCyan)
                            }
                            Button(
                                onClick = { onJoinSession(sess) },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("mp_join_${sess.id}")
                            ) { Text(if (isBn) "যোগ দিন" else "Join") }
                        }
                    }
                }
            }

            // Right: Friends & Pilot Callsign Directory
            Surface(
                color = InstrumentSlate,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, InstrumentBorder),
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle(if (isBn) "পাইলট বন্ধু ও আইডি" else "Pilot Friends & Crew")
                    Text("Your Pilot ID: ${profile?.userId?.take(12) ?: ""}...", style = MaterialTheme.typography.labelLarge, color = HudCyan)
                    OutlinedTextField(
                        value = friendUidInput,
                        onValueChange = { friendUidInput = it },
                        label = { Text("Friend Pilot UID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("mp_friend_uid_input")
                    )
                    OutlinedTextField(
                        value = friendCallsignInput,
                        onValueChange = { friendCallsignInput = it },
                        label = { Text("Friend Callsign") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("mp_friend_callsign_input")
                    )
                    Button(
                        onClick = {
                            onAddFriend(friendUidInput, friendCallsignInput)
                            friendUidInput = ""
                            friendCallsignInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HudCyan),
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("mp_add_friend_button")
                    ) { Text(if (isBn) "বন্ধু যোগ করুন" else "Add Pilot Friend", color = CockpitVoid) }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(friends, key = { it.id }) { fr ->
                            Surface(color = CockpitDeepNavy, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(fr.friendCallsign, color = TextInstrumentWhite)
                                    Text(fr.friendRank, color = TextMutedSlate)
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
fun ReplayScreen(
    language: AppLanguage,
    profile: PilotProfile?,
    replays: List<FlightReplayEntity>,
    playback: ReplayPlaybackState,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit,
    onOpenReplay: (FlightReplayEntity) -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Int) -> Unit,
    onJumpEvent: (String) -> Unit,
    onCycleCamera: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val frame = playback.frames.getOrNull(playback.currentIndex)
    val replay = playback.replay
    val originAirport = remember(replay?.originIcao) { BangladeshGeoData.findAirport(replay?.originIcao ?: "VGHS") }
    val spec = remember(replay?.aircraftId) { AircraftCatalog.findById(replay?.aircraftId ?: "atr72") }

    val replayFlightState = if (frame != null) {
        FlightState(
            xMeters = frame.xMeters,
            zMeters = frame.zMeters,
            altitudeMeters = frame.altitudeMeters,
            groundElevationMeters = originAirport.elevationFt * 0.3048f,
            pitchDeg = frame.pitchDeg,
            rollDeg = frame.rollDeg,
            headingDeg = frame.headingDeg,
            airspeedMps = frame.speedKts / 1.94384f,
            throttleActual = frame.throttle,
            flapsLevel = frame.flapsLevel,
            gearDown = frame.gearDown,
            elapsedSec = frame.timestampSec
        )
    } else FlightState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitVoid)
    ) {
        BdasTopBar(
            title = BdasStrings.replay(language),
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
            // Left: Recorded Replays List
            LazyColumn(
                modifier = Modifier.weight(0.8f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (replays.isEmpty()) {
                    item {
                        Text(
                            text = if (isBn) "কোনো সংরক্ষিত ফ্লাইট রিপ্লে নেই।" else "No recorded flights yet. Complete any flight to save its 3D replay.",
                            color = TextMutedSlate
                        )
                    }
                }
                items(replays, key = { it.id }) { rep ->
                    val active = replay?.id == rep.id
                    Surface(
                        color = if (active) BangladeshEmeraldDeep else InstrumentSlate,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (active) BangladeshEmerald else InstrumentBorder),
                        modifier = Modifier.fillMaxWidth().clickable { onOpenReplay(rep) }.testTag("replay_item_${rep.id}")
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(rep.title, style = MaterialTheme.typography.titleMedium, color = TextInstrumentWhite)
                            Text("${rep.durationSec.roundToInt()}s • Landing ${rep.landingFpm} FPM", style = MaterialTheme.typography.bodyMedium, color = HudCyan)
                        }
                    }
                }
            }

            // Right: Live 3D Replay Viewport & Scrub Controls
            Surface(
                color = InstrumentSlate,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, InstrumentBorder),
                modifier = Modifier.weight(1.4f).fillMaxHeight()
            ) {
                Column(Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        BdasViewport3D(
                            modifier = Modifier.fillMaxSize(),
                            sceneMode = Scene3DMode.FLIGHT,
                            flightState = replayFlightState,
                            aircraftSpec = spec,
                            livery = AircraftCatalog.liveries.first(),
                            weatherState = WeatherState(),
                            cameraMode = playback.cameraMode,
                            graphicsPreset = com.example.graphics3d.GraphicsPreset.MEDIUM,
                            runwayHeadingDeg = originAirport.runwayHeadingDeg,
                            runwayLengthM = originAirport.runwayLengthM,
                            groundElevMeters = originAirport.elevationFt * 0.3048f
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onTogglePlay,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("replay_play_pause_button")
                        ) { Text(if (playback.playing) "Pause" else "Play") }

                        Slider(
                            value = playback.currentIndex.toFloat(),
                            onValueChange = { onSeek(it.roundToInt()) },
                            valueRange = 0f..(playback.frames.lastIndex.coerceAtLeast(1)).toFloat(),
                            modifier = Modifier.weight(1f).testTag("replay_seek_slider")
                        )

                        OutlinedButton(onClick = { onJumpEvent("TAKEOFF") }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                            Text("Takeoff")
                        }
                        OutlinedButton(onClick = { onJumpEvent("LANDING") }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                            Text("Landing")
                        }
                        OutlinedButton(onClick = onCycleCamera, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                            Text(playback.cameraMode.title(isBn))
                        }
                    }
                }
            }
        }
    }
}
