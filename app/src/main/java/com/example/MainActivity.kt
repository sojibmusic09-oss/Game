package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.attemptAutoSignIn
import com.example.data.authStateFlow
import com.example.data.signOut
import com.example.ui.*
import com.example.ui.theme.BangladeshEmerald
import com.example.ui.theme.BdasTheme
import com.example.ui.theme.CockpitVoid
import com.example.viewmodel.MainAppViewModel
import com.example.viewmodel.UiState
import com.example.viewmodel.mainAppViewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BdasTheme {
                BdasRootApp()
            }
        }
    }
}

@Composable
private fun BdasRootApp() {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
    var autoSignInChecking by remember { mutableStateOf(Firebase.auth.currentUser == null) }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { autoSignInChecking = false },
                onUnauthenticated = { autoSignInChecking = false },
                scope = scope
            )
        } else {
            autoSignInChecking = false
        }
    }

    when {
        autoSignInChecking -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CockpitVoid),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BangladeshEmerald)
            }
        }
        currentUser == null -> {
            AuthScreen(onAuthSuccess = { autoSignInChecking = false })
        }
        else -> {
            val uid = currentUser!!.uid
            key(uid) {
                val vm: MainAppViewModel = viewModel(
                    key = "bdas_vm_$uid",
                    factory = mainAppViewModelFactory(app, uid)
                )
                BdasAuthenticatedNavHost(
                    vm = vm,
                    onRequestSignOut = {
                        signOut(
                            credentialManager = credentialManager,
                            scope = scope,
                            onBeforeSignOut = { vm.cleanupBeforeSignOut() }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun BdasAuthenticatedNavHost(
    vm: MainAppViewModel,
    onRequestSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profileState by vm.profileState.collectAsStateWithLifecycle()
    val profile = (profileState as? UiState.Success)?.data
    val bannerMessage by vm.bannerMessage.collectAsStateWithLifecycle()

    val briefing by vm.briefing.collectAsStateWithLifecycle()
    val flightState by vm.flightState.collectAsStateWithLifecycle()
    val controlInput by vm.controlInput.collectAsStateWithLifecycle()
    val weatherState by vm.weatherState.collectAsStateWithLifecycle()
    val cameraMode by vm.cameraMode.collectAsStateWithLifecycle()
    val flightPaused by vm.flightPaused.collectAsStateWithLifecycle()
    val debriefReport by vm.debriefReport.collectAsStateWithLifecycle()

    val passengerState by vm.passengerState.collectAsStateWithLifecycle()
    val airlines by vm.airlines.collectAsStateWithLifecycle()
    val fleet by vm.fleet.collectAsStateWithLifecycle()
    val routes by vm.routes.collectAsStateWithLifecycle()
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    val flightLogs by vm.flightLogs.collectAsStateWithLifecycle()
    val lastCycleReport by vm.lastCycleReport.collectAsStateWithLifecycle()

    val activeSessions by vm.activeSessions.collectAsStateWithLifecycle()
    val joinedSessionId by vm.joinedSessionId.collectAsStateWithLifecycle()
    val friends by vm.friends.collectAsStateWithLifecycle()
    val peerStates by vm.peerAircraftStates.collectAsStateWithLifecycle()

    val replays by vm.replays.collectAsStateWithLifecycle()
    val replayPlayback by vm.replayPlayback.collectAsStateWithLifecycle()

    val lang = settings.language

    NavHost(
        navController = navController,
        startDestination = Routes.MENU
    ) {
        composable(Routes.MENU) {
            MainMenuScreen(
                language = lang,
                profile = profile,
                bannerMessage = bannerMessage,
                onDismissBanner = vm::clearBanner,
                onToggleLanguage = vm::toggleLanguage,
                onStartFlightSetup = { mode ->
                    vm.updateBriefing { it.copy(mode = mode, missionId = "") }
                    navController.navigate(Routes.FLIGHT_SETUP)
                },
                onNavigate = { route ->
                    if (route == Routes.PASSENGER) {
                        vm.resetPassengerJourney()
                    }
                    navController.navigate(route)
                },
                onSignOut = onRequestSignOut
            )
        }

        composable(Routes.FLIGHT_SETUP) {
            FlightSetupScreen(
                language = lang,
                profile = profile,
                briefing = briefing,
                onToggleLanguage = vm::toggleLanguage,
                onBack = { navController.popBackStack() },
                onUpdateBriefing = vm::updateBriefing,
                onSelectMission = vm::selectMission,
                onLaunchFlight = {
                    vm.startFlightSimulation()
                    navController.navigate(Routes.IN_FLIGHT)
                }
            )
        }

        composable(Routes.IN_FLIGHT) {
            val peerRenders = remember(peerStates, briefing.originIcao) { vm.activePeerRenders() }
            InFlightScreen(
                language = lang,
                settings = settings,
                briefing = briefing,
                flightState = flightState,
                controlInput = controlInput,
                weatherState = weatherState,
                cameraMode = cameraMode,
                paused = flightPaused,
                debriefReport = debriefReport,
                peerAircraft = peerRenders,
                onUpdateControls = vm::setControls,
                onCycleCamera = vm::cycleCamera,
                onTogglePause = vm::togglePause,
                onEndFlightEarly = vm::finishAndEvaluateFlightNow,
                onWatchReplay = { replayId ->
                    vm.stopFlightSimulation()
                    val found = replays.firstOrNull { it.id == replayId } ?: replays.firstOrNull()
                    if (found != null) vm.openReplay(found)
                    navController.navigate(Routes.REPLAY) {
                        popUpTo(Routes.MENU)
                    }
                },
                onExitToMenu = {
                    vm.stopFlightSimulation()
                    navController.popBackStack(Routes.MENU, inclusive = false)
                }
            )
        }

        composable(Routes.PASSENGER) {
            BackHandler {
                navController.popBackStack()
            }
            PassengerScreen(
                language = lang,
                settings = settings,
                profile = profile,
                passengerState = passengerState,
                onToggleLanguage = vm::toggleLanguage,
                onBack = { navController.popBackStack() },
                onMove = vm::movePassenger,
                onLook = vm::lookPassenger,
                onToggleCamera = vm::togglePassengerCamera,
                onSelectRoute = vm::selectPassengerRoute,
                onSelectSeat = vm::selectPassengerSeat,
                onAdvanceStage = vm::advancePassengerStage,
                onCrewService = vm::interactWithCabinCrew,
                onResetJourney = vm::resetPassengerJourney
            )
        }

        composable(Routes.CAREER) {
            CareerScreen(
                language = lang,
                profile = profile,
                flightLogs = flightLogs,
                onToggleLanguage = vm::toggleLanguage,
                onBack = { navController.popBackStack() },
                onStartMission = { mission ->
                    vm.selectMission(mission)
                    vm.startFlightSimulation()
                    navController.navigate(Routes.IN_FLIGHT)
                }
            )
        }

        composable(Routes.AIRLINE) {
            AirlineScreen(
                language = lang,
                profile = profile,
                airlines = airlines,
                fleet = fleet,
                routes = routes,
                transactions = transactions,
                lastCycleReport = lastCycleReport,
                bannerMessage = bannerMessage,
                onDismissBanner = vm::clearBanner,
                onToggleLanguage = vm::toggleLanguage,
                onBack = { navController.popBackStack() },
                onSaveAirline = vm::saveAirline,
                onBuyFleet = vm::purchaseFleetForAirline,
                onOpenRoute = vm::openAirlineRoute,
                onRunCycle = vm::runAirlineOperatingCycle
            )
        }

        composable(Routes.HANGAR) {
            HangarScreen(
                language = lang,
                profile = profile,
                selectedAircraftId = briefing.aircraftId,
                selectedLiveryId = briefing.liveryId,
                bannerMessage = bannerMessage,
                onDismissBanner = vm::clearBanner,
                onToggleLanguage = vm::toggleLanguage,
                onBack = { navController.popBackStack() },
                onUnlockAircraft = vm::unlockAircraft,
                onSelectForFlight = { aircraftId, liveryId ->
                    vm.updateBriefing { it.copy(aircraftId = aircraftId, liveryId = liveryId) }
                    navController.navigate(Routes.FLIGHT_SETUP)
                }
            )
        }

        composable(Routes.WORLD_MAP) {
            WorldMapScreen(
                language = lang,
                profile = profile,
                bannerMessage = bannerMessage,
                onDismissBanner = vm::clearBanner,
                onToggleLanguage = vm::toggleLanguage,
                onBack = { navController.popBackStack() },
                onUnlockAirport = vm::unlockAirport,
                onFlyRoute = { origIcao, destIcao ->
                    vm.updateBriefing { it.copy(originIcao = origIcao, destinationIcao = destIcao) }
                    navController.navigate(Routes.FLIGHT_SETUP)
                }
            )
        }

        composable(Routes.MULTIPLAYER) {
            MultiplayerScreen(
                language = lang,
                profile = profile,
                activeSessions = activeSessions,
                joinedSessionId = joinedSessionId,
                friends = friends,
                bannerMessage = bannerMessage,
                onDismissBanner = vm::clearBanner,
                onToggleLanguage = vm::toggleLanguage,
                onBack = { navController.popBackStack() },
                onCreateSession = vm::createMultiplayerSession,
                onJoinSession = vm::joinMultiplayerSession,
                onStartSession = vm::startJoinedMultiplayerSession,
                onLeaveSession = vm::leaveMultiplayerSession,
                onLaunchOnlineFlight = {
                    vm.startFlightSimulation()
                    navController.navigate(Routes.IN_FLIGHT)
                },
                onAddFriend = vm::addFriend
            )
        }

        composable(Routes.REPLAY) {
            BackHandler {
                vm.stopReplay()
                navController.popBackStack()
            }
            ReplayScreen(
                language = lang,
                profile = profile,
                replays = replays,
                playback = replayPlayback,
                onToggleLanguage = vm::toggleLanguage,
                onBack = {
                    vm.stopReplay()
                    navController.popBackStack()
                },
                onOpenReplay = vm::openReplay,
                onTogglePlay = vm::toggleReplayPlay,
                onSeek = vm::seekReplay,
                onJumpEvent = vm::jumpToReplayEvent,
                onCycleCamera = vm::cycleReplayCamera
            )
        }

        composable(Routes.ACHIEVEMENTS) {
            AchievementsScreen(
                language = lang,
                profile = profile,
                onToggleLanguage = vm::toggleLanguage,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                language = lang,
                profile = profile,
                settings = settings,
                onToggleLanguage = vm::toggleLanguage,
                onBack = { navController.popBackStack() },
                onUpdateSettings = vm::updateSettings,
                onSignOut = onRequestSignOut
            )
        }
    }
}
