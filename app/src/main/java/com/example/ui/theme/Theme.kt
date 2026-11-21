package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AppThemeOption(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val subtitle: String,
    val vibeDescription: String
) {
    MODERN_HORIZON(
        id = "MODERN_HORIZON",
        displayName = "Modern Horizon",
        shortLabel = "Modern",
        subtitle = "Deep Ocean Navy • Sunset Coral • Frosted Bento",
        vibeDescription = "Sleek editorial travel aesthetic with coastal depth, warm coral actions, and frosted slate cards."
    ),
    MINIMALIST_NORDIC(
        id = "MINIMALIST_NORDIC",
        displayName = "Minimalist Nordic",
        shortLabel = "Minimalist",
        subtitle = "Warm Alabaster • Pine Green • Architectural Ink",
        vibeDescription = "Distraction-free Scandinavian daylight interface with crisp white flat cards and 1dp stone borders."
    ),
    HIGH_ENERGY_PULSE(
        id = "HIGH_ENERGY_PULSE",
        displayName = "High-Energy Pulse",
        shortLabel = "High-Energy",
        subtitle = "Midnight Obsidian • Cyber Lime • Electric Magenta",
        vibeDescription = "Bold neo-tropical adventure vibe with kinetic contrast, glowing borders, and neon lime highlights."
    );

    companion object {
        fun fromId(id: String?): AppThemeOption {
            return entries.firstOrNull { it.id == id } ?: MODERN_HORIZON
        }
    }
}

@Immutable
data class TripDesignTokens(
    val themeOption: AppThemeOption,
    val isDark: Boolean,
    val cardCornerRadius: Dp,
    val cardBorderWidth: Dp,
    val cardBorderColor: Color,
    val elevatedCardColor: Color,
    val heroGradientBrush: Brush,
    val accentGradientBrush: Brush,
    val positiveBalanceColor: Color,
    val warningAccentColor: Color,
    val mapBackgroundColor: Color,
    val mapGridLineColor: Color,
    val mapRouteLineColor: Color,
    val mutedTextColor: Color
)

val LocalTripDesignTokens = staticCompositionLocalOf {
    TripDesignTokens(
        themeOption = AppThemeOption.MODERN_HORIZON,
        isDark = true,
        cardCornerRadius = 18.dp,
        cardBorderWidth = 1.dp,
        cardBorderColor = ModernCardBorder,
        elevatedCardColor = ModernElevatedCard,
        heroGradientBrush = Brush.linearGradient(listOf(ModernSlateHarbor, ModernElevatedCard)),
        accentGradientBrush = Brush.horizontalGradient(listOf(ModernSunsetCoral, ModernLagoonCyan)),
        positiveBalanceColor = ModernSuccessMint,
        warningAccentColor = ModernWarningAmber,
        mapBackgroundColor = Color(0xFF0F1B33),
        mapGridLineColor = Color(0xFF1E325C),
        mapRouteLineColor = ModernLagoonCyan,
        mutedTextColor = ModernMutedSlateText
    )
}

private val ModernHorizonColorScheme: ColorScheme = darkColorScheme(
    primary = ModernSunsetCoral,
    onPrimary = Color.White,
    primaryContainer = ModernCoralContainer,
    onPrimaryContainer = Color(0xFFFFDAD2),
    secondary = ModernLagoonCyan,
    onSecondary = Color(0xFF002630),
    secondaryContainer = ModernCyanContainer,
    onSecondaryContainer = Color(0xFFB5F2FF),
    tertiary = ModernSuccessMint,
    onTertiary = Color(0xFF002B1D),
    background = ModernAbyssalNavy,
    onBackground = ModernAlabasterText,
    surface = ModernSlateHarbor,
    onSurface = ModernAlabasterText,
    surfaceVariant = ModernElevatedCard,
    onSurfaceVariant = ModernMutedSlateText,
    outline = ModernCardBorder
)

private val MinimalistNordicColorScheme: ColorScheme = lightColorScheme(
    primary = NordicPineGreen,
    onPrimary = Color.White,
    primaryContainer = NordicPineContainer,
    onPrimaryContainer = Color(0xFF0F261D),
    secondary = NordicTerracottaClay,
    onSecondary = Color.White,
    secondaryContainer = NordicTerracottaContainer,
    onSecondaryContainer = Color(0xFF3E1D0C),
    tertiary = NordicSuccessSage,
    onTertiary = Color.White,
    background = NordicAlabasterPaper,
    onBackground = NordicCharcoalInk,
    surface = NordicPureWhiteSurface,
    onSurface = NordicCharcoalInk,
    surfaceVariant = NordicWarmStoneCard,
    onSurfaceVariant = NordicMutedGraphite,
    outline = NordicHairlineBorder
)

