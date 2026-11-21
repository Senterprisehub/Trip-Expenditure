package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.LocalTripDesignTokens
import com.example.ui.theme.ModernAbyssalNavy
import com.example.ui.theme.ModernLagoonCyan
import com.example.ui.theme.ModernSlateHarbor
import com.example.ui.theme.ModernSunsetCoral
import com.example.ui.theme.NordicAlabasterPaper
import com.example.ui.theme.NordicCharcoalInk
import com.example.ui.theme.NordicPineGreen
import com.example.ui.theme.NordicPureWhiteSurface
import com.example.ui.theme.NordicTerracottaClay
import com.example.ui.theme.PulseCyberLime
import com.example.ui.theme.PulseDarkIndigoCard
import com.example.ui.theme.PulseElectricMagenta
import com.example.ui.theme.PulseMidnightObsidian

@Composable
fun LiveThemeSwitcherBar(
    activeTheme: AppThemeOption,
    onSelectTheme: (AppThemeOption) -> Unit,
    onOpenShowcase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalTripDesignTokens.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_theme_switcher_bar"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(tokens.cardCornerRadius),
        border = BorderStroke(tokens.cardBorderWidth, tokens.cardBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Theme Studio",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE THEME STUDIO",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Visual Preview Cards →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenShowcase() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("open_theme_showcase_button")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppThemeOption.entries.forEach { option ->
                    val isSelected = option == activeTheme
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        },
                        label = "themePillBg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        label = "themePillFg"
                    )

                    val icon = when (option) {
                        AppThemeOption.MODERN_HORIZON -> Icons.Default.WaterDrop
                        AppThemeOption.MINIMALIST_NORDIC -> Icons.Default.Eco
                        AppThemeOption.HIGH_ENERGY_PULSE -> Icons.Default.Bolt
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(tokens.cardCornerRadius / 1.5f))
                            .background(bgColor)
                            .border(
                                width = if (isSelected) 0.dp else 1.dp,
                                color = tokens.cardBorderColor,
                                shape = RoundedCornerShape(tokens.cardCornerRadius / 1.5f)
                            )
                            .clickable { onSelectTheme(option) }
                            .padding(horizontal = 8.dp)
                            .testTag("theme_pill_${option.id.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = option.displayName,
                                tint = contentColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = option.shortLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeVisualShowcaseBottomSheet(
    activeTheme: AppThemeOption,
    onSelectTheme: (AppThemeOption) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Choose Your Dashboard & Trip Theme",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap any visual preview card below to immediately style Trip Expenditure with that palette, typography, and surface geometry.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Option A Visual Mockup Card
            VisualThemePreviewCard(
                option = AppThemeOption.MODERN_HORIZON,
                isSelected = activeTheme == AppThemeOption.MODERN_HORIZON,
                bgColor = ModernAbyssalNavy,
                cardColor = ModernSlateHarbor,
                primaryColor = ModernSunsetCoral,
                secondaryColor = ModernLagoonCyan,
                textColor = Color.White,
                subtextColor = Color(0xFF94A3B8),
                cornerRadiusDp = 18,
                onSelect = {
                    onSelectTheme(AppThemeOption.MODERN_HORIZON)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Option B Visual Mockup Card
            VisualThemePreviewCard(
                option = AppThemeOption.MINIMALIST_NORDIC,
                isSelected = activeTheme == AppThemeOption.MINIMALIST_NORDIC,
                bgColor = NordicAlabasterPaper,
                cardColor = NordicPureWhiteSurface,
                primaryColor = NordicPineGreen,
                secondaryColor = NordicTerracottaClay,
                textColor = NordicCharcoalInk,
                subtextColor = Color(0xFF5C5E58),
                cornerRadiusDp = 8,
                onSelect = {
                    onSelectTheme(AppThemeOption.MINIMALIST_NORDIC)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Option C Visual Mockup Card
            VisualThemePreviewCard(
                option = AppThemeOption.HIGH_ENERGY_PULSE,
                isSelected = activeTheme == AppThemeOption.HIGH_ENERGY_PULSE,
                bgColor = PulseMidnightObsidian,
                cardColor = PulseDarkIndigoCard,
                primaryColor = PulseCyberLime,
                secondaryColor = PulseElectricMagenta,
                textColor = Color.White,
                subtextColor = Color(0xFFA5B4FC),
                cornerRadiusDp = 22,
                onSelect = {
                    onSelectTheme(AppThemeOption.HIGH_ENERGY_PULSE)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("close_theme_showcase_button")
            ) {
                Text("Done Reviewing Themes")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VisualThemePreviewCard(
    option: AppThemeOption,
    isSelected: Boolean,
    bgColor: Color,
    cardColor: Color,
    primaryColor: Color,
    secondaryColor: Color,
    textColor: Color,
    subtextColor: Color,
    cornerRadiusDp: Int,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("visual_preview_card_${option.id.lowercase()}"),
        shape = RoundedCornerShape(cornerRadiusDp.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) primaryColor else subtextColor.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active Theme",
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = option.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = subtextColor
                    )
                }

                // Swatch dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SwatchCircle(color = bgColor, borderColor = subtextColor)
                    SwatchCircle(color = cardColor, borderColor = subtextColor)
                    SwatchCircle(color = primaryColor, borderColor = primaryColor)
                    SwatchCircle(color = secondaryColor, borderColor = secondaryColor)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = option.vibeDescription,
                style = MaterialTheme.typography.bodySmall,
                color = subtextColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Miniature Live UI Mockup Inside the Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape((cornerRadiusDp - 4).coerceAtLeast(4).dp))
                    .background(cardColor)
                    .border(
                        width = 1.dp,
                        color = subtextColor.copy(alpha = 0.25f),
                        shape = RoundedCornerShape((cornerRadiusDp - 4).coerceAtLeast(4).dp)
                    )
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "KYOTO & OSAKA EXPEDITION",
                                style = MaterialTheme.typography.labelMedium,
                                color = secondaryColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$2,450.00 / $4,000 Budget",
                                style = MaterialTheme.typography.titleMedium,
                                color = textColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(primaryColor)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "+$142.50 Owed",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (option == AppThemeOption.HIGH_ENERGY_PULSE) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Simulated progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(subtextColor.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.62f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(primaryColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "09:30 AM • Fushimi Inari Shrine",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor
                        )
                        Text(
                            text = "▲ 4 Votes • Offline Ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = if (option == AppThemeOption.HIGH_ENERGY_PULSE) Color.Black else Color.White
                ),
                shape = RoundedCornerShape((cornerRadiusDp / 2).coerceAtLeast(6).dp)
            ) {
                Text(
                    text = if (isSelected) "Currently Active Theme" else "Apply ${option.displayName}",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SwatchCircle(color: Color, borderColor: Color) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, borderColor.copy(alpha = 0.6f), CircleShape)
    )
}
