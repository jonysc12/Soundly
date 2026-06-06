package com.soundly.ui.componentes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Física — más amortiguada, más suave. Un rebote limpio, no una sacudida.
// ─────────────────────────────────────────────────────────────────────────────
private val SLIDE  = spring<Float>(dampingRatio = 0.72f, stiffness = 340f)
private val BUMP   = spring<Float>(dampingRatio = 0.60f, stiffness = 420f)
private val SETTLE = spring<Float>(dampingRatio = 0.68f, stiffness = 380f)

@Composable
fun PillTabSelector(
    options: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    pillHeight: Dp = 54.dp,
    innerPadding: Dp = 6.5.dp,
) {
    val haptic = LocalHapticFeedback.current
    val count = options.size

    // ── Tema ──────────────────────────────────────────────────────────────────
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val outerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val innerColor = if (isDark) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val activeTextColor = MaterialTheme.colorScheme.onSurface
    val idleTextColor   = MaterialTheme.colorScheme.onSurfaceVariant

    // ── Geometría ─────────────────────────────────────────────────────────────
    var containerWidthPx by remember { mutableIntStateOf(0) }

    val tabWidthPx by remember(containerWidthPx, count) {
        derivedStateOf { if (count > 0) containerWidthPx / count.toFloat() else 0f }
    }
    val maxPillX by remember(containerWidthPx, tabWidthPx) {
        derivedStateOf { (containerWidthPx - tabWidthPx).coerceAtLeast(0f) }
    }

    // ── Animatables — solo lo esencial ────────────────────────────────────────
    // pillX      : posición horizontal de la inner pill
    // pillScale  : escala uniforme para el bump en tap/release (no squash asimétrico)
    val pillX     = remember { Animatable(0f) }
    val pillScale = remember { Animatable(1f) }

    // ── Drag state ────────────────────────────────────────────────────────────
    var isDragging      by remember { mutableStateOf(false) }
    var dragStartPillX  by remember { mutableFloatStateOf(0f) }
    var dragStartTouchX by remember { mutableFloatStateOf(0f) }

    // ── Helpers ───────────────────────────────────────────────────────────────
    fun clampPillX(x: Float) = x.coerceIn(0f, maxPillX)

    fun nearestTab(centerX: Float): Int {
        if (tabWidthPx <= 0f) return selectedIndex
        return (centerX / tabWidthPx - 0.5f).roundToInt().coerceIn(0, count - 1)
    }

    // ── Sync selectedIndex → animación (solo cuando no arrastramos) ───────────
    LaunchedEffect(selectedIndex, tabWidthPx) {
        if (!isDragging && tabWidthPx > 0f) {
            val target = (selectedIndex * tabWidthPx).coerceIn(0f, maxPillX)
            launch { pillX.animateTo(target, SLIDE) }
            // Bump suave: se infla levemente y regresa
            launch {
                pillScale.animateTo(1.06f, BUMP)
                pillScale.animateTo(1f, SETTLE)
            }
        }
    }

    // ── Outer Pill ────────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .height(pillHeight)
            .clip(CircleShape)
            .background(outerColor)
            .padding(innerPadding)
            .onSizeChanged { size ->
                containerWidthPx = size.width
                // pillX se corregirá vía LaunchedEffect en el siguiente frame
            }
            .pointerInput(count) {
                coroutineScope {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging      = true
                            dragStartTouchX = offset.x
                            dragStartPillX  = pillX.value

                            // Detiene cualquier animación en vuelo para que el dedo
                            // tenga control inmediato
                            launch { pillX.stop() }
                            launch { pillScale.snapTo(1f) }
                        },

                        onDrag = { change, _ ->
                            change.consume()

                            val totalDelta = change.position.x - dragStartTouchX
                            val newPillX   = clampPillX(dragStartPillX + totalDelta)
                            launch { pillX.snapTo(newPillX) }

                            // Ligero estiramiento proporcional al desplazamiento respecto
                            // al tab de origen — mucho más contenido que antes
                            val restX   = selectedIndex * tabWidthPx
                            val stretch = if (tabWidthPx > 0f)
                                abs(newPillX - restX) / tabWidthPx else 0f
                            // Máximo ±4 % de escala: presente pero discreto
                            launch { pillScale.snapTo(1f + (stretch * 0.04f).coerceAtMost(0.04f)) }
                        },

                        onDragEnd = {
                            isDragging = false

                            val pillCenter = pillX.value + tabWidthPx / 2f
                            val finalTab   = nearestTab(pillCenter)
                            val snapTarget = clampPillX(finalTab * tabWidthPx)

                            launch { pillX.animateTo(snapTarget, SLIDE) }
                            launch {
                                pillScale.animateTo(1.06f, BUMP)
                                pillScale.animateTo(1f, SETTLE)
                            }

                            onTabSelected(finalTab)
                        },

                        onDragCancel = {
                            isDragging = false
                            val restTarget = clampPillX(selectedIndex * tabWidthPx)
                            launch { pillX.animateTo(restTarget, SLIDE) }
                            launch { pillScale.animateTo(1f, SETTLE) }
                        }
                    )
                }
            }
    ) {
        // ── Inner Pill ────────────────────────────────────────────────────────
        val density = LocalDensity.current
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX    = pillX.value
                    scaleX          = pillScale.value
                    scaleY          = pillScale.value
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
                .width(with(density) { tabWidthPx.toDp() })
                .fillMaxHeight()
                .clip(CircleShape)
                .background(innerColor)
        )

        // ── Labels ────────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, label ->
                val tabCenterX  = (index + 0.5f) * tabWidthPx
                val pillCenterX = pillX.value + tabWidthPx / 2f
                val proximity   = if (tabWidthPx > 0f)
                    (1f - abs(pillCenterX - tabCenterX) / tabWidthPx).coerceIn(0f, 1f)
                else
                    if (index == selectedIndex) 1f else 0f

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            alpha = 0.50f + proximity * 0.50f
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                        ) {
                            if (!isDragging) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(index)
                            }
                        }
                ) {
                    Text(
                        text       = label,
                        color      = lerp(idleTextColor, activeTextColor, proximity),
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = if (proximity > 0.5f) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
