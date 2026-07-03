package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LightPrimaryVal,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainerVal,
    onPrimaryContainer = LightPrimaryVal,
    secondary = LightSecondaryVal,
    onSecondary = Color.White,
    secondaryContainer = LightSecondaryContainerVal,
    onSecondaryContainer = LightSecondaryVal,
    tertiary = Color(0xFFE11D48),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFECEF),
    onTertiaryContainer = Color(0xFF9F1239),
    background = LightBackgroundVal,
    onBackground = Color.Black,
    surface = LightSurfaceVal,
    onSurface = Color.Black,
    surfaceVariant = LightSurfaceContainerLowVal,
    onSurfaceVariant = LightOnSurfaceVariantVal,
    outline = LightOutlineVal,
    outlineVariant = LightOutlineVariantVal,
    error = BrandError,
    onError = BrandOnError,
    errorContainer = BrandErrorContainer,
    onErrorContainer = BrandOnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = StitchPrimary,
    onPrimary = Color.Black,
    primaryContainer = StitchPrimaryContainer,
    onPrimaryContainer = StitchPrimary,
    secondary = StitchSecondary,
    onSecondary = Color.Black,
    secondaryContainer = StitchSecondaryContainer,
    onSecondaryContainer = StitchSecondary,
    tertiary = Color(0xFF670007),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFF635A),
    onTertiaryContainer = Color(0xFF400003),
    background = StitchBackground,
    onBackground = Color.White,
    surface = StitchSurface,
    onSurface = Color.White,
    surfaceVariant = StitchSurfaceContainerHighest,
    onSurfaceVariant = StitchOnSurfaceVariant,
    outline = StitchOutline,
    outlineVariant = StitchOutlineVariant,
    error = BrandError,
    onError = BrandOnError,
    errorContainer = BrandErrorContainer,
    onErrorContainer = BrandOnErrorContainer
)

@Composable
fun KineticTrustTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    isDarkThemeActive = darkTheme
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
