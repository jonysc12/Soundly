package com.soundly.ui.componentes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import com.soundly.R
import com.soundly.ui.componentes.adaptDominantInstant

internal object NavDimens {
    const val PILL_HEIGHT_DP = 40
    const val NAV_PADDING_DP = 8
    const val TOTAL_HEIGHT_DP = PILL_HEIGHT_DP + (NAV_PADDING_DP * 2)
}

private val SpringSnappy = spring<Float>(dampingRatio = 0.75f, stiffness = 600f)
private val SpringBouncy = spring<Float>(dampingRatio = 0.45f, stiffness = 480f)
private val SpringSmooth = spring<Float>(dampingRatio = 0.88f, stiffness = 340f)

internal val PlayEnterTransition = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = 0.40f, stiffness = 500f)) +
        fadeIn(tween(120, easing = FastOutLinearInEasing))
internal val PlayExitTransition = scaleOut(targetScale = 0.3f, animationSpec = tween(90, easing = FastOutLinearInEasing)) +
        fadeOut(tween(70))

internal val TextEnterTransition = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f)) +
        fadeIn(tween(180, easing = FastOutSlowInEasing))
internal val TextExitTransition = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(130, easing = FastOutLinearInEasing)) +
        fadeOut(tween(100))

internal val ArtworkEnterTransition = fadeIn(tween(280, easing = FastOutSlowInEasing)) +
        scaleIn(initialScale = 0.88f, animationSpec = tween(280, easing = FastOutSlowInEasing))
internal val ArtworkExitTransition = fadeOut(tween(160)) +
        scaleOut(targetScale = 0.88f, animationSpec = tween(160))

@Immutable
data class MiniPlayerState(
    val songName: String = "Song name",
    val artistName: String = "Artist",
    val isPlaying: Boolean = false,
    val artwork: Any? = R.drawable.carga,
    val progress: Float = 0f, // 0..1
)

@Composable
internal fun rememberPressState(duration: Long = 100L): Pair<Boolean, () -> Unit> {
    var pressed by remember { mutableStateOf(false) }
    LaunchedEffect(pressed) {
        if (pressed) {
            delay(duration)
            pressed = false
        }
    }
    val trigger = remember { { if (!pressed) pressed = true } }
    return pressed to trigger
}

@Composable
fun MiniPlayer(
    state: MiniPlayerState,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    accentColor: Color = Color.Unspecified,
) {
    val haptic = LocalHapticFeedback.current
    val (isPillPressed, triggerPillPress) = rememberPressState(duration = 85L)
    val pillInteractionSource = remember { MutableInteractionSource() }

    val pillScale by animateFloatAsState(
        targetValue = if (isPillPressed) 0.968f else 1f,
        animationSpec = if (isPillPressed) SpringSnappy else SpringSmooth,
        label = "pillScale",
    )

    val onPillClick = remember(triggerPillPress, onClick) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            triggerPillPress()
            onClick()
        }
    }

    val baseSurface = MaterialTheme.colorScheme.surface
    val isDark = baseSurface.luminance() < 0.5f
    val accentInstant = adaptDominantInstant(
        rawColor = accentColor.takeIf { it != Color.Unspecified } ?: Color.Transparent,
        isDarkTheme = isDark,
        fallback = baseSurface
    )
    val bgColor = blendOnSurface(accentInstant, baseSurface, 0.25f)
    val onColorBase = if (bgColor.luminance() < 0.35f) Color.White else Color.Black
    val accentVibrant = blendOnSurface(accentInstant, onColorBase, 0.70f)
    val subColor = accentVibrant.copy(alpha = 0.82f)
    val buttonBg = blendOnSurface(accentInstant, MaterialTheme.colorScheme.surfaceVariant, 0.32f)

    Surface(
        modifier = modifier
            .height(NavDimens.TOTAL_HEIGHT_DP.dp)
            .graphicsLayer { scaleX = pillScale; scaleY = pillScale }
            .clickable(
                interactionSource = pillInteractionSource,
                indication = null,
                onClick = onPillClick,
            ),
        shape = RoundedCornerShape(50.dp),
        color = bgColor,
        tonalElevation = 3.dp,
        shadowElevation = 0.dp,
    ) {
        MiniPlayerBody(
            state = state,
            onPlayPauseClick = onPlayPauseClick,
            onNextClick = onNextClick,
            textColor = accentVibrant,
            subTextColor = subColor,
            buttonBg = buttonBg,
            buttonIconColor = accentVibrant,
            progress = state.progress,
        )
    }
}