private val HighEnergyPulseColorScheme: ColorScheme = darkColorScheme(
    primary = PulseCyberLime,
    onPrimary = Color(0xFF090A10),
    primaryContainer = PulseLimeContainer,
    onPrimaryContainer = PulseCyberLime,
    secondary = PulseElectricMagenta,
    onSecondary = Color.White,
    secondaryContainer = PulseMagentaContainer,
    onSecondaryContainer = Color(0xFFFFD8E6),
    tertiary = PulseHyperCobalt,
    onTertiary = Color.White,
    background = PulseMidnightObsidian,
    onBackground = PulsePureWhiteText,
    surface = PulseDarkIndigoCard,
    onSurface = PulsePureWhiteText,
    surfaceVariant = PulseElevatedIndigo,
    onSurfaceVariant = PulseNeonSubtext,
    outline = PulseGlowingBorder
)

fun getDesignTokensForTheme(themeOption: AppThemeOption): TripDesignTokens {
    return when (themeOption) {
        AppThemeOption.MODERN_HORIZON -> TripDesignTokens(
            themeOption = AppThemeOption.MODERN_HORIZON,
            isDark = true,
            cardCornerRadius = 18.dp,
            cardBorderWidth = 1.dp,
            cardBorderColor = ModernCardBorder,
            elevatedCardColor = ModernElevatedCard,
            heroGradientBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF1E294B), Color(0xFF153652), Color(0xFF2B213A))
            ),
            accentGradientBrush = Brush.horizontalGradient(
                colors = listOf(ModernSunsetCoral, ModernLagoonCyan)
            ),
            positiveBalanceColor = ModernSuccessMint,
            warningAccentColor = ModernWarningAmber,
            mapBackgroundColor = Color(0xFF0D1930),
            mapGridLineColor = Color(0xFF1B2E52),
            mapRouteLineColor = ModernLagoonCyan,
            mutedTextColor = ModernMutedSlateText
        )

        AppThemeOption.MINIMALIST_NORDIC -> TripDesignTokens(
            themeOption = AppThemeOption.MINIMALIST_NORDIC,
            isDark = false,
            cardCornerRadius = 8.dp,
            cardBorderWidth = 1.dp,
            cardBorderColor = NordicHairlineBorder,
            elevatedCardColor = NordicWarmStoneCard,
            heroGradientBrush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFF2EFE7))
            ),
            accentGradientBrush = Brush.horizontalGradient(
                colors = listOf(NordicPineGreen, NordicTerracottaClay)
            ),
            positiveBalanceColor = NordicSuccessSage,
            warningAccentColor = NordicWarningOchre,
            mapBackgroundColor = Color(0xFFEAE7DC),
            mapGridLineColor = Color(0xFFD5D1C4),
            mapRouteLineColor = NordicPineGreen,
            mutedTextColor = NordicMutedGraphite
        )

        AppThemeOption.HIGH_ENERGY_PULSE -> TripDesignTokens(
            themeOption = AppThemeOption.HIGH_ENERGY_PULSE,
            isDark = true,
            cardCornerRadius = 22.dp,
            cardBorderWidth = 1.5.dp,
            cardBorderColor = PulseGlowingBorder,
            elevatedCardColor = PulseElevatedIndigo,
            heroGradientBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF1E1136), Color(0xFF121C42), Color(0xFF28102B))
            ),
            accentGradientBrush = Brush.horizontalGradient(
                colors = listOf(PulseCyberLime, PulseElectricMagenta)
            ),
            positiveBalanceColor = PulseSuccessNeon,
            warningAccentColor = PulseWarningSolar,
            mapBackgroundColor = Color(0xFF070913),
            mapGridLineColor = Color(0xFF1D2242),
            mapRouteLineColor = PulseCyberLime,
            mutedTextColor = PulseNeonSubtext
        )
    }
}

@Composable
fun TripExpenditureTheme(
    themeOption: AppThemeOption = AppThemeOption.MODERN_HORIZON,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeOption) {
        AppThemeOption.MODERN_HORIZON -> ModernHorizonColorScheme
        AppThemeOption.MINIMALIST_NORDIC -> MinimalistNordicColorScheme
        AppThemeOption.HIGH_ENERGY_PULSE -> HighEnergyPulseColorScheme
    }
    val tokens = getDesignTokensForTheme(themeOption)
    val shapes = Shapes(
        small = RoundedCornerShape(tokens.cardCornerRadius / 2),
        medium = RoundedCornerShape(tokens.cardCornerRadius),
        large = RoundedCornerShape(tokens.cardCornerRadius + 4.dp)
    )

    CompositionLocalProvider(LocalTripDesignTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = shapes,
            content = content
        )
    }
}
