package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.AppLanguage
import com.example.core.BdasStrings
import com.example.data.PilotProfile
import com.example.missions.FlightModeType
import com.example.ui.theme.*

object Routes {
    const val MENU = "menu"
    const val FLIGHT_SETUP = "flight_setup"
    const val IN_FLIGHT = "in_flight"
    const val PASSENGER = "passenger"
    const val CAREER = "career"
    const val AIRLINE = "airline"
    const val HANGAR = "hangar"
    const val WORLD_MAP = "world_map"
    const val MULTIPLAYER = "multiplayer"
    const val REPLAY = "replay"
    const val ACHIEVEMENTS = "achievements"
    const val SETTINGS = "settings"
}

private data class MenuTile(
    val tag: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val onClick: () -> Unit
)

@Composable
fun MainMenuScreen(
    language: AppLanguage,
    profile: PilotProfile?,
    bannerMessage: String?,
    onDismissBanner: () -> Unit,
    onToggleLanguage: () -> Unit,
    onStartFlightSetup: (FlightModeType) -> Unit,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val tiles = listOf(
        MenuTile(
            "menu_new_flight",
            BdasStrings.newFlight(language),
            if (isBn) "ফ্রি ফ্লাইট, প্রশিক্ষণ ও জরুরি মিশন" else "Configure aircraft, weather & mission",
            Icons.Filled.FlightTakeoff,
            BangladeshEmerald
        ) { onStartFlightSetup(FlightModeType.FREE_FLIGHT) },
        MenuTile(
            "menu_pilot_career",
            BdasStrings.pilotCareer(language),
            if (isBn) "পদমর্যাদা, পরীক্ষা ও ইতিহাস" else "Ranks, license exams & logs",
            Icons.Filled.MilitaryTech,
            HudCyan
        ) { onNavigate(Routes.CAREER) },
        MenuTile(
            "menu_passenger_mode",
            BdasStrings.passengerJourney(language),
            if (isBn) "টার্মিনাল, বোর্ডিং ও কেবিন ৩ডি" else "Walk terminal, board & fly cabin 3D",
            Icons.Filled.AirlineSeatReclineExtra,
            WarningAmber
        ) { onNavigate(Routes.PASSENGER) },
        MenuTile(
            "menu_free_flight",
            BdasStrings.freeFlight(language),
            if (isBn) "বাংলাদেশের আকাশে সরাসরি উড়ুন" else "Explore all 8 divisions & skies",
            Icons.Filled.AirplanemodeActive,
            BangladeshEmerald
        ) { onStartFlightSetup(FlightModeType.FREE_FLIGHT) },
        MenuTile(
            "menu_airline_mgmt",
            BdasStrings.airlineMgmt(language),
            if (isBn) "ফ্লিট, রুট, টিকিট ও অর্থনীতি" else "Fleet, routes, pricing & finances",
            Icons.Filled.BusinessCenter,
            HudCyan
        ) { onNavigate(Routes.AIRLINE) },
        MenuTile(
            "menu_multiplayer",
            BdasStrings.multiplayer(language),
            if (isBn) "অনলাইন লবি, বন্ধু ও ফ্লাইট সেশন" else "Online lobbies, friends & shared sky",
            Icons.Filled.Groups,
            BangladeshEmerald
        ) { onNavigate(Routes.MULTIPLAYER) },
        MenuTile(
            "menu_training",
            BdasStrings.training(language),
            if (isBn) "টেকঅফ ও জরুরি অবতরণ অনুশীলন" else "Guided takeoff & emergency drills",
            Icons.Filled.School,
            WarningAmber
        ) { onStartFlightSetup(FlightModeType.TRAINING) },
        MenuTile(
            "menu_hangar",
            BdasStrings.hangar(language),
            if (isBn) "বিমানের তথ্য, ইঞ্জিন ও রঙ" else "4 aircraft specs & custom liveries",
            Icons.Filled.Warehouse,
            HudCyan
        ) { onNavigate(Routes.HANGAR) },
        MenuTile(
            "menu_world_map",
            BdasStrings.worldMap(language),
            if (isBn) "৮ বিভাগ, ৬৪ জেলা ও ৮ বিমানবন্দর" else "8 divisions, 64 districts & rivers",
            Icons.Filled.Public,
            BangladeshEmerald
        ) { onNavigate(Routes.WORLD_MAP) },
        MenuTile(
            "menu_replay",
            BdasStrings.replay(language),
            if (isBn) "সংরক্ষিত ৩ডি ফ্লাইট পুনরায় দেখুন" else "Reconstruct recorded 3D flights",
            Icons.Filled.SlowMotionVideo,
            WarningAmber
        ) { onNavigate(Routes.REPLAY) },
        MenuTile(
            "menu_achievements",
            BdasStrings.achievements(language),
            if (isBn) "অর্জন ও পুরস্কার দেখুন" else "${profile?.unlockedAchievementIds?.size ?: 0}/10 milestones unlocked",
            Icons.Filled.EmojiEvents,
            WarningAmber
        ) { onNavigate(Routes.ACHIEVEMENTS) },
        MenuTile(
            "menu_settings",
            BdasStrings.settings(language),
            if (isBn) "কন্ট্রোল, গ্রাফিক্স ও অডিও" else "Touch controls, graphics & audio",
            Icons.Filled.Settings,
            TextMutedSlate
        ) { onNavigate(Routes.SETTINGS) }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitVoid)
    ) {
        BdasTopBar(
            title = BdasStrings.title(language),
            language = language,
            profile = profile,
            onBack = null,
            onToggleLanguage = onToggleLanguage,
            actions = {
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = BdasStrings.signOut(language), tint = CautionCrimson)
                }
            }
        )

        BannerAlert(message = bannerMessage, onDismiss = onDismissBanner)

        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Hero Featured Launch Panel
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, InstrumentBorder),
                color = InstrumentSlate,
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_bdas_hero_1795772244695),
                        contentDescription = stringResource(R.string.cd_hero_banner),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        alpha = 0.65f
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, CockpitVoid.copy(alpha = 0.94f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${profile?.callsign ?: "BD-101"} • ${profile?.displayName ?: "Captain"}",
                            style = MaterialTheme.typography.labelLarge,
                            color = HudCyan
                        )
                        Text(
                            text = if (isBn) "বাংলাদেশের আকাশে উড়ুন" else "Command the Skies of Bengal",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextInstrumentWhite
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onStartFlightSetup(FlightModeType.FREE_FLIGHT) },
                                colors = ButtonDefaults.buttonColors(containerColor = BangladeshEmerald),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("hero_launch_flight_button")
                            ) {
                                Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = OnEmerald)
                                Spacer(Modifier.width(6.dp))
                                Text(BdasStrings.launchFlight(language), color = OnEmerald, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { onStartFlightSetup(FlightModeType.EMERGENCY) },
                                border = BorderStroke(1.dp, CautionCrimson),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("hero_emergency_button")
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = CautionCrimson)
                                Spacer(Modifier.width(4.dp))
                                Text(if (isBn) "জরুরি" else "Emergencies", color = CautionCrimson)
                            }
                        }
                    }
                }
            }

            // Right Command Grid (All 12 Menu Items)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1.65f)
                    .fillMaxHeight()
            ) {
                items(tiles) { tile ->
                    Surface(
                        color = InstrumentSlate,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, InstrumentBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 86.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = tile.onClick)
                            .testTag(tile.tag)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(tile.icon, contentDescription = tile.title, tint = tile.accent, modifier = Modifier.size(20.dp))
                                Text(
                                    text = tile.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextInstrumentWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = tile.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMutedSlate,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
