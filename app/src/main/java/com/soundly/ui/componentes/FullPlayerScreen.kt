package com.soundly.ui.componentes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.SliderDefaults as M2SliderDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.soundly.R
import com.soundly.player.PlayerUiState
import com.soundly.ui.componentes.blendOnSurface
import com.soundly.ui.componentes.rememberDominantColor
import com.soundly.ui.componentes.adaptDominantInstant
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import ir.mahozad.multiplatform.wavyslider.material.WavySlider
import kotlinx.coroutines.launch
import kotlin.math.max

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerContent(
    state: PlayerUiState,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSleepTimerSelected: (Int) -> Unit,
    onSleepTimerCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestSeek by rememberUpdatedState(onSeek)
    val initialProgress = remember {
        if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }
    var sliderPosition by rememberSaveable { mutableFloatStateOf(initialProgress) }
    val sliderAnim = remember { Animatable(initialProgress) }
    var jumpFlatten by remember { mutableStateOf(false) }
    var isUserSeeking by remember { mutableStateOf(false) }

    val durationMs = max(state.durationMs, 1L)

    LaunchedEffect(state.positionMs, state.durationMs, isUserSeeking) {
        if (!isUserSeeking && durationMs > 0) {
            val target = (state.positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            val diff = kotlin.math.abs(target - sliderAnim.value)
            jumpFlatten = diff > 0.25f
            sliderAnim.animateTo(
                target,
                animationSpec = tween(
                    durationMillis = (140 + (diff * 420)).toInt().coerceIn(140, 520),
                    easing = FastOutSlowInEasing
                )
            )
            jumpFlatten = false
            sliderPosition = sliderAnim.value
        }
    }

    var showSleepSheet by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val rawDominant = rememberDominantColor(state.artworkUri)

    val colorScheme = remember(rawDominant, isDark, surfaceColor, surfaceVariant) {
        val instantDominant = adaptDominantInstant(
            rawColor = rawDominant,
            isDarkTheme = isDark,
            fallback = surfaceColor
        )
        val container = blendOnSurface(instantDominant, surfaceColor, 0.32f)
        val neutralOn = if (container.luminance() < 0.35f) Color.White else Color.Black
        val on = blendOnSurface(instantDominant, neutralOn, 0.70f)
        
        object {
            val containerColor = container
            val onColor = on
            val subColor = on.copy(alpha = 0.82f)
            val tertiaryColor = on.copy(alpha = 0.58f)
            val buttonSurface = blendOnSurface(instantDominant, surfaceVariant, 0.32f)
        }
    }
    
    val containerColor = colorScheme.containerColor
    val onColor = colorScheme.onColor
    val subColor = colorScheme.subColor
    val tertiaryColor = colorScheme.tertiaryColor
    val buttonSurface = colorScheme.buttonSurface

    val listState = rememberLazyListState()

    var viewportHeight by remember { mutableStateOf(0) }
    var controlsVisible by remember { mutableStateOf(true) }

    val isFloatingHeaderVisible by remember(controlsVisible, state.title) {
        derivedStateOf { !controlsVisible && state.title.isNotBlank() }
    }

    if (showSleepSheet) {
        SleepTimerSheet(
            remainingMs = state.sleepRemainingMs,
            onSelect = onSleepTimerSelected,
            onCancelTimer = onSleepTimerCancel,
            onDismiss = { showSleepSheet = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(containerColor)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val shapes = MaterialTheme.shapes
            val typography = MaterialTheme.typography

            val designParams = remember(maxHeight, maxWidth, shapes, typography) {
                val isVeryShort = maxHeight < 680.dp
                val isTiny = maxHeight < 600.dp
                val isUltraShort = maxHeight < 560.dp
                val isNarrow = maxWidth < 360.dp
                val hRoom = ((maxHeight.value - 540f) / 340f).coerceIn(0f, 1f)
                val wRoom = ((maxWidth.value - 340f) / 180f).coerceIn(0f, 1f)
                val lRoom = (hRoom * 0.78f + wRoom * 0.22f).coerceIn(0f, 1f)

                val hPadding = when {
                    isUltraShort -> 16.dp
                    isNarrow -> lerp(18.dp, 24.dp, lRoom)
                    else -> lerp(20.dp, 38.dp, lRoom)
                }
                val vPadding = when {
                    isUltraShort -> 8.dp
                    else -> lerp(10.dp, 20.dp, hRoom)
                }
                val heroMinHeight = (maxHeight - (vPadding * 2)).coerceAtLeast(0.dp)
                val artMaxHeight = (heroMinHeight * (0.22f + (lRoom * 0.20f)))
                    .coerceIn(88.dp, 420.dp)
                val artWidthFraction = (0.46f + (lRoom * 0.54f)).coerceIn(
                    minimumValue = if (isUltraShort) 0.42f else 0.5f,
                    maximumValue = 1f
                )

                object {
                    val isVeryShortScreen = isVeryShort
                    val isTinyScreen = isTiny
                    val isUltraShortScreen = isUltraShort
                    val isNarrowScreen = isNarrow
                    val layoutRoom = lRoom
                    val horizontalPadding = hPadding
                    val verticalPadding = vPadding
                    val heroSectionMinHeight = heroMinHeight
                    val artworkMaxHeight = artMaxHeight
                    val artworkWidthFraction = artWidthFraction
                    val artworkShape = if (isTiny) shapes.large else shapes.extraLarge
                    val compactControls = lRoom < 0.34f
                    val heroElementGap = lerp(4.dp, 16.dp, lRoom)
                    val titleBlockGap = lerp(2.dp, 7.dp, lRoom)
                    val headerVerticalPadding = lerp(0.dp, 16.dp, lRoom)
                    val titleBlockVerticalPadding = lerp(0.dp, 10.dp, lRoom)
                    val sectionSpacer = lerp(4.dp, 14.dp, lRoom)
                    val extraControlsSpacer = if (isVeryShort) 28.dp else 42.dp
                    val sideButtonSize = lerp(44.dp, 64.dp, lRoom)
                    val sideButtonIconSize = lerp(22.dp, 28.dp, lRoom)
                    val sideButtonHorizontalPadding = lerp(4.dp, 12.dp, lRoom)
                    val mainButtonSize = lerp(56.dp, 72.dp, lRoom)
                    val mainButtonIconSize = lerp(28.dp, 36.dp, lRoom)
                    val mainButtonCornerRadius = lerp(18.dp, 22.dp, lRoom)
                    val controlRowHeight = mainButtonSize
                    val titleStyle = if (isTiny) {
                        typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    } else {
                        typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    }
                    val artistStyle = if (isTiny) typography.bodyMedium else typography.bodyLarge
                    val timeStyle = if (isUltraShort) typography.labelMedium else typography.labelLarge
                    val mainSliderSpacing = lerp(2.dp, 8.dp, lRoom)
                    val playbackBandGap = lerp(2.dp, 12.dp, lRoom)
                    val secondaryControlsPadding = lerp(0.dp, 10.dp, lRoom)
                }
            }

            val sliderInteraction = remember { MutableInteractionSource() }
            val scope = rememberCoroutineScope()
            val density = LocalDensity.current
            val sliderOffsetPx = remember(density) {
                with(density) { (-2f).dp.toPx() }
            }
            val waveHeightTarget by remember(isUserSeeking, state.isPlaying, jumpFlatten) {
                derivedStateOf {
                    when {
                        isUserSeeking -> 0f
                        !state.isPlaying -> 0f
                        jumpFlatten -> 0f
                        else -> 7f
                    }
                }
            }
            val animatedWaveHeight by animateFloatAsState(
                targetValue = waveHeightTarget,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                label = "waveHeight"
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = designParams.horizontalPadding, vertical = designParams.verticalPadding)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 24f) onCollapse()
                        }
                    }
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .onGloballyPositioned { coords -> viewportHeight = coords.size.height }
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .heightIn(min = designParams.heroSectionMinHeight)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(designParams.sectionSpacer)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(designParams.heroElementGap)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = designParams.headerVerticalPadding),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Cerrar",
                                    tint = onColor,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable(onClick = onCollapse)
                                )
                                Text(
                                    text = "REPRODUCIENDO",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = onColor.copy(alpha = 0.7f),
                                    letterSpacing = 2.sp
                                )
                                IconButton(onClick = { showSleepSheet = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = "Temporizador",
                                        tint = onColor
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = state.artworkUri,
                                    contentDescription = "Carátula",
                                    contentScale = ContentScale.Crop,
                                    placeholder = androidx.compose.ui.res.painterResource(id = R.drawable.carga),
                                    error = androidx.compose.ui.res.painterResource(id = R.drawable.carga),
                                    modifier = Modifier
                                        .fillMaxWidth(designParams.artworkWidthFraction)
                                        .heightIn(min = if (designParams.isUltraShortScreen) 72.dp else 88.dp, max = designParams.artworkMaxHeight)
                                        .aspectRatio(1f, matchHeightConstraintsFirst = true)
                                        .clip(designParams.artworkShape)
                                        .background(containerColor.copy(alpha = 0.35f))
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = designParams.titleBlockVerticalPadding),
                                verticalArrangement = Arrangement.spacedBy(designParams.titleBlockGap)
                            ) {
                                Text(
                                    text = if (state.title.isNotBlank()) state.title else "Sin título",
                                    style = designParams.titleStyle,
                                    color = onColor,
                                    maxLines = if (designParams.isUltraShortScreen) 1 else if (designParams.isTinyScreen) 2 else 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (state.artist.isNotBlank()) state.artist else "Artista desconocido",
                                    style = designParams.artistStyle,
                                    color = subColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(designParams.mainSliderSpacing)
                            ) {
                                WavySlider(
                                    value = sliderAnim.value,
                                    onValueChange = { value ->
                                        val wasSeeking = isUserSeeking
                                        sliderPosition = value
                                        scope.launch {
                                            if (wasSeeking) {
                                                sliderAnim.snapTo(value)
                                            } else {
                                                sliderAnim.animateTo(
                                                    value,
                                                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                                                )
                                            }
                                        }
                                        isUserSeeking = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer { translationX = sliderOffsetPx },
                                    enabled = true,
                                    valueRange = 0f..1f,
                                    onValueChangeFinished = {
                                        val seekTo = (sliderPosition * durationMs).toLong()
                                        latestSeek(seekTo)
                                        isUserSeeking = false
                                    },
                                    interactionSource = sliderInteraction,
                                    colors = M2SliderDefaults.colors(
                                        thumbColor = onColor,
                                        activeTrackColor = onColor,
                                        inactiveTrackColor = onColor.copy(alpha = 0.25f),
                                        activeTickColor = Color.Transparent,
                                        inactiveTickColor = Color.Transparent,
                                        disabledThumbColor = onColor.copy(alpha = 0.35f),
                                        disabledActiveTrackColor = onColor.copy(alpha = 0.35f),
                                        disabledInactiveTrackColor = onColor.copy(alpha = 0.18f),
                                        disabledActiveTickColor = Color.Transparent,
                                        disabledInactiveTickColor = Color.Transparent,
                                    ),
                                    waveLength = 32.dp,
                                    waveHeight = animatedWaveHeight.dp,
                                    waveVelocity = 18.dp to WaveDirection.HEAD,
                                    waveThickness = 6.dp,
                                    trackThickness = 6.dp,
                                    incremental = false
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formatTime(state.positionMs),
                                        style = designParams.timeStyle,
                                        color = subColor
                                    )
                                    Text(
                                        text = formatTime(state.durationMs),
                                        style = designParams.timeStyle,
                                        color = subColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(designParams.playbackBandGap))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(designParams.controlRowHeight)
                                .onGloballyPositioned { coordinates ->
                                    if (viewportHeight == 0) return@onGloballyPositioned
                                    val bounds = coordinates.boundsInParent()
                                    controlsVisible = bounds.bottom > 0f && bounds.top < viewportHeight
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerSideButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.SkipPrevious,
                                onClick = onPrevious,
                                iconColor = onColor,
                                buttonSize = designParams.sideButtonSize,
                                iconSize = designParams.sideButtonIconSize,
                                horizontalPadding = designParams.sideButtonHorizontalPadding
                            )
                            PlayerMainButton(
                                isPlaying = state.isPlaying,
                                onClick = onPlayPause,
                                containerColor = buttonSurface,
                                iconColor = onColor,
                                buttonSize = designParams.mainButtonSize,
                                iconSize = designParams.mainButtonIconSize,
                                cornerRadius = designParams.mainButtonCornerRadius
                            )
                            PlayerSideButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.SkipNext,
                                onClick = onNext,
                                iconColor = onColor,
                                buttonSize = designParams.sideButtonSize,
                                iconSize = designParams.sideButtonIconSize,
                                horizontalPadding = designParams.sideButtonHorizontalPadding
                            )
                        }

                        Spacer(modifier = Modifier.height(designParams.playbackBandGap))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = designParams.secondaryControlsPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            PlayerSecondaryControls(
                                isShuffleEnabled = state.isShuffleEnabled,
                                repeatMode = state.repeatMode,
                                isFavorite = state.isCurrentSongFavorite,
                                onToggleShuffle = onToggleShuffle,
                                onToggleFavorite = onToggleFavorite,
                                onCycleRepeat = onCycleRepeat,
                                onColor = onColor,
                                compact = designParams.compactControls
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(designParams.extraControlsSpacer))

                    PlayerExtraControls(
                        onOpenDevice = { },
                        onShare = { },
                        onOpenQueue = { },
                        onColor = onColor
                    )
                }

                item {
                    Spacer(Modifier.height(designParams.sectionSpacer))
                    Spacer(Modifier.height(designParams.sectionSpacer))

                    LyricsContainer(
                        lyrics = state.lyrics,
                        positionMs = state.positionMs,
                        onColor = onColor
                    )
                }

                item {
                    Spacer(Modifier.height(designParams.sectionSpacer))

                    ArtistInfoContainer(
                        artistName = "Lana Del Rey",
                        artistDescription = "Elizabeth Woolridge Grant, known professionally as Lana Del Rey, is an American singer and songwriter.",
                        imageUrl = "",
                        onColor = onColor
                    )
                }
            }
        }

        FloatingPlayerHeader(
            visible = isFloatingHeaderVisible,
            title = if (state.title.isNotBlank()) state.title else "Sin título",
            artist = if (state.artist.isNotBlank()) state.artist else "Artista desconocido",
            isPlaying = state.isPlaying,
            isFavorite = state.isCurrentSongFavorite,
            progress = sliderPosition,
            containerColor = containerColor,
            onColor = onColor,
            subColor = tertiaryColor,
            onToggleFavorite = onToggleFavorite,
            onPlayPause = onPlayPause
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = (totalSeconds % 60).toInt()
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun FloatingPlayerHeader(
    visible: Boolean,
    title: String,
    artist: String,
    isPlaying: Boolean,
    isFavorite: Boolean,
    progress: Float,
    containerColor: Color,
    onColor: Color,
    subColor: Color,
    onToggleFavorite: () -> Unit,
    onPlayPause: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "progress"
    )

    val darkSaturatedColor = remember(containerColor) {
        val hsl = FloatArray(3)
        android.graphics.Color.colorToHSV(containerColor.toArgb(), hsl)
        hsl[1] = (hsl[1] * 1.25f).coerceIn(0f, 1f)
        hsl[2] = (hsl[2] * 0.55f).coerceIn(0f, 1f)
        Color(android.graphics.Color.HSVToColor(hsl))
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(darkSaturatedColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    .padding(start = 32.dp, end = 32.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = onColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.titleMedium,
                            color = subColor,
                            maxLines = 1
                        )
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = subColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhoneAndroid,
                            contentDescription = "Device",
                            tint = onColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Device",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onColor
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = onColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            tint = onColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp),
                color = onColor,
                trackColor = onColor.copy(alpha = 0.18f)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SleepTimerSheet(
    onSelect: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit,
    remainingMs: Long? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val presets = listOf(10, 20, 30, 45, 60, 90)
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(is24Hour = true)
    val colors = MaterialTheme.colorScheme
    val gradient = remember(colors.surfaceVariant, colors.surfaceTint) {
        Brush.linearGradient(
            0f to colors.surfaceVariant.copy(alpha = 0.9f),
            1f to colors.surfaceTint.copy(alpha = 0.15f),
            start = Offset.Zero,
            end = Offset(800f, 1200f)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(gradient)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Temporizador",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Define en cuánto tiempo se detendrá la música.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val isActive = remainingMs != null && remainingMs > 0
            AnimatedContent(
                targetState = isActive,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "sleepTimerContent"
            ) { active ->
                if (active) {
                    val remSec = (remainingMs ?: 0L) / 1000
                    val hours = remSec / 3600
                    val minutes = (remSec % 3600) / 60
                    val seconds = (remSec % 60).toInt()
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Activo: %02d:%02d:%02d".format(hours, minutes, seconds),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = onCancelTimer,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text("Cancelar temporizador")
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEach { min ->
                                ElevatedAssistChip(
                                    onClick = { onSelect(min) },
                                    label = { Text("$min min") }
                                )
                            }
                        }
                        AnimatedVisibility(
                            visible = true,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Button(
                                onClick = { showTimePicker = true },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text("Elegir hora/min (24h)")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Cerrar")
                            }
                            Spacer(Modifier.width(12.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val totalMinutes = timePickerState.hour * 60 + timePickerState.minute
                    if (totalMinutes > 0) onSelect(totalMinutes)
                    showTimePicker = false
                }) { Text("Programar") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
            },
            title = { Text("Temporizador (24h)") },
            text = { TimePicker(state = timePickerState) }
        )
    }
}
