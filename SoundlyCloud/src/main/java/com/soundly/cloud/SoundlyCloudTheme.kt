package com.soundly.cloud

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val SoundlyDarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color(0xFF0A0A0A),
    background = Color(0xFF0A0A0A),
    onBackground = Color.White,
    surface = Color(0xFF141414),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFAAAAAA),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerLow = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    outline = Color(0xFF2A2A2A),
    outlineVariant = Color(0xFF2A2A2A)
)

private val SoundlyLightColors = lightColorScheme(
    primary = Color(0xFF0A0A0A),
    onPrimary = Color.White,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0A0A0A),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFFD0D0D0),
    onSurfaceVariant = Color(0xFF555555),
    surfaceContainer = Color(0xFFFAFAFA),
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainerHigh = Color(0xFFD0D0D0),
    outline = Color(0xFFFFFFFF),
    outlineVariant = Color(0xFFD0D0D0).copy(alpha = 0.5f)
)

private val SoundlyTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 57.sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 45.sp),
    displaySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 36.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 32.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 28.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp)
)

@Composable
fun SoundlyCloudTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> SoundlyDarkColors
        else -> SoundlyLightColors
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = SoundlyTypography,
        content = content
    )
}
