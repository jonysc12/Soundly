package com.soundly.feature.library.utils

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberAlbumColors(uri: Uri?, isDark: Boolean): List<Color> {
    val context = LocalContext.current
    var colors by remember { mutableStateOf(listOf(Color.Transparent, Color.Transparent, Color.Transparent)) }

    LaunchedEffect(uri, isDark) {
        if (uri == null) {
            colors = listOf(Color.Transparent, Color.Transparent, Color.Transparent)
            return@LaunchedEffect
        }
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val req = ImageRequest.Builder(context)
                    .data(uri)
                    .size(128)
                    .allowHardware(false)
                    .build()
                val success = context.imageLoader.execute(req) as? SuccessResult ?: return@runCatching null
                val palette = Palette.Builder(success.drawable.toBitmap())
                    .maximumColorCount(16)
                    .generate()

                val dominant = palette.dominantSwatch?.rgb ?: palette.vibrantSwatch?.rgb ?: 0
                val hsl = FloatArray(3)
                androidx.core.graphics.ColorUtils.colorToHSL(dominant, hsl)

                val base = Color(dominant)

                // Variante clara: Aumentamos luminosidad significativamente
                val lightHsl = hsl.copyOf()
                lightHsl[2] = (lightHsl[2] + 0.35f).coerceAtMost(0.95f)
                lightHsl[1] = (lightHsl[1] * 0.7f) // Un poco más pastel
                val light = Color(androidx.core.graphics.ColorUtils.HSLToColor(lightHsl))

                // Variante saturada: Aumentamos saturación al máximo y ajustamos brillo
                val satHsl = hsl.copyOf()
                satHsl[1] = (satHsl[1] + 0.50f).coerceAtMost(1.0f)
                satHsl[2] = (satHsl[2] * 0.8f).coerceAtLeast(0.4f)
                val sat = Color(androidx.core.graphics.ColorUtils.HSLToColor(satHsl))

                listOf(base, light, sat)
            }.getOrNull()
        }
        result?.let { colors = it }
    }
    return colors
}

@Composable
fun rememberDominantColor(uri: Uri?, isDark: Boolean): Color {
    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(Color.Transparent) }

    LaunchedEffect(uri, isDark) {
        if (uri == null) {
            dominantColor = Color.Transparent
            return@LaunchedEffect
        }
        val color = withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .size(64)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.drawable.toBitmap()
                    val palette = Palette.Builder(bitmap).maximumColorCount(24).generate()
                    val argb = palette.vibrantSwatch?.rgb
                        ?: palette.lightVibrantSwatch?.rgb
                        ?: palette.darkVibrantSwatch?.rgb
                        ?: palette.dominantSwatch?.rgb
                        ?: palette.mutedSwatch?.rgb
                    argb?.let {
                        val hsl = FloatArray(3)
                        androidx.core.graphics.ColorUtils.colorToHSL(it, hsl)

                        if (isDark) {
                            if (hsl[2] > 0.7f) hsl[2] = 0.4f
                        } else {
                            if (hsl[2] < 0.3f) hsl[2] = 0.7f
                        }

                        Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
                    }
                } else null
            } catch (_: Exception) { null }
        }
        if (color != null) dominantColor = color
    }
    return dominantColor
}

@Composable
fun Color.rememberAdaptedForTheme(isDark: Boolean): Color {
    return remember(this, isDark) {
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(this.toArgb(), hsl)
        val lum = hsl[2]

        when {
            // Tema claro: Si es muy oscuro, aclaramos
            !isDark && lum < 0.3f -> {
                hsl[2] = 0.65f
                Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
            }
            // Tema oscuro: Si es muy claro, oscurecemos
            isDark && lum > 0.7f -> {
                hsl[2] = 0.35f
                Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
            }
            else -> this
        }
    }
}

fun getDetailNameColor(dominant: Color, isDark: Boolean): Color {
    if (dominant == Color.Transparent) return if (isDark) Color.White else Color.Black
    val lum = dominant.luminance()
    val grayValue = when {
        lum < 0.30f -> 0.92f
        lum > 0.65f -> 0.12f
        else -> 0.92f - ((lum - 0.30f) / (0.65f - 0.30f)) * (0.92f - 0.12f)
    }
    return Color(grayValue, grayValue, grayValue)
}

@Composable
fun rememberAdaptedAccentColor(color: Color, isDark: Boolean): Color {
    return remember(color, isDark) {
        val lum = color.luminance()
        when {
            isDark && lum < 0.20f -> androidx.compose.ui.graphics.lerp(color, Color.White, 0.35f)
            !isDark && lum > 0.75f -> androidx.compose.ui.graphics.lerp(color, Color.Black, 0.35f)
            else -> color
        }
    }
}
