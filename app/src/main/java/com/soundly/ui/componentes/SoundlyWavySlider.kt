package com.soundly.ui.componentes

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundlyWavySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    thumbColor: Color = activeColor,
    showThumb: Boolean = true,
    isWave: Boolean = true,
    waveHeight: Dp = 7.dp,
    waveLength: Dp = 32.dp,
    waveThickness: Dp = 4.dp,
    trackThickness: Dp = 4.dp
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        colors = SliderDefaults.colors(
            activeTrackColor = Color.Transparent,
            inactiveTrackColor = Color.Transparent,
            thumbColor = if (showThumb) thumbColor else Color.Transparent
        ),
        thumb = {
            // Siempre declaramos el tamaño del thumb para que el padding sea constante
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showThumb) {
                    SliderDefaults.Thumb(
                        interactionSource = remember { MutableInteractionSource() },
                        colors = SliderDefaults.colors(thumbColor = thumbColor),
                        enabled = enabled,
                        thumbSize = androidx.compose.ui.unit.DpSize(20.dp, 20.dp)
                    )
                }
            }
        },
        track = { sliderState ->
            WavyTrack(
                sliderState = sliderState,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                isWave = isWave,
                waveHeight = waveHeight,
                waveLength = waveLength,
                waveThickness = waveThickness,
                trackThickness = trackThickness
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WavyTrack(
    sliderState: SliderState,
    activeColor: Color,
    inactiveColor: Color,
    isWave: Boolean,
    waveHeight: Dp,
    waveLength: Dp,
    waveThickness: Dp,
    trackThickness: Dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp) 
    ) {
        val width = size.width
        val centerY = size.height / 2
        val waveThickPx = waveThickness.toPx()
        val trackThickPx = trackThickness.toPx()
        
        val fraction = (sliderState.value - sliderState.valueRange.start) / 
                      (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
        val sliderValuePx = width * fraction

        // 1. Inactive Track (Plane)
        drawLine(
            color = inactiveColor,
            start = Offset(sliderValuePx, centerY),
            end = Offset(width, centerY),
            strokeWidth = trackThickPx,
            cap = StrokeCap.Round
        )

        // 2. Active Track (Wavy or Plane)
        if (isWave && waveHeight > 0.dp) {
            val path = Path()
            val waveLenPx = waveLength.toPx()
            val waveHeightPx = waveHeight.toPx()
            val phaseOffset = phase * waveLenPx
            
            path.moveTo(0f, centerY)
            
            // OPTIMIZACIÓN: Paso más grande (4f en lugar de 2f) para reducir cálculos de senos por frame.
            val step = 4f
            var x = 0f
            while (x <= sliderValuePx) {
                val relativeX = x + phaseOffset
                val angle = (relativeX / waveLenPx) * 2 * Math.PI
                val y = centerY + (sin(angle).toFloat() * waveHeightPx / 2)
                path.lineTo(x, y)
                x += step
            }
            path.lineTo(sliderValuePx, centerY)
            
            drawPath(
                path = path,
                color = activeColor,
                style = Stroke(
                    width = waveThickPx, 
                    join = StrokeJoin.Round, 
                    cap = StrokeCap.Round
                )
            )
        } else {
            drawLine(
                color = activeColor,
                start = Offset(0f, centerY),
                end = Offset(sliderValuePx, centerY),
                strokeWidth = waveThickPx,
                cap = StrokeCap.Round
            )
        }
    }
}
