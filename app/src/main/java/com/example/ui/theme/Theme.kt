package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CozyFarmColorScheme = lightColorScheme(
    primary = FarmLeafGreen,
    onPrimary = Color.White,
    primaryContainer = FarmMint,
    onPrimaryContainer = FarmInk,
    secondary = FarmWood,
    onSecondary = FarmParchment,
    secondaryContainer = FarmCreamGold,
    onSecondaryContainer = FarmWoodDark,
    tertiary = FarmBlushPink,
    onTertiary = FarmInk,
    tertiaryContainer = Color(0xFFFDE2E8),
    onTertiaryContainer = FarmInk,
    background = FarmParchment,
    onBackground = FarmInk,
    surface = FarmParchment,
    onSurface = FarmInk,
    surfaceVariant = FarmParchmentDeep,
    onSurfaceVariant = FarmInkMuted,
    outline = FarmBorder
)

private val CozyShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp)
)

@Composable
fun OurLittleFarmTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CozyFarmColorScheme,
        typography = AppTypography,
        shapes = CozyShapes,
        content = content
    )
}
