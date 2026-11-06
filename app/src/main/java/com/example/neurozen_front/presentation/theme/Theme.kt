package com.example.neurozen_front.presentation.theme

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

// Original NeuroZen palette (first version of the app).
val Forest900 = Color(0xFF123524)
val Forest700 = Color(0xFF2A6B4B)
val Emerald600 = Color(0xFF4E9F6E)
val Sage500 = Color(0xFF7DBA93)
val Mint300 = Color(0xFFADE8C8)
val Mint100 = Color(0xFFF0FAF3)
val Sand100 = Color(0xFFF7F5EE)
val White = Color(0xFFFFFFFF)

/** Aliases used across the screens, mapped onto the original palette. */
object NzColors {
    val Forest = Forest700
    val ForestLight = Emerald600
    val Mint = Mint300
    val MintSoft = Mint100
    val Beige = Sand100
    val Turquoise = Emerald600
    val Stone = Color(0xFF8A968E)
    val StoneDark = Forest900
    val Night = Color(0xFF0F2219)
    val NightSurface = Color(0xFF173126)

    // Stress levels / statuses
    val Low = Color(0xFF2E7D32)
    val Moderate = Color(0xFFE0A030)
    val High = Color(0xFFC62828)

    // Original auth / onboarding gradients
    val LoginTop = Color(0xFFF1F8E9)
    val LoginBottom = Color(0xFFDCEDC8)
    val OnboardingTop = Color(0xFFE8F5E9)
    val OnboardingMiddle = Color(0xFFC8E6C9)
    val SuccessBg = Color(0xFFE8F5E9)
    val SuccessText = Color(0xFF2E7D32)
}

private val LightColorScheme = lightColorScheme(
    primary = Forest700,
    onPrimary = White,
    primaryContainer = Mint300,
    onPrimaryContainer = Forest900,
    secondary = Emerald600,
    onSecondary = White,
    secondaryContainer = Color(0xFFCFEFDB),
    onSecondaryContainer = Forest900,
    tertiary = Sage500,
    onTertiary = Forest900,
    tertiaryContainer = Color(0xFFE3F1E7),
    onTertiaryContainer = Forest900,
    background = Mint100,
    onBackground = Forest900,
    surface = White,
    onSurface = Forest900,
    surfaceVariant = Sand100,
    onSurfaceVariant = Color(0xFF4F6157),
    outline = Sage500,
    outlineVariant = Color(0xFFD5E6DA)
)

/** "Noche zen": same identity on dark surfaces. Only used when explicitly requested. */
private val DarkColorScheme = darkColorScheme(
    primary = Mint300,
    onPrimary = Forest900,
    primaryContainer = Forest700,
    onPrimaryContainer = Mint100,
    secondary = Sage500,
    onSecondary = Forest900,
    secondaryContainer = Color(0xFF24503A),
    onSecondaryContainer = Mint100,
    tertiary = Sage500,
    background = NzColors.Night,
    onBackground = Mint100,
    surface = NzColors.NightSurface,
    onSurface = Mint100,
    surfaceVariant = Color(0xFF203C2F),
    onSurfaceVariant = Color(0xFFBFD5C7),
    outline = Sage500
)

val NeuroZenTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
)

/** Light by default, exactly like the original app. */
@Composable
fun NeuroZenTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = NeuroZenTypography,
        content = content
    )
}
