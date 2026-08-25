package com.soundly.ui.componentes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundly.data.repository.MiniPlayerStyle
import com.soundly.data.repository.ArtworkShape
import com.soundly.data.repository.MiniProgressBarType
import com.soundly.data.repository.MiniProgressBarThickness
import com.soundly.ui.theme.rememberArtworkShape
import kotlinx.coroutines.delay
import com.soundly.R
import com.soundly.ui.componentes.adaptDominantInstant
import com.soundly.ui.componentes.blendOnSurface
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator


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
data class MiniPlayerMetadata(
    val songName: String = "Song name",
    val artistName: String = "Artist",
    val isPlaying: Boolean = false,
    val artwork: Any? = R.drawable.carga,
)

// Mantenemos MiniPlayerState por compatibilidad si es necesario, pero marcamos como obsoleto o lo usamos solo como DTO
@Immutable
data class MiniPlayerState(
    val songName: String = "Song name",
    val artistName: String = "Artist",
    val isPlaying: Boolean = false,
    val artwork: Any? = R.drawable.carga,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
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

private data class MiniColorScheme(
    val bg: Color,
    val accent: Color,
    val sub: Color,
    val btn: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(
    metadata: MiniPlayerMetadata,
    progress: () -> Float,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onDismiss: () -> Unit = {},
    accentColor: Color = Color.Unspecified,
    miniPlayerStyle: MiniPlayerStyle = MiniPlayerStyle.SOLID,
    artworkShape: ArtworkShape = ArtworkShape.CIRCLE,
    miniProgressBarType: MiniProgressBarType = MiniProgressBarType.WAVE,
    miniProgressBarThickness: MiniProgressBarThickness = MiniProgressBarThickness.NORMAL,
    showPrevious: Boolean = false,
    swipeToDismiss: Boolean = true,
    vividColors: Boolean = false,
    marqueeTextEnabled: Boolean = false,
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
    
    val colorScheme = remember(accentColor, isDark, baseSurface, vividColors, miniPlayerStyle) {
        val accentInstant = adaptDominantInstant(
            rawColor = accentColor.takeIf { it != Color.Unspecified } ?: Color.Transparent,
            isDarkTheme = isDark,
            fallback = baseSurface,
            isVivid = vividColors
        )
        
        val bg: Color
        val accent: Color
        val sub: Color
        val btn: Color

        if (!isDark && vividColors) {
            bg = if (miniPlayerStyle == MiniPlayerStyle.BLUR) baseSurface else blendOnSurface(accentInstant, baseSurface, 0.12f)
            accent = accentInstant
            sub = accent.copy(alpha = 0.80f)
            btn = accentInstant.copy(alpha = 0.18f)
        } else {
            bg = when(miniPlayerStyle) {
                MiniPlayerStyle.SOLID -> baseSurface
                MiniPlayerStyle.TINTED -> blendOnSurface(accentInstant, baseSurface, if (vividColors) 0.65f else 0.25f)
                MiniPlayerStyle.BLUR -> baseSurface
            }
            val effectiveBg = if (miniPlayerStyle == MiniPlayerStyle.BLUR) baseSurface else bg
            val onColorBase = if (effectiveBg.luminance() < 0.52f) Color.White else Color.Black
            val vividFactor = if (vividColors) 0.25f else 0.70f
            accent = blendOnSurface(accentInstant, onColorBase, vividFactor)
            sub = accent.copy(alpha = if (vividColors) 0.85f else 0.82f)
            btn = blendOnSurface(onColorBase, effectiveBg, if (vividColors) 0.25f else 0.15f) // OPACO
        }
        
        MiniColorScheme(bg, accent, sub, btn)
    }

    val rawBgColor = colorScheme.bg
    val rawAccentVibrant = colorScheme.accent
    val rawSubColor = colorScheme.sub
    val rawButtonBg = colorScheme.btn

    val bgColor by animateColorAsState(rawBgColor, tween(800), label = "miniBg")
    val accentVibrant by animateColorAsState(rawAccentVibrant, tween(800), label = "miniAccent")
    val subColor by animateColorAsState(rawSubColor, tween(800), label = "miniSub")
    val buttonBg by animateColorAsState(rawButtonBg, tween(800), label = "miniBtn")

    if (swipeToDismiss) {
        val dismissState = rememberSwipeToDismissBoxState()
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {},
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = true,
            modifier = modifier
        ) {
            LaunchedEffect(dismissState.currentValue) {
                if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd || 
                    dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                    onDismiss()
                }
            }
            MiniPlayerContent(
                pillScale = pillScale,
                pillInteractionSource = pillInteractionSource,
                onPillClick = onPillClick,
                bgColor = bgColor,
                miniPlayerStyle = miniPlayerStyle,
                metadata = metadata,
                onPlayPauseClick = onPlayPauseClick,
                onNextClick = onNextClick,
                onPreviousClick = onPreviousClick,
                accentVibrant = accentVibrant,
                subColor = subColor,
                buttonBg = buttonBg,
                artworkShape = artworkShape,
                miniProgressBarType = miniProgressBarType,
                miniProgressBarThickness = miniProgressBarThickness,
                showPrevious = showPrevious,
                marqueeTextEnabled = marqueeTextEnabled,
                progress = progress
            )
        }
    } else {
        MiniPlayerContent(
            modifier = modifier,
            pillScale = pillScale,
            pillInteractionSource = pillInteractionSource,
            onPillClick = onPillClick,
            bgColor = bgColor,
            miniPlayerStyle = miniPlayerStyle,
            metadata = metadata,
            onPlayPauseClick = onPlayPauseClick,
            onNextClick = onNextClick,
            onPreviousClick = onPreviousClick,
            accentVibrant = accentVibrant,
            subColor = subColor,
            buttonBg = buttonBg,
            artworkShape = artworkShape,
            miniProgressBarType = miniProgressBarType,
            miniProgressBarThickness = miniProgressBarThickness,
            showPrevious = showPrevious,
            marqueeTextEnabled = marqueeTextEnabled,
            progress = progress
        )
    }
}

