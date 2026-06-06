package com.soundly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/* -------------------- ESCALA MONOCROMÁTICA -------------------- */

private object SoundlyMono {

    // ☀ LIGHT (versión con contraste real)
    val LightBackground = Color(0xFFFFFFFF)      // Fondo puro
    val LightSurface = Color(0xFFF9F9F9)         // Capa 1 (más clara)
    val LightSurfaceVariant = Color(0xFFD0D0D0)  // Capa 2 (más grisácea)
    val LightOnBackground = Color(0xFF0A0A0A)    // Texto negro profundo
    val LightOnSurfaceVariant = Color(0xFF555555)
    val LightOutline = Color(0xFFCCCCCC)

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

    background = SoundlyMono.LightBackground,
    onBackground = SoundlyMono.LightOnBackground,

    // 🔥 Jerarquía correcta
    surface = SoundlyMono.LightSurface,
    onSurface = SoundlyMono.LightOnBackground,

    surfaceVariant = SoundlyMono.LightSurfaceVariant,
    onSurfaceVariant = SoundlyMono.LightOnSurfaceVariant,

    outline = SoundlyMono.LightOutline
)

private val SoundlyDarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = SoundlyMono.DarkBackground,

    background = SoundlyMono.DarkBackground,
    onBackground = SoundlyMono.DarkOnBackground,

    surface = SoundlyMono.DarkSurface,
    onSurface = SoundlyMono.DarkOnBackground,

    surfaceVariant = SoundlyMono.DarkSurfaceVariant,
    onSurfaceVariant = SoundlyMono.DarkOnSurfaceVariant,

    outline = SoundlyMono.DarkOutline
)
/* -------------------- THEME GLOBAL -------------------- */

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SoundlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        SoundlyDarkColors
    } else {
        SoundlyLightColors
    }

    MaterialTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        typography = SoundlyTypography,
        content = content
    )
}
