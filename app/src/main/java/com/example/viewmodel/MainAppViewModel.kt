package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.aircraft.AircraftCatalog
import com.example.aircraft.AircraftSpec
import com.example.airline.AirlineCycleReport
import com.example.airline.AirlineEconomyEngine
import com.example.audio.AviationAudioEngine
import com.example.core.AppLanguage
import com.example.core.BdasStrings
import com.example.data.*
import com.example.flightphysics.*
import com.example.graphics3d.GraphicsPreset
import com.example.graphics3d.PeerAircraftRender
import com.example.graphics3d.Scene3DMode
import com.example.missions.*
import com.example.passenger.PassengerController
import com.example.passenger.PassengerSessionState
import com.example.passenger.PassengerStage
import com.example.replay.BdasLocalDatabase
import com.example.replay.FlightReplayEntity
import com.example.replay.ReplayFrameEntity
import com.example.replay.ReplayRepository
import com.example.weather.*
import com.example.world.BangladeshGeoData
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class FlightBriefingConfig(
    val mode: FlightModeType = FlightModeType.FREE_FLIGHT,
    val aircraftId: String = "atr72",
    val liveryId: String = "emerald_sun",
    val originIcao: String = "VGHS",
    val destinationIcao: String = "VGEG",
    val weatherPreset: WeatherPreset = WeatherPreset.CLEAR,
    val timeOfDay: TimeOfDay = TimeOfDay.DAY,
    val season: SeasonType = SeasonType.AUTUMN_CLEAR,
    val windMode: WindDirectionMode = WindDirectionMode.CROSSWIND,
    val missionId: String = ""
)

data class FlightDebriefReport(
    val success: Boolean,
    val title: String,
    val summary: String,
    val landingFpm: Int,
    val score: Int,
    val xpEarned: Long,
    val coinsEarned: Long,
    val newAchievements: List<String>,
    val savedReplayId: Long?
)

data class ReplayPlaybackState(
    val replay: FlightReplayEntity? = null,
    val frames: List<ReplayFrameEntity> = emptyList(),
    val currentIndex: Int = 0,
    val playing: Boolean = false,
    val cameraMode: CameraMode = CameraMode.CHASE
)