@Composable
private fun MiniPlayerContent(
    modifier: Modifier = Modifier,
    pillScale: Float,
    pillInteractionSource: MutableInteractionSource,
    onPillClick: () -> Unit,
    bgColor: Color,
    miniPlayerStyle: MiniPlayerStyle,
    metadata: MiniPlayerMetadata,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    accentVibrant: Color,
    subColor: Color,
    buttonBg: Color,
    artworkShape: ArtworkShape,
    miniProgressBarType: MiniProgressBarType,
    miniProgressBarThickness: MiniProgressBarThickness,
    showPrevious: Boolean,
    marqueeTextEnabled: Boolean,
    progress: () -> Float
) {
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
        tonalElevation = if (miniPlayerStyle == MiniPlayerStyle.BLUR) 0.dp else 3.dp,
        shadowElevation = 0.dp,
    ) {
        MiniPlayerBody(
            metadata = metadata,
            onPlayPauseClick = onPlayPauseClick,
            onNextClick = onNextClick,
            onPreviousClick = onPreviousClick,
            textColor = accentVibrant,
            subTextColor = subColor,
            buttonBg = buttonBg,
            buttonIconColor = accentVibrant,
            progress = progress,
            artworkShape = artworkShape,
            miniProgressBarType = miniProgressBarType,
            miniProgressBarThickness = miniProgressBarThickness,
            showPrevious = showPrevious,
            marqueeTextEnabled = marqueeTextEnabled,
        )
    }
}

