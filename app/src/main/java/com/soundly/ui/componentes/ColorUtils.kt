package com.soundly.ui.componentes

import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extrae un color dominante de una carátula usando Palette y lo mantiene memorizado.
 * Si no hay carátula disponible, devuelve [Color.Transparent].
 */
@Composable
fun rememberDominantColor(uri: Uri?): Color {
    val context = LocalContext.current
    var dominantColor by rememberSaveable { mutableStateOf(Color.Transparent.toArgb()) }

    LaunchedEffect(uri) {
        if (uri == null) {
            dominantColor = Color.Transparent.toArgb()
            return@LaunchedEffect
        }
        // Evitar reprocesar si ya tenemos un color válido para esta URI
        // (Aunque para ser perfectos necesitaríamos guardar la URI también, 
        // pero rememberSaveable ya ayuda en el expand/collapse del mismo item)
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
                    val palette = Palette.Builder(bitmap).maximumColorCount(8).generate()
                    val argb = palette.vibrantSwatch?.rgb
                        ?: palette.dominantSwatch?.rgb
                        ?: palette.mutedSwatch?.rgb
                    argb?.let { Color(it) }
                } else null
            } catch (_: Exception) {
                null
            }
        }
        if (color != null) dominantColor = color.toArgb()
    }

    return Color(dominantColor)
}

/**
 * Ajusta el color dominante para que sea legible según tema y devuelve una
 * versión animada para transiciones suaves.
 */
@Composable
fun rememberAdaptedDominant(
    rawColor: Color,
    isDarkTheme: Boolean,
    fallback: Color,
    isVivid: Boolean = false
): Color {
    val adjusted = remember(rawColor, isDarkTheme, isVivid) {
        if (rawColor == Color.Transparent) fallback else rawColor.adaptForTheme(isDarkTheme, isVivid)
    }
    val spring = spring<Color>(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)
    val animated by animateColorAsState(targetValue = adjusted, animationSpec = spring, label = "dominantAnimated")
    return animated
}

fun blendOnSurface(accent: Color, base: Color, fraction: Float): Color =
    lerp(base, accent, fraction)

private val SPRING_COLOR_SLOW = spring<Color>(stiffness = Spring.StiffnessVeryLow)

@Composable
fun rememberAnimatedDominant(
    rawColor: Color,
    isDarkTheme: Boolean,
    fallback: Color,
    isVivid: Boolean = false
): Color {
    val adapted = remember(rawColor, isDarkTheme, fallback, isVivid) {
        val base = if (rawColor == Color.Transparent) fallback else rawColor
        base.adaptForTheme(isDarkTheme, isVivid)
    }
    val animated by animateColorAsState(
        targetValue = adapted,
        animationSpec = SPRING_COLOR_SLOW,
        label = "animatedDominant"
    )
    return if (animated == Color.Transparent) fallback else animated
}

fun adaptDominantInstant(
    rawColor: Color,
    isDarkTheme: Boolean,
    fallback: Color,
    isVivid: Boolean = false
): Color {
    val base = if (rawColor == Color.Transparent) fallback else rawColor
    return base.adaptForTheme(isDarkTheme, isVivid)
}

private fun Color.adaptForTheme(isDark: Boolean, isVivid: Boolean = false): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    
    if (isVivid) {
        if (isDark) {
            // MODO OSCURO VIVO:
            // Saturación alta para que resalte contra el negro
            hsv[1] = (hsv[1] * 1.5f).coerceIn(0.25f, 1f)
            // Brillo controlado
            if (hsv[2] < 0.40f) hsv[2] = 0.45f
            if (hsv[2] > 0.80f) hsv[2] = 0.70f
        } else {
            // MODO CLARO VIVO:
            // Forzamos saturación muy alta para que los colores sean eléctricos
            hsv[1] = (hsv[1] * 1.8f).coerceIn(0.60f, 1f)
            
            // Mantenemos el brillo en un rango medio-bajo para que cuando se use
            // como texto sobre fondo claro, o como acentos, tenga fuerza.
            hsv[2] = hsv[2].coerceIn(0.35f, 0.65f)
        }
        
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    val lum = luminance()
    return when {
        isDark && lum > 0.78f -> lerp(this, Color.Black, 0.32f)
        !isDark && lum < 0.18f -> lerp(this, Color.White, 0.32f)
        else -> this
    }
}
