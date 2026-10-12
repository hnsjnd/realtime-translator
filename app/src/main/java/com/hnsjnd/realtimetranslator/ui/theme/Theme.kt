package com.hnsjnd.realtimetranslator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Deep navy / charcoal + soft teal accent
val Navy950 = Color(0xFF0B1220)
val Navy900 = Color(0xFF111827)
val Navy850 = Color(0xFF151E32)
val Navy800 = Color(0xFF1C2740)
val Navy700 = Color(0xFF243049)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)
val Slate200 = Color(0xFFE2E8F0)
val Teal400 = Color(0xFF2DD4BF)
val Teal500 = Color(0xFF14B8A6)
val Teal600 = Color(0xFF0D9488)
val Violet400 = Color(0xFFA78BFA)
val Rose400 = Color(0xFFFB7185)
val Amber300 = Color(0xFFFCD34D)
private val SoftWhite = Color(0xFFF8FAFC)

private val DarkColors = darkColorScheme(
    primary = Teal400,
    onPrimary = Navy950,
    primaryContainer = Color(0xFF0F3D3A),
    onPrimaryContainer = Teal400,
    secondary = Violet400,
    onSecondary = Navy950,
    secondaryContainer = Color(0xFF2E2260),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = Amber300,
    background = Navy950,
    onBackground = Slate200,
    surface = Navy900,
    onSurface = SoftWhite,
    surfaceVariant = Navy800,
    onSurfaceVariant = Slate400,
    outline = Color(0xFF3A4660),
    outlineVariant = Color(0xFF2A3548),
    error = Rose400,
    onError = Navy950,
    errorContainer = Color(0xFF4C1D24),
    onErrorContainer = Color(0xFFFFCDD2)
)

// Keep a light scheme for rare system-light preference, still refined
private val LightColors = lightColorScheme(
    primary = Teal600,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    background = Color(0xFFF1F5F9),
    onBackground = Navy900,
    surface = Color.White,
    onSurface = Navy900,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    error = Color(0xFFE11D48)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp
    )
)

@Composable
fun RealtimeTranslatorTheme(
    darkTheme: Boolean = true, // dark-first premium default
    content: @Composable () -> Unit
) {
    // Still honor explicit light if system is light AND we pass false; default stays dark
    val scheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content
    )
}