class MainAppViewModel(
    private val currentUserId: String,
    private val cloudRepo: CloudSyncRepository,
    private val multiplayerRepo: MultiplayerRepository,
    private val replayRepo: ReplayRepository,
    private val settingsRepo: SettingsRepository,
    private val audioEngine: AviationAudioEngine
) : ViewModel() {

    private companion object {
        const val TAG = "BdasMainVM"
        const val STOP_TIMEOUT_MS = 5000L
        const val METERS_PER_DEG_LAT = 111320.0
    }

    val settings: StateFlow<UserSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UserSettings())

    // Two-Tier Flow Error Unwrapping per operational invariants
    val profileState: StateFlow<UiState<PilotProfile>> = cloudRepo.observeProfile(currentUserId)
        .map<PilotProfile?, UiState<PilotProfile>> { profile ->
            UiState.Success(profile ?: PilotProfile(userId = currentUserId))
        }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Profile observe error", fsErr ?: error)
            emit(UiState.Error((fsErr ?: error).message ?: "Failed to sync pilot profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val airlines: StateFlow<List<AirlineDoc>> = cloudRepo.observeAirlines(currentUserId)
        .catch { error -> handleNonFatalFlowError(error, emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val fleet: StateFlow<List<FleetAircraftDoc>> = cloudRepo.observeFleet(currentUserId)
        .catch { error -> handleNonFatalFlowError(error, emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val routes: StateFlow<List<AirlineRouteDoc>> = cloudRepo.observeRoutes(currentUserId)
        .catch { error -> handleNonFatalFlowError(error, emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val transactions: StateFlow<List<EconomyTransactionDoc>> = cloudRepo.observeTransactions(currentUserId)
        .catch { error -> handleNonFatalFlowError(error, emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val flightLogs: StateFlow<List<FlightLogDoc>> = cloudRepo.observeFlightLogs(currentUserId)
        .catch { error -> handleNonFatalFlowError(error, emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val replays: StateFlow<List<FlightReplayEntity>> = replayRepo.replays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val activeSessions: StateFlow<List<MultiplayerSessionDoc>> = multiplayerRepo.observeActiveSessions()
        .catch { error -> handleNonFatalFlowError(error, emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val friends: StateFlow<List<FriendEntryDoc>> = multiplayerRepo.observeFriends(currentUserId)
        .catch { error -> handleNonFatalFlowError(error, emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _joinedSessionId = MutableStateFlow<String?>(null)
    val joinedSessionId: StateFlow<String?> = _joinedSessionId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val peerAircraftStates: StateFlow<List<MultiplayerAircraftStateDoc>> = _joinedSessionId
        .flatMapLatest { sid ->
            if (sid.isNullOrBlank()) flowOf(emptyList())
            else multiplayerRepo.observeSessionAircraftStates(sid, currentUserId)
                .catch { error -> handleNonFatalFlowError(error, emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _briefing = MutableStateFlow(FlightBriefingConfig())
    val briefing: StateFlow<FlightBriefingConfig> = _briefing.asStateFlow()

    private val _flightState = MutableStateFlow(FlightState())
    val flightState: StateFlow<FlightState> = _flightState.asStateFlow()

    private val _controlInput = MutableStateFlow(FlightControlInput())
    val controlInput: StateFlow<FlightControlInput> = _controlInput.asStateFlow()

    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    private val _cameraMode = MutableStateFlow(CameraMode.CHASE)
    val cameraMode: StateFlow<CameraMode> = _cameraMode.asStateFlow()

    private val _flightPaused = MutableStateFlow(false)
    val flightPaused: StateFlow<Boolean> = _flightPaused.asStateFlow()

    private val _debriefReport = MutableStateFlow<FlightDebriefReport?>(null)
    val debriefReport: StateFlow<FlightDebriefReport?> = _debriefReport.asStateFlow()

    private val _passengerState = MutableStateFlow(PassengerSessionState())
    val passengerState: StateFlow<PassengerSessionState> = _passengerState.asStateFlow()

    private val _lastCycleReport = MutableStateFlow<AirlineCycleReport?>(null)
    val lastCycleReport: StateFlow<AirlineCycleReport?> = _lastCycleReport.asStateFlow()

    private val _replayPlayback = MutableStateFlow(ReplayPlaybackState())
    val replayPlayback: StateFlow<ReplayPlaybackState> = _replayPlayback.asStateFlow()

    private val physicsEngine = FlightPhysicsEngine()
    private val weatherController = DynamicWeatherController()
    private val passengerController = PassengerController()

    private var flightLoopJob: Job? = null
    private var replayPlaybackJob: Job? = null
    private var passengerCruiseJob: Job? = null
    private val recordedFrames = ArrayList<ReplayFrameEntity>(512)
    private var lastFrameRecordSec = -1f
    private var lastMpSyncSec = -1f
    private var flightResultPersisted = false

    init {
        viewModelScope.launch {
            cloudRepo.ensureInitialProfile(Firebase.auth.currentUser?.displayName)
        }
        viewModelScope.launch {
            settings.collect { s ->
                audioEngine.masterVolume = s.masterVolume
                audioEngine.engineVolume = s.engineVolume
                audioEngine.cabinVolume = s.cabinVolume
            }
        }
    }

    private suspend fun <T> FlowCollector<T>.handleNonFatalFlowError(error: Throwable, fallback: T) {
        val fsErr = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (fsErr == null && error is CancellationException) throw error
        Log.w(TAG, "Stream error", fsErr ?: error)
        emit(fallback)
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun toggleLanguage() {
        viewModelScope.launch { settingsRepo.toggleLanguage(settings.value.language) }
    }

    fun updateSettings(
        beginnerAssist: Boolean? = null,
        leftHanded: Boolean? = null,
        sensitivity: Float? = null,
        haptics: Boolean? = null,
        graphicsPreset: GraphicsPreset? = null,
        masterVol: Float? = null,
        engineVol: Float? = null,
        cabinVol: Float? = null
    ) {
        viewModelScope.launch {
            beginnerAssist?.let { settingsRepo.setBeginnerAssist(it) }
            leftHanded?.let { settingsRepo.setLeftHanded(it) }
            sensitivity?.let { settingsRepo.setTouchSensitivity(it) }
            haptics?.let { settingsRepo.setHapticsEnabled(it) }
            graphicsPreset?.let { settingsRepo.setGraphicsPreset(it) }
            if (masterVol != null || engineVol != null || cabinVol != null) {
                val cur = settings.value
                settingsRepo.setVolumes(
                    masterVol ?: cur.masterVolume,
                    engineVol ?: cur.engineVolume,
                    cabinVol ?: cur.cabinVolume
                )
            }
        }
    }

    fun updateBriefing(transform: (FlightBriefingConfig) -> FlightBriefingConfig) {
        _briefing.update(transform)
    }

    fun selectMission(mission: MissionDefinition) {
        _briefing.value = FlightBriefingConfig(
            mode = mission.mode,
            aircraftId = mission.recommendedAircraftId,
            liveryId = _briefing.value.liveryId,
            originIcao = mission.originIcao,
            destinationIcao = mission.destinationIcao,
            weatherPreset = mission.weatherPreset,
            timeOfDay = if (mission.emergencyType == EmergencyType.BAD_WEATHER_LANDING) TimeOfDay.SUNSET else TimeOfDay.DAY,
            season = if (mission.weatherPreset == WeatherPreset.FOG) SeasonType.WINTER_FOG else SeasonType.SUMMER_MONSOON,
            windMode = WindDirectionMode.CROSSWIND,
            missionId = mission.id
        )
    }

    fun startFlightSimulation() {
        stopFlightSimulation()
        val cfg = _briefing.value
        val origin = BangladeshGeoData.findAirport(cfg.originIcao)
        val spec = AircraftCatalog.findById(cfg.aircraftId)
        val mission = MissionCatalog.findById(cfg.missionId)
        val emergencyType = mission?.emergencyType ?: EmergencyType.NONE

        val initialWeather = weatherController.createInitialState(
            preset = cfg.weatherPreset,
            timeOfDay = cfg.timeOfDay,
            season = cfg.season,
            windMode = cfg.windMode,
            runwayHeadingDeg = origin.runwayHeadingDeg
        )
        val initialFlight = physicsEngine.createInitialState(
            originLat = origin.lat,
            originLon = origin.lon,
            groundElevFt = origin.elevationFt,
            runwayHeadingDeg = origin.runwayHeadingDeg,
            spec = spec,
            startAirborne = mission?.startAirborne ?: false,
            startAltFt = mission?.startAltitudeFt ?: origin.elevationFt.toFloat(),
            startSpeedKts = mission?.startSpeedKts ?: 0f,
            fuelFraction = mission?.startFuelFraction ?: 0.8f,
            emergencyType = emergencyType
        )

        _weatherState.value = initialWeather
        _flightState.value = initialFlight
        _controlInput.value = FlightControlInput(
            throttle = initialFlight.throttleActual,
            flapsLevel = initialFlight.flapsLevel,
            gearDown = initialFlight.gearDown
        )
        _flightPaused.value = false
        _debriefReport.value = null
        flightResultPersisted = false
        recordedFrames.clear()
        lastFrameRecordSec = -1f
        lastMpSyncSec = -1f

        audioEngine.startFlightAudio()
        val lang = settings.value.language
        audioEngine.speakAnnouncement(BdasStrings.takeoffAnnouncement(lang), lang)

        flightLoopJob = viewModelScope.launch {
            var lastNs = System.nanoTime()
            while (isActive) {
                delay(16L)
                val nowNs = System.nanoTime()
                val dt = ((nowNs - lastNs) / 1_000_000_000f).coerceIn(0.005f, 0.04f)
                lastNs = nowNs

                if (_flightPaused.value || _flightState.value.outcome != FlightOutcomeStatus.IN_PROGRESS) {
                    audioEngine.updateTelemetry(0f, 0f, 0f, 0f, spec.isTurboprop, false)
                    continue
                }

                val s = settings.value
                val nextWeather = weatherController.step(
                    _weatherState.value,
                    dt,
                    _flightState.value.altitudeFt,
                    _flightState.value.airspeedKts
                )
                val nextFlight = physicsEngine.step(
                    current = _flightState.value,
                    input = _controlInput.value,
                    spec = spec,
                    weather = nextWeather,
                    originLat = origin.lat,
                    originLon = origin.lon,
                    beginnerAssist = s.beginnerAssist,
                    sensitivity = s.touchSensitivity,
                    dtRaw = dt
                )

                _weatherState.value = nextWeather
                _flightState.value = nextFlight

                audioEngine.updateTelemetry(
                    throttle = nextFlight.throttleActual,
                    airspeedKts = nextFlight.airspeedKts,
                    rainIntensity = nextWeather.preset.rainIntensity,
                    turbulence = nextWeather.turbulenceIntensity,
                    turboprop = spec.isTurboprop,
                    stallWarning = nextFlight.stallWarning
                )

                // Record replay frames at 4Hz
                if (nextFlight.elapsedSec - lastFrameRecordSec >= 0.25f) {
                    lastFrameRecordSec = nextFlight.elapsedSec
                    val tag = when {
                        nextFlight.hasTakenOff && recordedFrames.none { it.eventTag == "TAKEOFF" } -> "TAKEOFF"
                        nextFlight.isOnGround && nextFlight.hasTakenOff -> "LANDING"
                        else -> ""
                    }
                    if (recordedFrames.size < 2400) {
                        recordedFrames.add(
                            ReplayFrameEntity(
                                replayId = 0L,
                                timestampSec = nextFlight.elapsedSec,
                                xMeters = nextFlight.xMeters,
                                zMeters = nextFlight.zMeters,
                                altitudeMeters = nextFlight.altitudeMeters,
                                pitchDeg = nextFlight.pitchDeg,
                                rollDeg = nextFlight.rollDeg,
                                headingDeg = nextFlight.headingDeg,
                                speedKts = nextFlight.airspeedKts,
                                throttle = nextFlight.throttleActual,
                                flapsLevel = nextFlight.flapsLevel,
                                gearDown = nextFlight.gearDown,
                                eventTag = tag
                            )
                        )
                    }
                }

                // Publish multiplayer telemetry at 1Hz when in an active session
                val sid = _joinedSessionId.value
                if (!sid.isNullOrBlank() && nextFlight.elapsedSec - lastMpSyncSec >= 1.0f) {
                    lastMpSyncSec = nextFlight.elapsedSec
                    val sess = activeSessions.value.firstOrNull { it.id == sid }
                    val profile = (profileState.value as? UiState.Success)?.data
                    if (sess != null && profile != null) {
                        launch {
                            multiplayerRepo.publishAircraftState(
                                sessionId = sid,
                                memberUids = sess.memberUids,
                                callsign = profile.callsign,
                                aircraftId = cfg.aircraftId,
                                latitude = nextFlight.latitude,
                                longitude = nextFlight.longitude,
                                altitudeFt = nextFlight.altitudeFt.toDouble(),
                                headingDeg = nextFlight.headingDeg.toDouble(),
                                pitchDeg = nextFlight.pitchDeg.toDouble(),
                                rollDeg = nextFlight.rollDeg.toDouble(),
                                speedKts = nextFlight.airspeedKts.toDouble()
                            )
                        }
                    }
                }

                // Evaluate flight completion or crash
                if (nextFlight.outcome == FlightOutcomeStatus.CRASHED && !flightResultPersisted) {
                    flightResultPersisted = true
                    completeFlight(success = false, reason = nextFlight.outcomeReason)
                } else if (nextFlight.hasTakenOff && nextFlight.isOnGround && nextFlight.airspeedKts < 18f && !flightResultPersisted) {
                    flightResultPersisted = true
                    _flightState.update { it.copy(outcome = FlightOutcomeStatus.LANDED_SAFELY) }
                    completeFlight(success = true, reason = "Safe Touchdown")
                }
            }
        }
    }

    fun finishAndEvaluateFlightNow() {
        if (flightResultPersisted) return
        val fs = _flightState.value
        val success = fs.outcome != FlightOutcomeStatus.CRASHED && (fs.hasTakenOff || fs.elapsedSec > 8f)
        flightResultPersisted = true
        completeFlight(
            success = success,
            reason = if (success) "Flight logged cleanly" else "Flight ended early"
        )
    }

    private fun completeFlight(success: Boolean, reason: String) {
        viewModelScope.launch {
            val cfg = _briefing.value
            val fs = _flightState.value
            val mission = MissionCatalog.findById(cfg.missionId)
            val profile = (profileState.value as? UiState.Success)?.data ?: PilotProfile(userId = currentUserId)
            val lang = settings.value.language

            val fpm = fs.lastTouchdownFpm
            val baseScore = if (!success) 120 else (700 + (300 - abs(fpm).coerceAtMost(300))).coerceIn(400, 1000)
            val xpEarned = if (success) (mission?.rewardXp ?: 220L) + (fs.elapsedSec.toLong() * 2L) else 40L
            val coinsEarned = if (success) (mission?.rewardCoins ?: 1400L) + (baseScore / 2L) else 150L
            val creditsEarned = if (success && mission != null) 4L else 1L

            val unlockedNow = mutableListOf<String>()
            fun checkUnlock(id: String, cond: Boolean) {
                if (cond && id !in profile.unlockedAchievementIds) unlockedNow.add(id)
            }
            checkUnlock("first_takeoff", fs.hasTakenOff)
            checkUnlock("first_landing", success && fs.hasTakenOff)
            checkUnlock("perfect_landing", success && fs.hasTakenOff && fpm in -180..-1)
            checkUnlock("emergency_landing", success && cfg.mode == FlightModeType.EMERGENCY)
            checkUnlock("night_flight", success && cfg.timeOfDay == TimeOfDay.NIGHT)
            checkUnlock("storm_flight", success && (cfg.weatherPreset == WeatherPreset.HEAVY_RAIN || cfg.weatherPreset == WeatherPreset.THUNDERSTORM))
            checkUnlock("long_distance", (profile.flightHours * 3600.0 + fs.elapsedSec) >= 900.0)
            checkUnlock("career_milestone", PilotRank.forXp(profile.xp + xpEarned).ordinal >= 1)

            val savedReplayId = runCatching {
                replayRepo.saveRecordedReplay(
                    title = "${cfg.originIcao} -> ${cfg.destinationIcao} (${cfg.aircraftId.uppercase()})",
                    aircraftId = cfg.aircraftId,
                    originIcao = cfg.originIcao,
                    destinationIcao = cfg.destinationIcao,
                    weatherPreset = cfg.weatherPreset.name,
                    durationSec = fs.elapsedSec,
                    landingFpm = fpm,
                    frames = recordedFrames.toList()
                )
            }.getOrNull()

            cloudRepo.recordCompletedFlight(
                currentProfile = profile,
                mode = cfg.mode.name,
                aircraftId = cfg.aircraftId,
                originIcao = cfg.originIcao,
                destinationIcao = cfg.destinationIcao,
                missionId = cfg.missionId,
                landingFpm = fpm,
                score = baseScore,
                xpEarned = xpEarned,
                coinsEarned = coinsEarned,
                creditsEarned = creditsEarned,
                weatherPreset = cfg.weatherPreset.name,
                durationSec = fs.elapsedSec.toLong(),
                newlyUnlockedAchievements = unlockedNow
            )

            if (success) {
                audioEngine.speakAnnouncement(BdasStrings.landingAnnouncement(lang, cfg.destinationIcao), lang)
            }

            _debriefReport.value = FlightDebriefReport(
                success = success,
                title = if (success) (if (lang == AppLanguage.BANGLA) "ফ্লাইট সফল!" else "Flight Completed") else (if (lang == AppLanguage.BANGLA) "জরুরি ব্যর্থতা" else "Flight Incident"),
                summary = reason,
                landingFpm = fpm,
                score = baseScore,
                xpEarned = xpEarned,
                coinsEarned = coinsEarned,
                newAchievements = unlockedNow,
                savedReplayId = savedReplayId
            )
        }
    }

    fun stopFlightSimulation() {
        flightLoopJob?.cancel()
        flightLoopJob = null
        audioEngine.stopFlightAudio()
    }

    fun setControls(transform: (FlightControlInput) -> FlightControlInput) {
        _controlInput.update(transform)
    }

    fun cycleCamera() {
        val entries = CameraMode.entries
        _cameraMode.value = entries[(_cameraMode.value.ordinal + 1) % entries.size]
    }

    fun togglePause() {
        _flightPaused.update { !it }
    }

    // --- Passenger Simulator Actions ---

    fun resetPassengerJourney() {
        passengerCruiseJob?.cancel()
        _passengerState.value = PassengerSessionState()
        val lang = settings.value.language
        audioEngine.speakAnnouncement(BdasStrings.welcomeAnnouncement(lang, "Dhaka", "Chattogram"), lang)
    }

    fun movePassenger(forward: Float, strafe: Float) {
        _passengerState.update { passengerController.move(it, forward, strafe, 0.05f) }
    }

    fun lookPassenger(dYaw: Float, dPitch: Float) {
        _passengerState.update { passengerController.look(it, dYaw, dPitch) }
    }

    fun togglePassengerCamera() {
        _passengerState.update { passengerController.toggleThirdPerson(it) }
    }

    fun selectPassengerRoute(originIcao: String, destIcao: String) {
        val dist = BangladeshGeoData.distanceNm(
            BangladeshGeoData.findAirport(originIcao),
            BangladeshGeoData.findAirport(destIcao)
        )
        val fare = (dist * 12.0).roundToInt().toLong().coerceIn(800L, 6500L)
        _passengerState.update {
            it.copy(
                originIcao = originIcao,
                destinationIcao = destIcao,
                ticketFareCoins = fare,
                stage = PassengerStage.PURCHASE_TICKET
            )
        }
    }

    fun selectPassengerSeat(seatCode: String, row: Int, rightSide: Boolean, isWindow: Boolean) {
        _passengerState.update {
            it.copy(
                selectedSeat = seatCode,
                seatRow = row,
                seatIsRightSide = rightSide,
                seatIsWindow = isWindow,
                renderState = it.renderState.copy(seatRow = row, seatIsRightSide = rightSide)
            )
        }
    }

    fun advancePassengerStage(shopItem: String = "") {
        val cur = _passengerState.value
        val lang = settings.value.language
        when (cur.stage) {
            PassengerStage.ARRIVE_AIRPORT -> _passengerState.update { it.copy(stage = PassengerStage.ENTER_TERMINAL) }
            PassengerStage.ENTER_TERMINAL -> _passengerState.update { it.copy(stage = PassengerStage.SELECT_FLIGHT) }
            PassengerStage.SELECT_FLIGHT -> _passengerState.update { it.copy(stage = PassengerStage.PURCHASE_TICKET) }
            PassengerStage.PURCHASE_TICKET -> _passengerState.update {
                it.copy(stage = PassengerStage.CHECK_IN, boardingPassCode = "BDAS-${(1000..9999).random()}")
            }
            PassengerStage.CHECK_IN -> _passengerState.update {
                it.copy(stage = PassengerStage.BOARDING_PASS, luggageChecked = true)
            }
            PassengerStage.BOARDING_PASS -> _passengerState.update { it.copy(stage = PassengerStage.SECURITY_CHECK) }
            PassengerStage.SECURITY_CHECK -> _passengerState.update { it.copy(stage = PassengerStage.TERMINAL_SHOPS) }
            PassengerStage.TERMINAL_SHOPS -> {
                _passengerState.update {
                    it.copy(
                        stage = PassengerStage.BOARDING_GATE,
                        snackPurchased = shopItem.ifBlank { it.snackPurchased }
                    )
                }
                audioEngine.speakAnnouncement(BdasStrings.welcomeAnnouncement(lang, cur.originIcao, cur.destinationIcao), lang)
            }
            PassengerStage.BOARDING_GATE -> _passengerState.update {
                it.copy(
                    stage = PassengerStage.BOARD_AIRCRAFT,
                    renderState = it.renderState.copy(playerX = 0f, playerZ = 0f, yawDeg = 0f)
                )
            }
            PassengerStage.BOARD_AIRCRAFT -> _passengerState.update { it.copy(stage = PassengerStage.FIND_SEAT) }
            PassengerStage.FIND_SEAT -> {
                _passengerState.update {
                    it.copy(
                        stage = PassengerStage.EXPERIENCE_TAKEOFF,
                        renderState = it.renderState.copy(seated = true, yawDeg = if (it.seatIsRightSide) 90f else 270f)
                    )
                }
                audioEngine.speakAnnouncement(BdasStrings.takeoffAnnouncement(lang), lang)
                startPassengerFlightArc()
            }
            PassengerStage.EXPERIENCE_TAKEOFF -> _passengerState.update {
                it.copy(stage = PassengerStage.CABIN_CRUISE, renderState = it.renderState.copy(seated = false))
            }
            PassengerStage.CABIN_CRUISE -> {
                _passengerState.update {
                    it.copy(
                        stage = PassengerStage.EXPERIENCE_LANDING,
                        renderState = it.renderState.copy(seated = true, yawDeg = if (it.seatIsRightSide) 90f else 270f)
                    )
                }
                audioEngine.speakAnnouncement(BdasStrings.landingAnnouncement(lang, cur.destinationIcao), lang)
            }
            PassengerStage.EXPERIENCE_LANDING -> {
                passengerCruiseJob?.cancel()
                _passengerState.update {
                    it.copy(
                        stage = PassengerStage.BAGGAGE_CLAIM,
                        renderState = it.renderState.copy(seated = false, flightAltitudeM = 10f, flightSpeedKts = 0f)
                    )
                }
            }
            PassengerStage.BAGGAGE_CLAIM -> {
                if (!cur.journeyCompleted) {
                    _passengerState.update { it.copy(journeyCompleted = true) }
                    completePassengerJourneyReward(cur)
                }
            }
        }
    }

    fun interactWithCabinCrew(serviceItem: String) {
        val lang = settings.value.language
        val msg = passengerController.crewMessage(lang, serviceItem)
        _passengerState.update { it.copy(crewGreeting = msg) }
        audioEngine.speakAnnouncement(msg, lang)
    }

    private fun startPassengerFlightArc() {
        passengerCruiseJob?.cancel()
        passengerCruiseJob = viewModelScope.launch {
            var t = 0f
            while (isActive) {
                delay(50L)
                t = (t + 0.015f).coerceAtMost(1f)
                val altM = when (_passengerState.value.stage) {
                    PassengerStage.EXPERIENCE_TAKEOFF -> 15f + t * 2400f
                    PassengerStage.CABIN_CRUISE -> 2600f
                    PassengerStage.EXPERIENCE_LANDING -> (2400f * (1f - t)).coerceAtLeast(15f)
                    else -> 12f
                }
                val spd = when (_passengerState.value.stage) {
                    PassengerStage.EXPERIENCE_TAKEOFF -> 80f + t * 180f
                    PassengerStage.CABIN_CRUISE -> 380f
                    PassengerStage.EXPERIENCE_LANDING -> 165f * (1f - t * 0.6f)
                    else -> 0f
                }
                _passengerState.update {
                    it.copy(
                        flightProgress = t,
                        renderState = it.renderState.copy(
                            flightAltitudeM = altM,
                            flightSpeedKts = spd,
                            flightRollDeg = sin(t * 6.28f) * 4f
                        )
                    )
                }
            }
        }
    }

    private fun completePassengerJourneyReward(ps: PassengerSessionState) {
        viewModelScope.launch {
            val profile = (profileState.value as? UiState.Success)?.data ?: return@launch
            val unlocks = if ("passenger_transport" !in profile.unlockedAchievementIds) listOf("passenger_transport") else emptyList()
            cloudRepo.recordCompletedFlight(
                currentProfile = profile,
                mode = FlightModeType.PASSENGER.name,
                aircraftId = "a320",
                originIcao = ps.originIcao,
                destinationIcao = ps.destinationIcao,
                missionId = "passenger_journey",
                landingFpm = -120,
                score = 950,
                xpEarned = 180L,
                coinsEarned = 950L,
                creditsEarned = 2L,
                weatherPreset = WeatherPreset.CLEAR.name,
                durationSec = 180L,
                newlyUnlockedAchievements = unlocks
            )
            _bannerMessage.value = "Passenger Journey Complete! +950 BDAS Coins"
        }
    }

    // --- Hangar, World Map & Airline Management ---

    fun unlockAircraft(spec: AircraftSpec) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val res = cloudRepo.unlockAircraft(profile, spec.id, spec.purchasePriceCoins)
            _bannerMessage.value = res.fold(
                onSuccess = { "${spec.nameEn} unlocked!" },
                onFailure = { it.localizedMessage ?: "Failed to unlock aircraft" }
            )
        }
    }

    fun unlockAirport(icao: String, costCoins: Long) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val res = cloudRepo.unlockAirport(profile, icao, costCoins)
            _bannerMessage.value = res.fold(
                onSuccess = { "Airport $icao unlocked!" },
                onFailure = { it.localizedMessage ?: "Failed to unlock airport" }
            )
        }
    }

    fun saveAirline(
        name: String,
        iataCode: String,
        logoId: String,
        primaryHex: String,
        secondaryHex: String,
        pilotsDelta: Long = 0L,
        crewDelta: Long = 0L
    ) {
        val existing = airlines.value.firstOrNull()
        viewModelScope.launch {
            val res = cloudRepo.createOrUpdateAirline(
                existingId = existing?.id,
                name = name,
                iataCode = iataCode,
                logoId = logoId,
                primaryHex = primaryHex,
                secondaryHex = secondaryHex,
                reputation = existing?.reputation ?: 68L,
                pilotsHired = ((existing?.pilotsHired ?: 2L) + pilotsDelta).coerceAtLeast(1L),
                cabinCrewHired = ((existing?.cabinCrewHired ?: 4L) + crewDelta).coerceAtLeast(2L),
                dailyRevenue = existing?.dailyRevenue ?: 0L,
                dailyExpenses = existing?.dailyExpenses ?: 0L
            )
            _bannerMessage.value = res.fold(
                onSuccess = { "Airline saved!" },
                onFailure = { it.localizedMessage ?: "Airline update failed" }
            )
        }
    }

    fun purchaseFleetForAirline(spec: AircraftSpec, liveryName: String) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        val airline = airlines.value.firstOrNull() ?: run {
            _bannerMessage.value = "Create your airline first!"
            return
        }
        val cost = spec.purchasePriceCoins.coerceAtLeast(8000L)
        val reg = "S2-${('A'..'Z').random()}${('A'..'Z').random()}${('A'..'Z').random()}"
        viewModelScope.launch {
            val res = cloudRepo.purchaseFleetAircraft(profile, airline.id, spec.id, reg, liveryName, cost)
            _bannerMessage.value = res.fold(
                onSuccess = { "Added $reg to your fleet!" },
                onFailure = { it.localizedMessage ?: "Purchase failed" }
            )
        }
    }

    fun openAirlineRoute(originIcao: String, destIcao: String, ticketPriceCoins: Long) {
        val airline = airlines.value.firstOrNull() ?: run {
            _bannerMessage.value = "Create your airline first!"
            return
        }
        val fleetId = fleet.value.firstOrNull()?.id ?: ""
        val demand = AirlineEconomyEngine.computeDemandScore(originIcao, destIcao, ticketPriceCoins, airline.reputation)
        viewModelScope.launch {
            val res = cloudRepo.createRoute(airline.id, originIcao, destIcao, ticketPriceCoins, demand, fleetId)
            _bannerMessage.value = res.fold(
                onSuccess = { "Route $originIcao -> $destIcao opened! Demand $demand%" },
                onFailure = { it.localizedMessage ?: "Could not create route" }
            )
        }
    }

    fun runAirlineOperatingCycle() {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        val airline = airlines.value.firstOrNull() ?: run {
            _bannerMessage.value = "Create an airline first!"
            return
        }
        val report = AirlineEconomyEngine.simulateOperatingCycle(airline, fleet.value, routes.value)
        _lastCycleReport.value = report
        viewModelScope.launch {
            cloudRepo.settleAirlineCycle(
                currentProfile = profile,
                airlineId = airline.id,
                netCoins = report.netProfitCoins,
                grossIncome = report.grossRevenueCoins,
                totalExpenses = report.fuelExpenseCoins + report.crewSalaryCoins + report.airportFeeCoins + report.maintenanceCoins,
                summary = "Daily Ops: ${report.passengersCarried} pax, net ${report.netProfitCoins} coins"
            )
        }
    }

    // --- Multiplayer ---

    fun createMultiplayerSession(title: String, airportIcao: String, weather: WeatherPreset) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val res = multiplayerRepo.createSession(
                hostCallsign = profile.callsign,
                title = title,
                airportIcao = airportIcao,
                weatherPreset = weather.name,
                missionType = "FREE_FLIGHT"
            )
            res.onSuccess { sid ->
                _joinedSessionId.value = sid
                _briefing.update { it.copy(originIcao = airportIcao, weatherPreset = weather) }
                _bannerMessage.value = "Created & joined multiplayer session"
            }.onFailure { err ->
                _bannerMessage.value = err.localizedMessage ?: "Failed to create session"
            }
        }
    }

    fun joinMultiplayerSession(session: MultiplayerSessionDoc) {
        viewModelScope.launch {
            multiplayerRepo.joinSession(session)
                .onSuccess {
                    _joinedSessionId.value = session.id
                    val weather = WeatherPreset.entries.firstOrNull { it.name == session.weatherPreset } ?: WeatherPreset.CLEAR
                    _briefing.update { it.copy(originIcao = session.airportIcao, weatherPreset = weather) }
                    _bannerMessage.value = "Joined ${session.title}"
                }
                .onFailure { err ->
                    _bannerMessage.value = err.localizedMessage ?: "Failed to join session"
                }
        }
    }

    fun startJoinedMultiplayerSession() {
        val sid = _joinedSessionId.value ?: return
        viewModelScope.launch {
            multiplayerRepo.startSession(sid)
        }
    }

    fun leaveMultiplayerSession() {
        val sid = _joinedSessionId.value ?: return
        val session = activeSessions.value.firstOrNull { it.id == sid }
        _joinedSessionId.value = null
        if (session != null) {
            viewModelScope.launch { multiplayerRepo.leaveSession(session) }
        }
    }

    fun addFriend(friendUid: String, callsign: String) {
        viewModelScope.launch {
            multiplayerRepo.addFriend(friendUid, callsign, "FIRST_OFFICER")
                .onSuccess { _bannerMessage.value = "Friend added!" }
                .onFailure { _bannerMessage.value = it.localizedMessage ?: "Could not add friend" }
        }
    }

    fun activePeerRenders(): List<PeerAircraftRender> {
        val cfg = _briefing.value
        val origin = BangladeshGeoData.findAirport(cfg.originIcao)
        val cosLat = cos(Math.toRadians(origin.lat))
        return peerAircraftStates.value
            .filter { it.playerUid != currentUserId }
            .map { peer ->
                val xM = ((peer.longitude - origin.lon) * METERS_PER_DEG_LAT * cosLat).toFloat()
                val zM = ((peer.latitude - origin.lat) * METERS_PER_DEG_LAT).toFloat()
                PeerAircraftRender(
                    callsign = peer.callsign,
                    aircraftId = peer.aircraftId,
                    xMeters = xM,
                    zMeters = zM,
                    altitudeMeters = (peer.altitudeFt * 0.3048).toFloat(),
                    headingDeg = peer.headingDeg.toFloat(),
                    pitchDeg = peer.pitchDeg.toFloat(),
                    rollDeg = peer.rollDeg.toFloat()
                )
            }
    }

    // --- Replay Playback ---

    fun openReplay(replay: FlightReplayEntity) {
        replayPlaybackJob?.cancel()
        viewModelScope.launch {
            val frames = replayRepo.loadFrames(replay.id)
            _replayPlayback.value = ReplayPlaybackState(
                replay = replay,
                frames = frames,
                currentIndex = 0,
                playing = frames.isNotEmpty()
            )
            if (frames.isNotEmpty()) startReplayTimer()
        }
    }

    fun toggleReplayPlay() {
        val cur = _replayPlayback.value
        if (cur.frames.isEmpty()) return
        val nextPlay = !cur.playing
        _replayPlayback.value = cur.copy(playing = nextPlay)
        if (nextPlay) startReplayTimer() else replayPlaybackJob?.cancel()
    }

    fun seekReplay(index: Int) {
        val cur = _replayPlayback.value
        if (cur.frames.isEmpty()) return
        _replayPlayback.value = cur.copy(currentIndex = index.coerceIn(0, cur.frames.lastIndex))
    }

    fun jumpToReplayEvent(tag: String) {
        val cur = _replayPlayback.value
        val idx = cur.frames.indexOfFirst { it.eventTag == tag }
        if (idx >= 0) seekReplay(idx)
    }

    fun cycleReplayCamera() {
        val entries = CameraMode.entries
        _replayPlayback.update {
            it.copy(cameraMode = entries[(it.cameraMode.ordinal + 1) % entries.size])
        }
    }

    fun stopReplay() {
        replayPlaybackJob?.cancel()
        _replayPlayback.value = ReplayPlaybackState()
    }

    private fun startReplayTimer() {
        replayPlaybackJob?.cancel()
        replayPlaybackJob = viewModelScope.launch {
            while (isActive) {
                delay(250L)
                val cur = _replayPlayback.value
                if (!cur.playing || cur.frames.isEmpty()) continue
                val next = (cur.currentIndex + 1) % cur.frames.size
                _replayPlayback.value = cur.copy(currentIndex = next)
            }
        }
    }

    fun cleanupBeforeSignOut() {
        stopFlightSimulation()
        stopReplay()
        passengerCruiseJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        cleanupBeforeSignOut()
        audioEngine.shutdown()
    }

    class Factory(
        private val app: Application,
        private val currentUserId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = FirestoreInstanceProvider.get(app)
            val localDb = BdasLocalDatabase.getInstance(app)
            return MainAppViewModel(
                currentUserId = currentUserId,
                cloudRepo = CloudSyncRepository(db),
                multiplayerRepo = MultiplayerRepository(db),
                replayRepo = ReplayRepository(localDb.replayDao()),
                settingsRepo = SettingsRepository(app),
                audioEngine = AviationAudioEngine(app)
            ) as T
        }
    }
}

fun mainAppViewModelFactory(app: Application, currentUserId: String): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            MainAppViewModel.Factory(app, currentUserId).create(MainAppViewModel::class.java)
        }
    }