@Composable
private fun MiniPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    bg: Color,
    iconColor: Color,
    progress: () -> Float,
    progressBarType: MiniProgressBarType = MiniProgressBarType.WAVE,
    progressBarThickness: MiniProgressBarThickness = MiniProgressBarThickness.NORMAL,
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
        val animAmp by animateFloatAsState(
            targetValue = if (isPlaying) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 120f),
            label = "progressAmp"
        )
        
        when (progressBarType) {
            MiniProgressBarType.WAVE -> {
                @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                CircularWavyProgressIndicator(
                    progress = progress,
                    color = iconColor,
                    trackColor = iconColor.copy(alpha = 0.05f),
                    gapSize = 8.dp,
                    amplitude = { animAmp },
                    modifier = Modifier.fillMaxSize()
                )
            }
            MiniProgressBarType.PLANE -> {
                CircularProgressIndicator(
                    progress = progress,
                    color = iconColor,
                    trackColor = iconColor.copy(alpha = 0.05f),
                    strokeWidth = progressBarThickness.value.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }
            MiniProgressBarType.NONE -> { /* No progress */ }
        }

        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = { PlayEnterTransition togetherWith PlayExitTransition },
            label = "playPauseIcon",
        ) { playing ->
            Icon(
                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(if (playing) R.string.cd_pause else R.string.cd_play),
                tint = iconColor,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun MiniSkipPreviousButton(
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
        label = "skipPrevScale",
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
                onClick = { onBtnClick() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.SkipPrevious,
            contentDescription = stringResource(R.string.cd_previous),
            tint = iconColor,
            modifier = Modifier.size(24.dp),
        )
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
                onClick = { onBtnClick() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.SkipNext,
            contentDescription = stringResource(R.string.cd_next),
            tint = iconColor,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
internal fun MiniPlayerBody(
    metadata: MiniPlayerMetadata,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    subTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    buttonBg: Color = MaterialTheme.colorScheme.surfaceVariant,
    buttonIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    progress: () -> Float = { 0f },
    artworkShape: ArtworkShape = ArtworkShape.CIRCLE,
    miniProgressBarType: MiniProgressBarType = MiniProgressBarType.WAVE,
    miniProgressBarThickness: MiniProgressBarThickness = MiniProgressBarThickness.NORMAL,
    showPrevious: Boolean = false,
    marqueeTextEnabled: Boolean = false,
) {
    val shape = rememberArtworkShape(artworkShape)
    var titleOverflows by remember { mutableStateOf(false) }
    var artistOverflows by remember { mutableStateOf(false) }
    
    var isTitleMoving by remember { mutableStateOf(false) }
    var isArtistMoving by remember { mutableStateOf(false) }

    LaunchedEffect(metadata.songName, marqueeTextEnabled, titleOverflows) {
        isTitleMoving = false
        if (marqueeTextEnabled && titleOverflows) {
            delay(1200)
            isTitleMoving = true
        }
    }
    
    LaunchedEffect(metadata.artistName, marqueeTextEnabled, artistOverflows) {
        isArtistMoving = false
        if (marqueeTextEnabled && artistOverflows) {
            delay(1200)
            isArtistMoving = true
        }
    }

    val titleFadeBrush = remember(isTitleMoving) {
        Brush.horizontalGradient(
            0f to if (isTitleMoving) Color.Transparent else Color.Black,
            0.05f to Color.Black,
            0.95f to Color.Black,
            1f to if (isTitleMoving) Color.Transparent else Color.Black
        )
    }
    
    val artistFadeBrush = remember(isArtistMoving) {
        Brush.horizontalGradient(
            0f to if (isArtistMoving) Color.Transparent else Color.Black,
            0.05f to Color.Black,
            0.95f to Color.Black,
            1f to if (isArtistMoving) Color.Transparent else Color.Black
        )
    }

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
        if (metadata.artwork is Color) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(NavDimens.PILL_HEIGHT_DP.dp)
                    .clip(shape)
                    .background(metadata.artwork as Color)
            )
        } else if (metadata.artwork != null && metadata.artwork != R.drawable.carga) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(NavDimens.PILL_HEIGHT_DP.dp)
                    .clip(shape)
            ) {
                AnimatedContent(
                    targetState = metadata.artwork,
                    transitionSpec = { ArtworkEnterTransition togetherWith ArtworkExitTransition },
                    label = "miniArtwork",
                    modifier = Modifier.fillMaxSize()
                ) { art ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(art)
                            .crossfade(true)
                            .allowHardware(true)
                            .build(),
                        contentDescription = stringResource(R.string.cd_artwork),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.carga),
                        error = painterResource(id = R.drawable.carga),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(NavDimens.PILL_HEIGHT_DP.dp)
                    .clip(shape)
                    .background(textColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SmartButton,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedContent(
                targetState = metadata.songName,
                transitionSpec = { TextEnterTransition togetherWith TextExitTransition },
                label = "miniSongName",
            ) { name ->
                val marqueeModifier = if (marqueeTextEnabled) Modifier.basicMarquee() else Modifier
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                    maxLines = 1,
                    overflow = if (marqueeTextEnabled) TextOverflow.Visible else TextOverflow.Ellipsis,
                    onTextLayout = { titleOverflows = it.hasVisualOverflow },
                    modifier = Modifier
                        .then(if (marqueeTextEnabled && isTitleMoving) Modifier.fadingEdge(titleFadeBrush) else Modifier)
                        .then(marqueeModifier)
                )
            }
            AnimatedContent(
                targetState = metadata.artistName,
                transitionSpec = { TextEnterTransition togetherWith TextExitTransition },
                label = "miniArtistName",
            ) { artist ->
                val marqueeModifier = if (marqueeTextEnabled) Modifier.basicMarquee() else Modifier
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = subTextColor,
                    maxLines = 1,
                    overflow = if (marqueeTextEnabled) TextOverflow.Visible else TextOverflow.Ellipsis,
                    onTextLayout = { artistOverflows = it.hasVisualOverflow },
                    modifier = Modifier
                        .then(if (marqueeTextEnabled && isArtistMoving) Modifier.fadingEdge(artistFadeBrush) else Modifier)
                        .then(marqueeModifier)
                )
            }
        }

        if (showPrevious) {
            MiniSkipPreviousButton(
                onClick = onPreviousClick,
                bg = Color.Transparent,
                iconColor = buttonIconColor,
            )
        }

        MiniPlayPauseButton(
            isPlaying = metadata.isPlaying,
            onClick = onPlayPauseClick,
            bg = buttonBg,
            iconColor = buttonIconColor,
            progress = progress,
            progressBarType = miniProgressBarType,
            progressBarThickness = miniProgressBarThickness,
        )
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
