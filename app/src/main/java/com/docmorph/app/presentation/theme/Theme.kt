package com.docmorph.app.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ─── Brand Colours ────────────────────────────────────────────────────────────

val DocMorphBlue        = Color(0xFF1565C0)
val DocMorphBlueDark    = Color(0xFF0D47A1)
val DocMorphAccent      = Color(0xFF42A5F5)
val DocMorphSurface     = Color(0xFFF8F9FA)
val DocMorphOnSurface   = Color(0xFF1A1A2E)

val DocMorphBlueDarkTheme   = Color(0xFF90CAF9)
val DocMorphSurfaceDark     = Color(0xFF121212)
val DocMorphOnSurfaceDark   = Color(0xFFE0E0E0)

// ─── Colour Schemes ──────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary          = DocMorphBlue,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    secondary        = DocMorphAccent,
    onSecondary      = Color.White,
    background       = DocMorphSurface,
    onBackground     = DocMorphOnSurface,
    surface          = Color.White,
    onSurface        = DocMorphOnSurface,
    surfaceVariant   = Color(0xFFEEF2FF),
    outline          = Color(0xFFBDBDBD)
)

private val DarkColorScheme = darkColorScheme(
    primary          = DocMorphBlueDarkTheme,
    onPrimary        = Color(0xFF003A75),
    primaryContainer = DocMorphBlueDark,
    secondary        = DocMorphAccent,
    onSecondary      = Color.Black,
    background       = DocMorphSurfaceDark,
    onBackground     = DocMorphOnSurfaceDark,
    surface          = Color(0xFF1E1E1E),
    onSurface        = DocMorphOnSurfaceDark,
    surfaceVariant   = Color(0xFF2C2C3E),
    outline          = Color(0xFF616161)
)

// ─── Typography ──────────────────────────────────────────────────────────────

val DocMorphTypography = Typography(
    headlineLarge   = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,   fontSize = 28.sp),
    headlineMedium  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge      = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,  fontSize = 16.sp),
    bodyLarge       = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 16.sp),
    bodyMedium      = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 14.sp),
    labelSmall      = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,  fontSize = 11.sp)
)

// ─── Theme Composable ────────────────────────────────────────────────────────

@Composable
fun DocMorphTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,           // Material You on Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else           dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    // Apply status bar colour to match the app bar
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = DocMorphTypography,
        content     = content
    )
}
