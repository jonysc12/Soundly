package com.soundly.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.soundly.data.repository.ThemeMode

/* -------------------- ESCALA MONOCROMÁTICA -------------------- */

private object SoundlyMono {

    // ☀ LIGHT (versión con contraste real)
    val LightBackground = Color(0xFFFFFFFF)      // Fondo puro
    val LightSurface = Color(0xFFFAFAFA)         // Capa 1 (más clara)
    val LightSurfaceVariant = Color(0xFFD0D0D0)  // Capa 2 (más grisácea)
    val LightOnBackground = Color(0xFF0A0A0A)    // Texto negro profundo
    val LightOnSurfaceVariant = Color(0xFF555555)
    val LightOutline = Color(0xFFFFFFFF)

    // 🌙 DARK (lo dejas igual porque está perfecto)
    val DarkBackground = Color(0xFF0A0A0A)
    val DarkSurface = Color(0xFF141414)
    val DarkSurfaceVariant = Color(0xFF1E1E1E)
    val DarkOnBackground = Color.White
    val DarkOnSurfaceVariant = Color(0xFFAAAAAA)
    val DarkOutline = Color(0xFF2A2A2A)
}
/* -------------------- COLOR SCHEMES -------------------- */

private val SoundlyLightColors = lightColorScheme(
    primary = SoundlyMono.LightOnBackground,
    onPrimary = Color.White,
    primaryContainer = SoundlyMono.LightSurfaceVariant,
    onPrimaryContainer = SoundlyMono.LightOnBackground,

    secondary = SoundlyMono.LightOnSurfaceVariant,
    onSecondary = Color.White,
    secondaryContainer = SoundlyMono.LightSurface,
    onSecondaryContainer = SoundlyMono.LightOnBackground,

    background = SoundlyMono.LightBackground,
    onBackground = SoundlyMono.LightOnBackground,

    surface = SoundlyMono.LightSurface,
    onSurface = SoundlyMono.LightOnBackground,

    surfaceVariant = SoundlyMono.LightSurfaceVariant,
    onSurfaceVariant = SoundlyMono.LightOnSurfaceVariant,

    // 🔥 Jerarquía Monocromática Completa
    surfaceContainerLowest = SoundlyMono.LightBackground,
    surfaceContainerLow = SoundlyMono.LightSurface,
    surfaceContainer = SoundlyMono.LightSurface,
    surfaceContainerHigh = SoundlyMono.LightSurfaceVariant,
    surfaceContainerHighest = SoundlyMono.LightSurfaceVariant,

    outline = SoundlyMono.LightOnSurfaceVariant.copy(alpha = 0.5f),
    outlineVariant = SoundlyMono.LightSurfaceVariant.copy(alpha = 0.5f)
)

private val SoundlyDarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = SoundlyMono.DarkBackground,
    primaryContainer = SoundlyMono.DarkSurfaceVariant,
    onPrimaryContainer = Color.White,

    secondary = SoundlyMono.DarkOnSurfaceVariant,
    onSecondary = Color.Black,
    secondaryContainer = SoundlyMono.DarkSurface,
    onSecondaryContainer = Color.White,

    background = SoundlyMono.DarkBackground,
    onBackground = SoundlyMono.DarkOnBackground,

    surface = SoundlyMono.DarkSurface,
    onSurface = SoundlyMono.DarkOnBackground,

    surfaceVariant = SoundlyMono.DarkSurfaceVariant,
    onSurfaceVariant = SoundlyMono.DarkOnSurfaceVariant,

    // 🔥 Jerarquía Monocromática Completa
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = SoundlyMono.DarkSurface,
    surfaceContainer = SoundlyMono.DarkSurfaceVariant,
    surfaceContainerHigh = SoundlyMono.DarkOutline,
    surfaceContainerHighest = Color(0xFF333333),

    outline = SoundlyMono.DarkOutline,
    outlineVariant = SoundlyMono.DarkOutline
)
/* -------------------- THEME GLOBAL -------------------- */

val LocalIsDarkTheme = staticCompositionLocalOf { false }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SoundlyTheme(
    themeMode: ThemeMode? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = if (themeMode != null) {
        when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    } else {
        darkTheme
    }

    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> SoundlyDarkColors
        else -> SoundlyLightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Removiendo asignación redundante de colores para evitar parpadeos (handled by MainActivity)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        typography = SoundlyTypography,
        content = {
            CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
                content()
            }
        }
    )
}
