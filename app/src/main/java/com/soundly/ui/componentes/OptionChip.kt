package com.soundly.ui.componentes

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val physicalSpringInt = spring<Int>(dampingRatio = 0.72f, stiffness = 380f)
private val physicalSpringDp = spring<Dp>(dampingRatio = 0.72f, stiffness = 380f)
private val physicalSpringFloat = spring<Float>(dampingRatio = 0.72f, stiffness = 380f)
private val cornerTween = tween<Dp>(durationMillis = 220, easing = FastOutSlowInEasing)

private val SLOT_GAP = 8.dp

enum class LibraryFilter { PLAYLISTS, ALBUMS, ARTISTS, FOLDERS }

private data class PillItem(val label: String, val filter: LibraryFilter)

private val allPills = listOf(
    PillItem("Playlists", LibraryFilter.PLAYLISTS),
    PillItem("Álbumes",   LibraryFilter.ALBUMS),
    PillItem("Artistas",  LibraryFilter.ARTISTS),
    PillItem("Carpetas",  LibraryFilter.FOLDERS)
)

@Composable
fun OptionPillsRow(
    selected: LibraryFilter?,
    onSelect: (LibraryFilter?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val density     = LocalDensity.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        PhysicalSlot(
            visible      = selected != null,
            gapSide      = GapSide.End,
            extraStartPx = with(density) { 8.dp.roundToPx() }
        ) {
            ClearPill(onClick = { onSelect(null) })
        }

        allPills.forEach { item ->
            key(item.filter) {
                PhysicalSlot(
                    visible = selected == null || selected == item.filter,
                    gapSide = GapSide.Start
                ) {
                    OptionPill(
                        text     = item.label,
                        selected = selected == item.filter,
                        onClick  = { onSelect(if (selected == item.filter) null else item.filter) }
                    )
                }
            }
        }
    }
}

private enum class GapSide { Start, End }

@Composable
private fun PhysicalSlot(
    visible:      Boolean,
    gapSide:      GapSide,
    extraStartPx: Int = 0,
    content:      @Composable () -> Unit
) {
    val density = LocalDensity.current
    val gapPx   = with(density) { SLOT_GAP.roundToPx() }

    var contentWidthPx by remember { mutableIntStateOf(0) }

    val targetWidthPx = if (visible) extraStartPx + gapPx + contentWidthPx else 0

    val animatedWidthPx by animateIntAsState(
        targetValue   = targetWidthPx,
        animationSpec = physicalSpringInt,
        label         = "slotWidth"
    )
    val slideOffset by animateDpAsState(
        targetValue   = if (visible) 0.dp else (-10).dp,
        animationSpec = physicalSpringDp,
        label         = "slideOffset"
    )
    val contentAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label         = "contentAlpha"
    )

    val animatedWidthDp = with(density) { animatedWidthPx.coerceAtLeast(0).toDp() }
    val contentPaddingStart = with(density) {
        (extraStartPx + if (gapSide == GapSide.Start) gapPx else 0).toDp()
    }

    // El slot es interactuable solo cuando está visible (alpha > 0 suficiente)
    val interactable = contentAlpha > 0.01f

    Box(
        modifier = Modifier
            .width(animatedWidthDp)
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .padding(start = contentPaddingStart)
                .offset(x = slideOffset)
                .graphicsLayer { alpha = contentAlpha }
                // Bloquea eventos táctiles cuando el slot está colapsado
                .then(if (!interactable) Modifier.size(0.dp) else Modifier)
                .onGloballyPositioned { coords ->
                    if (coords.size.width > 0 && coords.size.width != contentWidthPx) {
                        contentWidthPx = coords.size.width
                    }
                }
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClearPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic            = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val onSurface         = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(onSurface.copy(alpha = 0.10f))
            .combinedClickable(
                indication        = null,
                interactionSource = interactionSource,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {}
            )
            .padding(horizontal = 10.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = Icons.Rounded.Close,
            contentDescription = "Quitar filtro",
            tint               = onSurface,
            modifier           = Modifier.size(16.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OptionPill(
    text:     String,
    selected: Boolean,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic            = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val onSurface         = MaterialTheme.colorScheme.onSurface

    val transition = updateTransition(targetState = selected, label = "pill")

    val scale by transition.animateFloat(
        transitionSpec = { physicalSpringFloat },
        label          = "scale"
    ) { if (it) 0.96f else 1f }

    val bgAlpha by transition.animateFloat(
        transitionSpec = { tween(220, easing = FastOutSlowInEasing) },
        label          = "bgAlpha"
    ) { if (it) 0.18f else 0.08f }

    val corner by transition.animateDp(
        transitionSpec = { cornerTween },
        label          = "corner"
    ) { if (it) 26.dp else 20.dp }

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(corner))
            .background(onSurface.copy(alpha = bgAlpha))
            .combinedClickable(
                indication        = null,
                interactionSource = interactionSource,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {}
            )
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = text,
            style = MaterialTheme.typography.bodyMedium,
            color = onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewOptionPills() {
    var selected by remember { mutableStateOf<LibraryFilter?>(null) }
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        OptionPillsRow(selected = selected, onSelect = { selected = it })
    }
}
