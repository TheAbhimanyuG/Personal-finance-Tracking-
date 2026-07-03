package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

// Global state tracking active theme configuration (updated by Theme provider)
var isDarkThemeActive = true
var isCompactViewActive by mutableStateOf(false)

// Define static Stitch Dark Color values (Softer, luxurious slate and emerald mint)
val StitchPrimary = Color(0xFF5EEAD4)          // Soft Emerald Mint Green
val StitchPrimaryContainer = Color(0xFF1E293B) // Premium Deep Slate Container
val StitchSecondary = Color(0xFF34D399)        // Elegant Lighter Emerald Accent
val StitchSecondaryContainer = Color(0xFF334155)
val StitchBackground = Color(0xFF0F172A)        // Luxurious Dark Slate Blue
val StitchSurface = Color(0xFF1E293B)          // Tactile Slate Card Surface
val StitchOnSurfaceVariant = Color(0xFF94A3B8)  // Soft Cool Slate Text
val StitchSurfaceContainerLowest = Color(0xFF0F172A)
val StitchSurfaceContainerLow = Color(0xFF1E293B)
val StitchSurfaceContainer = Color(0xFF334155)
val StitchSurfaceContainerHigh = Color(0xFF475569)
val StitchSurfaceContainerHighest = Color(0xFF64748B)
val StitchOutline = Color(0xFF334155)          // Smooth Tactile Outline
val StitchOutlineVariant = Color(0xFF475569)

// Define static Light Theme values (Lighter, soft airy pastel blues and whites)
val LightPrimaryVal = Color(0xFF60A5FA)        // Smooth Pastel Blue Accent
val LightPrimaryContainerVal = Color(0xFFEFF6FF)
val LightSecondaryVal = Color(0xFF93C5FD)        // Lighter Soft Blue Secondary
val LightSecondaryContainerVal = Color(0xFFF8FAFC)
val LightBackgroundVal = Color(0xFFFAFBFE)      // Airy Soft White Canvas
val LightSurfaceVal = Color(0xFFFFFFFF)         // Clean Surface White
val LightOnSurfaceVariantVal = Color(0xFF647488) // Gentle Cool Gray Text
val LightSurfaceContainerLowestVal = Color(0xFFFFFFFF)
val LightSurfaceContainerLowVal = Color(0xFFF1F5F9)
val LightSurfaceContainerVal = Color(0xFFE2E8F0)
val LightSurfaceContainerHighVal = Color(0xFFCBD5E1)
val LightSurfaceContainerHighestVal = Color(0xFF94A3B8)
val LightOutlineVal = Color(0xFFE2E8F0)         // Soft Card Border Outline
val LightOutlineVariantVal = Color(0xFFF1F5F9)

// Dynamic brand color evaluation mapping (using standard custom getters)
val BrandPrimary: Color
    get() = if (isDarkThemeActive) StitchPrimary else LightPrimaryVal

val BrandPrimaryContainer: Color
    get() = if (isDarkThemeActive) StitchPrimaryContainer else LightPrimaryContainerVal

val BrandOnPrimaryContainer: Color
    get() = if (isDarkThemeActive) StitchPrimary else LightPrimaryVal

val BrandSecondary: Color
    get() = if (isDarkThemeActive) StitchSecondary else LightSecondaryVal

val BrandSecondaryContainer: Color
    get() = if (isDarkThemeActive) StitchSecondaryContainer else LightSecondaryContainerVal

val BrandOnSecondaryContainer: Color
    get() = if (isDarkThemeActive) Color(0xFF8FFFBD) else LightSecondaryVal

val BrandBackground: Color
    get() = if (isDarkThemeActive) StitchBackground else LightBackgroundVal

val BrandOnBackground: Color
    get() = if (isDarkThemeActive) Color.White else Color.Black

val BrandSurface: Color
    get() = if (isDarkThemeActive) StitchSurface else LightSurfaceVal

val BrandOnSurface: Color
    get() = if (isDarkThemeActive) Color.White else Color.Black

val BrandOnSurfaceVariant: Color
    get() = if (isDarkThemeActive) StitchOnSurfaceVariant else LightOnSurfaceVariantVal

val BrandSurfaceContainerLowest: Color
    get() = if (isDarkThemeActive) StitchSurfaceContainerLowest else LightSurfaceContainerLowestVal

val BrandSurfaceContainerLow: Color
    get() = if (isDarkThemeActive) StitchSurfaceContainerLow else LightSurfaceContainerLowVal

val BrandSurfaceContainer: Color
    get() = if (isDarkThemeActive) StitchSurfaceContainer else LightSurfaceContainerVal

val BrandSurfaceContainerHigh: Color
    get() = if (isDarkThemeActive) StitchSurfaceContainerHigh else LightSurfaceContainerHighVal

val BrandSurfaceContainerHighest: Color
    get() = if (isDarkThemeActive) StitchSurfaceContainerHighest else LightSurfaceContainerHighestVal

val BrandOutline: Color
    get() = if (isDarkThemeActive) StitchOutline else LightOutlineVal

val BrandOutlineVariant: Color
    get() = if (isDarkThemeActive) StitchOutlineVariant else LightOutlineVariantVal

val BrandOnPrimary: Color
    get() = if (isDarkThemeActive) Color.Black else Color.White

val BrandOnSecondary: Color
    get() = if (isDarkThemeActive) Color.Black else Color.White

// Standard custom tertiary / errors
val BrandTertiary: Color
    get() = if (isDarkThemeActive) Color(0xFF670007) else Color(0xFFE11D48)

val BrandTertiaryContainer: Color
    get() = if (isDarkThemeActive) Color(0xFFFF635A) else Color(0xFFFFECEF)

val BrandOnTertiaryContainer: Color
    get() = if (isDarkThemeActive) Color(0xFF400003) else Color(0xFF9F1239)

val BrandError = Color(0xFFFE3B30)            // High-precision Alert Red
val BrandOnError = Color(0xFFFFFFFF)
val BrandErrorContainer = Color(0xFF2F1416)
val BrandOnErrorContainer = Color(0xFFFE3B30)



