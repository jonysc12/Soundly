package com.soundly.ui.componentes.listas

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Marca la posición (índice) en la que empieza cada letra dentro de la lista,
 * para poder dibujar los "ticks" de sección sobre el track y saber en qué
 * letra estamos en todo momento sin recorrer la lista completa en cada evento.
 */
private fun firstLetterOf(name: String): String {
    val c = name.firstOrNull()?.uppercaseChar() ?: '#'
    return if (c in 'A'..'Z') c.toString() else "#"
}

private fun resolveIndex(y: Float, maxHeight: Float, itemCount: Int): Int {
    if (maxHeight <= 0 || itemCount == 0) return 0
    val percent = (y / maxHeight).coerceIn(0f, 1f)
    return (percent * (itemCount - 1)).roundToInt().coerceIn(0, itemCount - 1)
}

@Composable
fun FastScrollIndex(
    items: List<Any>,
    itemToName: (Any) -> String,
    onScrollRequest: (Int) -> Unit,
    scrollProgress: Float,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var isDragging by remember { mutableStateOf(false) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var currentLetter by remember { mutableStateOf("") }

    // Estiramiento "líquido" del thumb: crece con la velocidad del gesto
    // y vuelve a su forma con un rebote elástico al soltar (squash & stretch).
    val stretch = remember { Animatable(1f) }
    // Pequeño "pop" táctil al tocar, para reforzar que el gesto fue capturado.
    val pressScale = remember { Animatable(1f) }

    BoxWithConstraints(
        modifier = modifier
            .width(28.dp)
            .fillMaxHeight()
            .padding(vertical = 0.dp)
    ) {
        val maxHeightPx = constraints.maxHeight.toFloat()

        val targetY = if (isDragging) dragY else scrollProgress * maxHeightPx
        val animatedThumbY by animateFloatAsState(
            targetValue = targetY.coerceIn(0f, maxHeightPx),
            animationSpec = if (isDragging) snap()
            else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "thumbY"
        )
        val thumbOffset = if (isDragging) dragY.coerceIn(0f, maxHeightPx) else animatedThumbY

        val thumbHeight by animateDpAsState(
            targetValue = if (isDragging) 52.dp else 36.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 380f),
            label = "thumbHeight"
        )
        val trackWidth by animateDpAsState(
            targetValue = if (isDragging) 8.dp else 5.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 400f),
            label = "trackWidth"
        )
        val trackAlpha by animateFloatAsState(
            targetValue = if (isDragging) 0.14f else 0.06f,
            label = "trackAlpha"
        )

        // ---------- TRACK ----------
        Box(
            modifier = Modifier
                .width(trackWidth)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = trackAlpha))
        ) {
            // ---------- THUMB LÍQUIDO ----------
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = with(density) { thumbOffset.toDp() } - (thumbHeight / 2))
                    .graphicsLayer {
                        // Se estira en Y con la velocidad del gesto y se afina en X
                        // como si conservara su "volumen", dando sensación de gel.
                        scaleY = stretch.value
                        scaleX = 1f / (1f + (stretch.value - 1f) * 0.35f)
                        this.scaleX *= pressScale.value
                        this.scaleY *= pressScale.value
                    }
                    .fillMaxWidth()
                    .height(thumbHeight)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = if (isDragging) listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                            ) else listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        )
                    )
            )
        }

        // ---------- BURBUJA DE LETRA ----------
        val bubbleSize = 46.dp
        AnimatedVisibility(
            visible = isDragging,
            enter = scaleIn(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 300f),
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f)
            ) + fadeIn(tween(120)),
            exit = scaleOut(tween(140)) + fadeOut(tween(140)),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            val bubbleYPx = thumbOffset.coerceIn(
                with(density) { (bubbleSize / 2).toPx() },
                maxHeightPx - with(density) { (bubbleSize / 2).toPx() }
            )
            Box(
                modifier = Modifier
                    .offset(
                        x = (-40).dp,
                        y = with(density) { bubbleYPx.toDp() } - (bubbleSize / 2)
                    )
                    .size(bubbleSize)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentLetter,
                    transitionSpec = {
                        (slideInVertically(tween(180)) { h -> h / 2 } + fadeIn(tween(140))) togetherWith
                                (slideOutVertically(tween(180)) { h -> -h / 2 } + fadeOut(tween(120)))
                    },
                    label = "letterTransition"
                ) { letter ->
                    Text(
                        text = letter,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // ---------- ÁREA TÁCTIL ----------
        // Permite tanto "tap" en cualquier punto (salto directo, como en iOS)
        // como arrastre continuo, todo dentro del mismo gesto.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(items, maxHeightPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()

                        isDragging = true
                        dragY = down.position.y
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            pressScale.animateTo(1.06f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }

                        val index = resolveIndex(dragY, maxHeightPx, items.size)
                        currentLetter = firstLetterOf(itemToName(items[index]))
                        onScrollRequest(index)

                        var lastY = down.position.y
                        var lastTime = down.uptimeMillis

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (change.changedToUp()) {
                                change.consume()
                                break
                            }

                            if (change.positionChanged()) {
                                change.consume()
                                val newY = change.position.y
                                val newTime = change.uptimeMillis
                                val dt = (newTime - lastTime).coerceAtLeast(1L)
                                val speed = abs(newY - lastY) / dt.toFloat() // px por ms

                                // A más velocidad, más se "estira" el thumb (efecto gomoso).
                                val targetStretch = (1f + speed * 6f).coerceIn(1f, 1.9f)
                                scope.launch {
                                    stretch.animateTo(targetStretch, tween(60, easing = LinearOutSlowInEasing))
                                }

                                dragY = newY.coerceIn(0f, maxHeightPx)
                                val newIndex = resolveIndex(dragY, maxHeightPx, items.size)
                                val newLetter = firstLetterOf(itemToName(items[newIndex]))
                                if (newLetter != currentLetter) {
                                    currentLetter = newLetter
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onScrollRequest(newIndex)

                                lastY = newY
                                lastTime = newTime
                            }
                        }

                        isDragging = false
                        scope.launch {
                            stretch.animateTo(
                                1f,
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 260f)
                            )
                        }
                        scope.launch {
                            pressScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                    }
                }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
fun FastScrollIndexPreview() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.White,
            surfaceVariant = Color(0xFF1E1E1E)
        )
    ) {
        Box(
            modifier = Modifier

        ) {
            val dummyItems = remember {
                listOf("Adele", "Beach House", "Coldplay", "Daft Punk", "Empire of the Sun", "Fleetwood Mac")
                    .flatMap { base -> List(15) { "$base ${it + 1}" } }
                    .sorted()
            }
            FastScrollIndex(
                items = dummyItems,
                itemToName = { it as String },
                onScrollRequest = { /* Preview only */ },
                scrollProgress = 0.3f,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}