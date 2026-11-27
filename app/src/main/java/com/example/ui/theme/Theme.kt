package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val BdasDarkColorScheme = darkColorScheme(
    primary = BangladeshEmerald,
    onPrimary = OnEmerald,
    primaryContainer = BangladeshEmeraldDeep,
    onPrimaryContainer = TextInstrumentWhite,
    secondary = HudCyan,
    onSecondary = CockpitVoid,
    secondaryContainer = HudCyanContainer,
    onSecondaryContainer = TextInstrumentWhite,
    tertiary = WarningAmber,
    onTertiary = CockpitVoid,
    tertiaryContainer = WarningAmberContainer,
    onTertiaryContainer = WarningAmber,
    error = CautionCrimson,
    onError = CockpitVoid,
    errorContainer = CautionCrimsonContainer,
    onErrorContainer = CautionCrimson,
    background = CockpitVoid,
    onBackground = TextInstrumentWhite,
    surface = CockpitDeepNavy,
    onSurface = TextInstrumentWhite,
    surfaceVariant = InstrumentSlate,
    onSurfaceVariant = TextMutedSlate,
    surfaceContainerHigh = InstrumentElevated,
    outline = InstrumentBorder
)

private val BdasShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp)
)

@Composable
fun BdasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BdasDarkColorScheme,
        typography = BdasTypography,
        shapes = BdasShapes,
        content = content
    )
}
