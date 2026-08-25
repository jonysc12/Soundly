@file:OptIn(ExperimentalSharedTransitionApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.soundly.ui.componentes

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soundly.R
import com.soundly.data.model.Song
import com.soundly.data.model.Playlist
import com.soundly.player.PlayerUiState
import com.soundly.data.repository.ProgressBarType
import com.soundly.data.repository.ArtworkShape
import com.soundly.data.repository.AnimationSpeed
import com.soundly.data.repository.PlayerType
import kotlinx.coroutines.delay

@Composable
internal fun SongMetadata(
    title: String,
    artist: String,
    onColor: Color,
    subColor: Color,
    titleStyle: androidx.compose.ui.text.TextStyle,
    artistStyle: androidx.compose.ui.text.TextStyle,
    marqueeTextEnabled: Boolean,
    textAlignCentered: Boolean,
    isUltraShortScreen: Boolean,
    isTinyScreen: Boolean,
    onArtistClick: () -> Unit
) {
    var titleOverflows by remember(title) { mutableStateOf(false) }
    var artistOverflows by remember(artist) { mutableStateOf(false) }
    
    var isTitleMoving by remember(title) { mutableStateOf(false) }
    var isArtistMoving by remember(artist) { mutableStateOf(false) }

    LaunchedEffect(title, marqueeTextEnabled, titleOverflows) {
        isTitleMoving = false
        if (marqueeTextEnabled && titleOverflows) {
            delay(1200)
            isTitleMoving = true
        }
    }
    
    LaunchedEffect(artist, marqueeTextEnabled, artistOverflows) {
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

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = if (textAlignCentered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        val marqueeModifier = if (marqueeTextEnabled) Modifier.basicMarquee() else Modifier
        
        AnimatedContent(
            targetState = title,
            transitionSpec = {
                (fadeIn(tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                    .togetherWith(fadeOut(tween(90)))
            },
            label = "title_anim"
        ) { targetTitle ->
            Text(
                text = if (targetTitle.isNotBlank()) targetTitle else stringResource(R.string.info_value_no_title),
                style = titleStyle,
                color = onColor,
                maxLines = if (isUltraShortScreen) 1 else if (isTinyScreen) 2 else 1,
                overflow = if (marqueeTextEnabled) TextOverflow.Visible else TextOverflow.Ellipsis,
                textAlign = if (textAlignCentered) TextAlign.Center else TextAlign.Start,
                onTextLayout = { titleOverflows = it.hasVisualOverflow },
                modifier = Modifier
                    .then(if (marqueeTextEnabled && isTitleMoving) Modifier.fadingEdge(titleFadeBrush) else Modifier)
                    .then(marqueeModifier)
                    .then(if (textAlignCentered) Modifier.fillMaxWidth() else Modifier)
            )
        }

        AnimatedContent(
            targetState = artist,
            transitionSpec = {
                (fadeIn(tween(220, delayMillis = 90)) + slideInVertically { it / 2 })
                    .togetherWith(fadeOut(tween(90)))
            },
            label = "artist_anim"
        ) { targetArtist ->
            Text(
                text = if (targetArtist.isNotBlank()) targetArtist else stringResource(R.string.info_value_unknown_artist),
                style = artistStyle,
                color = subColor,
                maxLines = 1,
                overflow = if (marqueeTextEnabled) TextOverflow.Visible else TextOverflow.Ellipsis,
                textAlign = if (textAlignCentered) TextAlign.Center else TextAlign.Start,
                onTextLayout = { artistOverflows = it.hasVisualOverflow },
                modifier = Modifier
                    .then(if (marqueeTextEnabled && isArtistMoving) Modifier.fadingEdge(artistFadeBrush) else Modifier)
                    .then(marqueeModifier)
                    .then(if (textAlignCentered) Modifier.fillMaxWidth() else Modifier)
                    .clickable { onArtistClick() }
            )
        }
    }
}

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
    onMoveQueueItem: (Int, Int) -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlayNext: (Song) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {},
    onAddToPlaylist: (String, Long) -> Unit = { _, _ -> },
    onHideSong: (Long) -> Unit = {},
    onOpenAlbum: (Long) -> Unit = {},
    onArtistClick: (Long) -> Unit = {},
    userPlaylists: List<Playlist> = emptyList(),
    playlistMembershipBySong: Map<Long, Set<String>> = emptyMap(),
    isWideLayout: Boolean = false,
    progressBarType: ProgressBarType = ProgressBarType.WAVE,
    showThumb: Boolean = true,
    progressBarThickness: Float = 6f,
    artworkShape: ArtworkShape = ArtworkShape.DEFAULT,
    windowInsets: WindowInsets? = null,
    vividColors: Boolean = false,
    lyricsExpansionSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    useLyricsAgslAnimation: Boolean = false,
    textAlignCentered: Boolean = false,
    marqueeTextEnabled: Boolean = false,
    carouselEnabled: Boolean = false,
    playerType: PlayerType = PlayerType.CLASSIC,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val rawDominant = rememberDominantColor(state.artworkUri)

    val colorScheme = remember(rawDominant, isDark, surfaceColor, surfaceVariant, vividColors) {
        val instantDominant = adaptDominantInstant(
            rawColor = rawDominant,
            isDarkTheme = isDark,
            fallback = surfaceColor,
            isVivid = vividColors
        )

        if (!isDark && vividColors) {
            val container = blendOnSurface(instantDominant, surfaceColor, 0.12f)
            val on = instantDominant 
            PlayerColorScheme(
                containerColor = container,
                onColor = on,
                subColor = on.copy(alpha = 0.80f),
                tertiaryColor = on.copy(alpha = 0.60f),
                buttonSurface = blendOnSurface(instantDominant, surfaceVariant, 0.60f)
            )
        } else {
            val container = blendOnSurface(instantDominant, surfaceColor, if (vividColors) 0.65f else 0.32f)
            val neutralOn = if (container.luminance() < 0.52f) Color.White else Color.Black
            val on = blendOnSurface(instantDominant, neutralOn, if (vividColors) 0.25f else 0.70f)
            PlayerColorScheme(
                containerColor = container,
                onColor = on,
                subColor = on.copy(alpha = if (vividColors) 0.85f else 0.82f),
                tertiaryColor = on.copy(alpha = if (vividColors) 0.65f else 0.58f),
                buttonSurface = blendOnSurface(instantDominant, surfaceVariant, if (vividColors) 0.65f else 0.32f)
            )
        }
    }

    val animatedContainerColor by animateColorAsState(targetValue = colorScheme.containerColor, animationSpec = tween(800), label = "containerColor")
    val animatedOnColor by animateColorAsState(targetValue = colorScheme.onColor, animationSpec = tween(800), label = "onColor")
    val animatedSubColor by animateColorAsState(targetValue = colorScheme.subColor, animationSpec = tween(800), label = "subColor")
    val animatedTertiaryColor by animateColorAsState(targetValue = colorScheme.tertiaryColor, animationSpec = tween(800), label = "tertiaryColor")
    val animatedButtonSurface by animateColorAsState(targetValue = colorScheme.buttonSurface, animationSpec = tween(800), label = "buttonSurface")

    var isLyricsFullscreen by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(animatedContainerColor) }
    ) {
        SharedTransitionLayout {
            AnimatedContent(
                targetState = isLyricsFullscreen,
                label = "lyrics_transition",
                transitionSpec = {
                    val duration = lyricsExpansionSpeed.duration
                    fadeIn(tween(duration)) togetherWith fadeOut(tween(duration))
                }
            ) { targetIsLyrics ->
                if (targetIsLyrics) {
                    FullscreenLyricsView(
                        lyrics = state.lyrics,
                        title = state.title,
                        artist = state.artist,
                        artworkUri = state.artworkUri,
                        positionMs = state.positionMs,
                        durationMs = state.durationMs,
                        containerColor = animatedContainerColor,
                        onColor = animatedOnColor,
                        onClose = { isLyricsFullscreen = false },
                        onSeek = onSeek,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        isPlaying = state.isPlaying,
                        useAgsl = useLyricsAgslAnimation,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    if (playerType == PlayerType.MODERN) {
                        ModernPlayerLayout(
                            state = state,
                            onCollapse = onCollapse,
                            onPlayPause = onPlayPause,
                            onNext = onNext,
                            onPrevious = onPrevious,
                            onSeek = onSeek,
                            onToggleShuffle = onToggleShuffle,
                            onToggleFavorite = onToggleFavorite,
                            onCycleRepeat = onCycleRepeat,
                            onSleepTimerSelected = onSleepTimerSelected,
                            onSleepTimerCancel = onSleepTimerCancel,
                            onMoveQueueItem = onMoveQueueItem,
                            onPlaySong = onPlaySong,
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue,
                            onAddToPlaylist = onAddToPlaylist,
                            onHideSong = onHideSong,
                            onOpenAlbum = onOpenAlbum,
                            onArtistClick = onArtistClick,
                            userPlaylists = userPlaylists,
                            playlistMembershipBySong = playlistMembershipBySong,
                            isWideLayout = isWideLayout,
                            progressBarType = progressBarType,
                            showThumb = showThumb,
                            progressBarThickness = progressBarThickness,
                            artworkShape = artworkShape,
                            windowInsets = windowInsets,
                            vividColors = vividColors,
                            lyricsExpansionSpeed = lyricsExpansionSpeed,
                            useLyricsAgslAnimation = useLyricsAgslAnimation,
                            textAlignCentered = textAlignCentered,
                            marqueeTextEnabled = marqueeTextEnabled,
                            carouselEnabled = carouselEnabled,
                            colorScheme = colorScheme,
                            animatedContainerColor = animatedContainerColor,
                            animatedOnColor = animatedOnColor,
                            animatedSubColor = animatedSubColor,
                            animatedTertiaryColor = animatedTertiaryColor,
                            animatedButtonSurface = animatedButtonSurface,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedContent,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ClassicPlayerLayout(
                            state = state,
                            onCollapse = onCollapse,
                            onPlayPause = onPlayPause,
                            onNext = onNext,
                            onPrevious = onPrevious,
                            onSeek = onSeek,
                            onToggleShuffle = onToggleShuffle,
                            onToggleFavorite = onToggleFavorite,
                            onCycleRepeat = onCycleRepeat,
                            onSleepTimerSelected = onSleepTimerSelected,
                            onSleepTimerCancel = onSleepTimerCancel,
                            onMoveQueueItem = onMoveQueueItem,
                            onPlaySong = onPlaySong,
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue,
                            onAddToPlaylist = onAddToPlaylist,
                            onHideSong = onHideSong,
                            onOpenAlbum = onOpenAlbum,
                            onArtistClick = onArtistClick,
                            userPlaylists = userPlaylists,
                            playlistMembershipBySong = playlistMembershipBySong,
                            isWideLayout = isWideLayout,
                            progressBarType = progressBarType,
                            showThumb = showThumb,
                            progressBarThickness = progressBarThickness,
                            artworkShape = artworkShape,
                            windowInsets = windowInsets,
                            vividColors = vividColors,
                            lyricsExpansionSpeed = lyricsExpansionSpeed,
                            useLyricsAgslAnimation = useLyricsAgslAnimation,
                            textAlignCentered = textAlignCentered,
                            marqueeTextEnabled = marqueeTextEnabled,
                            carouselEnabled = carouselEnabled,
                            colorScheme = colorScheme,
                            animatedContainerColor = animatedContainerColor,
                            animatedOnColor = animatedOnColor,
                            animatedSubColor = animatedSubColor,
                            animatedTertiaryColor = animatedTertiaryColor,
                            animatedButtonSurface = animatedButtonSurface,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedContent,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
