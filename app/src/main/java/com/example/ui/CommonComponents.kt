package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.data.PilotProfile
import com.example.missions.PilotRank
import com.example.ui.theme.*

@Composable
fun BdasTopBar(
    title: String,
    language: AppLanguage,
    profile: PilotProfile?,
    onBack: (() -> Unit)?,
    onToggleLanguage: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = InstrumentSlate.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, InstrumentBorder),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("top_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextInstrumentWhite
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextInstrumentWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (profile != null) {
                val rank = PilotRank.forXp(profile.xp)
                StatChip(
                    icon = { Icon(Icons.Filled.Stars, contentDescription = null, tint = HudCyan, modifier = Modifier.size(16.dp)) },
                    label = rank.title(language)
                )
                StatChip(
                    icon = { Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp)) },
                    label = "${profile.bdasCoins}"
                )
                StatChip(
                    icon = { Icon(Icons.Filled.CloudDone, contentDescription = null, tint = BangladeshEmerald, modifier = Modifier.size(16.dp)) },
                    label = "${profile.airlineCredits} CR"
                )
            }
            OutlinedButton(
                onClick = onToggleLanguage,
                border = BorderStroke(1.dp, BangladeshEmerald),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("language_toggle_button")
            ) {
                Icon(Icons.Filled.Language, contentDescription = "Toggle Language", tint = BangladeshEmerald, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (language == AppLanguage.ENGLISH) "বাংলা" else "EN",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextInstrumentWhite
                )
            }
            actions()
        }
    }
}

@Composable
fun StatChip(
    icon: @Composable () -> Unit,
    label: String,
    containerColor: Color = CockpitDeepNavy
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, InstrumentBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = TextInstrumentWhite,
                maxLines = 1
            )
        }
    }
}

@Composable
fun BannerAlert(message: String?, onDismiss: () -> Unit) {
    if (message.isNullOrBlank()) return
    Surface(
        color = BangladeshEmeraldDeep,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BangladeshEmerald),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = TextInstrumentWhite,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_banner_button")) {
                Text("OK", color = WarningAmber)
            }
        }
    }
}
