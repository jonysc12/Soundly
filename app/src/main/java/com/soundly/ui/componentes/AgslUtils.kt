package com.soundly.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.soundly.cloud.LegacyBlurView

/**
 * Puente para mantener compatibilidad con el código existente.
 * NOTA: El efecto de blur ha sido desactivado globalmente para maximizar el rendimiento.
 */

/**
 * Extensión de Modifier para proporcionar un fondo que simula el cristal esmerilado.
 * OPTIMIZADO: Función pura para evitar el overhead de recomposición de modificadores Composable.
 */
fun Modifier.agslFrostedGlass(
    radius: Float = 15f,
    tint: Color = Color.Unspecified,
    noise: Float = 0.03f
): Modifier = if (tint != Color.Unspecified) this.background(tint) else this

@Deprecated("Usar agslFrostedGlass directamente", ReplaceWith("this.agslFrostedGlass(radius, tint, noise)"))
fun Modifier.agslSelectiveBlur(
    area1Provider: () -> androidx.compose.ui.geometry.Rect,
    area2Provider: () -> androidx.compose.ui.geometry.Rect,
    radius: Float = 15f,
    tint: Color = Color.White.copy(alpha = 0.1f),
    noise: Float = 0.03f
): Modifier = this.agslFrostedGlass(radius, tint, noise)
