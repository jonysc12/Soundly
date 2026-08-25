package com.soundly.cloud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Composable que envuelve la librería BlurView para proporcionar backdrop blur
 * compatible con versiones anteriores de Android y seguro para Compose.
 * 
 * NOTA: El efecto de blur ha sido desactivado globalmente a petición del usuario.
 */
@Composable
fun LegacyBlurView(
    modifier: Modifier = Modifier,
    blurRadius: Float = 15f,
    tintColor: Color = Color.Transparent,
    content: @Composable () -> Unit = {}
) {
    Box(modifier = modifier.background(tintColor)) {
        content()
    }
}
