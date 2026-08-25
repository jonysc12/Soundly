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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
    useBlur: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val count = options.size

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f
    
    // Restauramos la transparencia cristalina original
    val outerColor = remember(isDark, colorScheme) {
        colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val innerColor = remember(isDark, colorScheme) {
        if (isDark) {
            colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
        } else {
            colorScheme.surface.copy(alpha = 0.7f)
        }
    }
    val activeTextColor = colorScheme.onSurface
    val idleTextColor   = colorScheme.onSurfaceVariant

    var containerWidthPx by remember { mutableIntStateOf(0) }
    
    // tabWidthPx como derivedStateOf ayuda a que los componentes dependientes no se invaliden 
    // a menos que el resultado final cambie.
    val tabWidthPx by remember(containerWidthPx, count) {
        derivedStateOf { if (count > 0) containerWidthPx / count.toFloat() else 0f }
    }
    val maxPillX by remember(containerWidthPx, tabWidthPx) {
        derivedStateOf { (containerWidthPx - tabWidthPx).coerceAtLeast(0f) }
    }

    val pillX     = remember { Animatable(0f) }
    val pillScale = remember { Animatable(1f) }

    var isDragging      by remember { mutableStateOf(false) }
    var dragStartPillX  by remember { mutableFloatStateOf(0f) }
    var dragStartTouchX by remember { mutableFloatStateOf(0f) }
    var lastNearestTab  by remember { mutableIntStateOf(selectedIndex) }

    fun clampPillX(x: Float) = x.coerceIn(0f, maxPillX)
    fun nearestTab(centerX: Float): Int {
        if (tabWidthPx <= 0f) return selectedIndex
        return (centerX / tabWidthPx - 0.5f).roundToInt().coerceIn(0, count - 1)
    }

    LaunchedEffect(selectedIndex, tabWidthPx) {
        if (!isDragging && tabWidthPx > 0f) {
            val target = (selectedIndex * tabWidthPx).coerceIn(0f, maxPillX)
            launch { pillX.animateTo(target, SLIDE) }
            launch {
                pillScale.animateTo(1.06f, BUMP)
                pillScale.animateTo(1f, SETTLE)
            }
        }
    }

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .height(pillHeight)
            .clip(CircleShape)
            .then(
                if (useBlur) {
                    Modifier.agslFrostedGlass(
                        radius = 20f,
                        tint = outerColor.copy(alpha = if (isDark) 0.6f else 0.4f)
                    )
                } else {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                outerColor.copy(alpha = 0.8f),
                                outerColor.copy(alpha = 0.2f)
                            )
                        )
                    )
                }
            )
            .padding(innerPadding)
            .onSizeChanged { size -> containerWidthPx = size.width }
            .pointerInput(count, maxPillX, tabWidthPx) {
                coroutineScope {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging      = true
                            dragStartTouchX = offset.x
                            dragStartPillX  = pillX.value
                            lastNearestTab  = selectedIndex
                            launch { pillX.stop() }
                            launch { pillScale.snapTo(1f) }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val totalDelta = change.position.x - dragStartTouchX
                            val newPillX   = clampPillX(dragStartPillX + totalDelta)
                            launch { pillX.snapTo(newPillX) }
                            
                            val currentNearest = nearestTab(newPillX + tabWidthPx / 2f)
                            if (currentNearest != lastNearestTab) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                lastNearestTab = currentNearest
                            }
                            
                            val restX   = selectedIndex * tabWidthPx
                            val stretch = if (tabWidthPx > 0f) abs(newPillX - restX) / tabWidthPx else 0f
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
                            if (finalTab != selectedIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                .then(
                    if (useBlur) {
                        Modifier.agslFrostedGlass(
                            radius = 15f,
                            tint = innerColor
                        )
                    } else {
                        Modifier.background(innerColor)
                    }
                )
        )

        Row(
            modifier          = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, label ->
                val tabCenterX  = (index + 0.5f) * tabWidthPx
                
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            // Cálculo de proximidad dentro de graphicsLayer para evitar recomposición masiva
                            val pillCenterX = pillX.value + tabWidthPx / 2f
                            val proximity = if (tabWidthPx > 0f) (1f - abs(pillCenterX - tabCenterX) / tabWidthPx).coerceIn(0f, 1f) else if (index == selectedIndex) 1f else 0f
                            alpha = 0.50f + proximity * 0.50f
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() }, 
                            indication = null
                        ) {
                            if (!isDragging) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTabSelected(index)
                            }
                        }
                ) {
                    // Usamos dos capas de texto con opacidad variable en lugar de lerp(color)
                    // para evitar recomponer el composable Text en cada frame de la animación.
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text       = label,
                            color      = idleTextColor,
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Normal,
                            modifier   = Modifier.graphicsLayer {
                                val pillCenterX = pillX.value + tabWidthPx / 2f
                                val proximity = if (tabWidthPx > 0f) (1f - abs(pillCenterX - tabCenterX) / tabWidthPx).coerceIn(0f, 1f) else if (index == selectedIndex) 1f else 0f
                                alpha = 1f - proximity
                            }
                        )
                        Text(
                            text       = label,
                            color      = activeTextColor,
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.graphicsLayer {
                                val pillCenterX = pillX.value + tabWidthPx / 2f
                                val proximity = if (tabWidthPx > 0f) (1f - abs(pillCenterX - tabCenterX) / tabWidthPx).coerceIn(0f, 1f) else if (index == selectedIndex) 1f else 0f
                                alpha = proximity
                            }
                        )
                    }
                }
            }
        }
    }
}