@Composable
private fun MiniPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    bg: Color,
    iconColor: Color,
    progress: Float,
) {
    val haptic = LocalHapticFeedback.current
    val (isPressed, triggerPress) = rememberPressState(duration = 75L)
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.78f else 1f,
        animationSpec = if (isPressed) SpringBouncy else SpringSmooth,
        label = "playBtnScale",
    )

    val onBtnClick = remember(triggerPress, onClick) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            triggerPress()
            onClick()
        }
    }

    val clampedProgress = progress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .size(NavDimens.PILL_HEIGHT_DP.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onBtnClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Progress ring: single composable, amplitude animates between wavy (1f) and flat (0f)
        val animAmp by animateFloatAsState(
            targetValue = if (isPlaying) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 120f),
            label = "progressAmp"
        )
        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
        CircularWavyProgressIndicator(
            progress = { clampedProgress },
            color = iconColor,
            trackColor = iconColor.copy(alpha = 0.05f),
            gapSize = 8.dp,
            amplitude = { animAmp },
            modifier = Modifier.fillMaxSize()
        )
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = { PlayEnterTransition togetherWith PlayExitTransition },
            label = "playPauseIcon",
        ) { playing ->
            Icon(
                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playing) "Pausar" else "Reproducir",
                tint = iconColor,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun MiniSkipNextButton(
    onClick: () -> Unit,
    bg: Color,
    iconColor: Color,
) {
    val haptic = LocalHapticFeedback.current
    val (isPressed, triggerPress) = rememberPressState(duration = 70L)
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = SpringBouncy,
        label = "skipNextScale",
    )
    val onBtnClick = remember(triggerPress, onClick) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            triggerPress()
            onClick()
        }
    }
    Box(
        modifier = Modifier
            .size((NavDimens.PILL_HEIGHT_DP - 6).dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onBtnClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.SkipNext,
            contentDescription = "Siguiente",
            tint = iconColor,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
internal fun MiniPlayerBody(
    state: MiniPlayerState,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    subTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    buttonBg: Color = MaterialTheme.colorScheme.surfaceVariant,
    buttonIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    progress: Float = 0f,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = NavDimens.NAV_PADDING_DP.dp,
                vertical = NavDimens.NAV_PADDING_DP.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AnimatedContent(
            targetState = state.artwork,
            transitionSpec = { ArtworkEnterTransition togetherWith ArtworkExitTransition },
            label = "miniArtwork",
            modifier = Modifier
                .padding(start = 4.dp)
                .size(NavDimens.PILL_HEIGHT_DP.dp)
                .clip(CircleShape),
        ) { art ->
            AsyncImage(
                model = art,
                contentDescription = "Carátula",
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.carga),
                error = painterResource(id = R.drawable.carga),
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedContent(
                targetState = state.songName,
                transitionSpec = { TextEnterTransition togetherWith TextExitTransition },
                label = "miniSongName",
            ) { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AnimatedContent(
                targetState = state.artistName,
                transitionSpec = { TextEnterTransition togetherWith TextExitTransition },
                label = "miniArtistName",
            ) { artist ->
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = subTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        MiniPlayPauseButton(
            isPlaying = state.isPlaying,
            onClick = onPlayPauseClick,
            bg = buttonBg,
            iconColor = buttonIconColor,
            progress = progress,
        )
        Spacer(Modifier.width(0.dp))
        MiniSkipNextButton(
            onClick = onNextClick,
            bg = Color.Transparent,
            iconColor = buttonIconColor,
        )
    }
}

@Composable
fun CollapsedMiniPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(NavDimens.TOTAL_HEIGHT_DP.dp)
            .fillMaxWidth()
    )
}
