package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.core.AppLanguage
import com.example.core.BdasStrings
import com.example.data.onGoogleSignInClicked
import com.example.ui.theme.*

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var lang by remember { mutableStateOf(AppLanguage.ENGLISH) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitVoid)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_bdas_hero_1795772244695),
            contentDescription = stringResource(R.string.cd_hero_banner),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.48f
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            CockpitVoid.copy(alpha = 0.94f),
                            CockpitDeepNavy.copy(alpha = 0.72f),
                            CockpitVoid.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 28.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Column(
                modifier = Modifier.weight(1.2f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = BangladeshEmeraldDeep.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BangladeshEmerald)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = TextInstrumentWhite, modifier = Modifier.size(18.dp))
                        Text("BDAS • BANGLADESH AVIATION", style = MaterialTheme.typography.labelLarge, color = TextInstrumentWhite)
                    }
                }
                Text(
                    text = BdasStrings.title(lang),
                    style = MaterialTheme.typography.displayLarge,
                    color = TextInstrumentWhite
                )
                Text(
                    text = BdasStrings.subtitle(lang),
                    style = MaterialTheme.typography.titleMedium,
                    color = HudCyan
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        lang = if (lang == AppLanguage.ENGLISH) AppLanguage.BANGLA else AppLanguage.ENGLISH
                    },
                    border = BorderStroke(1.dp, InstrumentBorder),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("auth_language_toggle")
                ) {
                    Icon(Icons.Filled.Language, contentDescription = null, tint = HudCyan)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (lang == AppLanguage.ENGLISH) "বাংলায় দেখুন" else "Switch to English",
                        color = TextInstrumentWhite
                    )
                }
            }

            Surface(
                color = InstrumentSlate.copy(alpha = 0.92f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, InstrumentBorder),
                tonalElevation = 6.dp,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = BdasStrings.signInHeading(lang),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextInstrumentWhite
                    )
                    Text(
                        text = BdasStrings.signInDesc(lang),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMutedSlate
                    )
                    if (!errorMsg.isNullOrBlank()) {
                        Surface(
                            color = CautionCrimsonContainer,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CautionCrimson)
                        ) {
                            Text(
                                text = errorMsg!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CautionCrimson,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    Button(
                        onClick = {
                            errorMsg = null
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMsg = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                    errorMsg = "Sign-in cancelled. Tap below to try again."
                                }
                            )
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = BangladeshEmerald),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = OnEmerald,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.sign_in_with_google),
                                style = MaterialTheme.typography.titleMedium,
                                color = OnEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}
