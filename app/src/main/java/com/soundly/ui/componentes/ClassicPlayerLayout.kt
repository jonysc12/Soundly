@file:OptIn(ExperimentalSharedTransitionApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.soundly.ui.componentes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import com.soundly.R
import com.soundly.data.model.Song
import com.soundly.data.model.Playlist
import com.soundly.feature.library.components.SongMenuSource
import com.soundly.feature.library.components.SongOverflowMenu
import com.soundly.ui.componentes.edit.SongEditSheet
import com.soundly.player.PlayerUiState
import com.soundly.data.repository.ProgressBarType
import com.soundly.data.repository.ArtworkShape
import com.soundly.data.repository.AnimationSpeed
import com.soundly.ui.theme.rememberArtworkShape
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import ir.mahozad.multiplatform.wavyslider.material.WavySlider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun ClassicPlayerLayout(
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
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (String, Long) -> Unit,
    onHideSong: (Long) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onArtistClick: (Long) -> Unit,
    userPlaylists: List<Playlist>,
    playlistMembershipBySong: Map<Long, Set<String>>,
    isWideLayout: Boolean,
    progressBarType: ProgressBarType,
    showThumb: Boolean,
    progressBarThickness: Float,
    artworkShape: ArtworkShape,
    windowInsets: WindowInsets?,
    vividColors: Boolean,
    lyricsExpansionSpeed: AnimationSpeed,
    useLyricsAgslAnimation: Boolean,
    textAlignCentered: Boolean,
    marqueeTextEnabled: Boolean,
    carouselEnabled: Boolean,
    colorScheme: PlayerColorScheme,
    animatedContainerColor: Color,
    animatedOnColor: Color,
    animatedSubColor: Color,
    animatedTertiaryColor: Color,
    animatedButtonSurface: Color,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    // Sheets state
    var showSleepSheet by remember { mutableStateOf(false) }
    var showMenuSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showDeviceSheet by remember { mutableStateOf(false) }
    
    val currentSong = remember(state.queue, state.currentSongId) {
        state.queue.find { it.id == state.currentSongId }
    }

    val activeRoute = rememberCurrentRoute()
    val deviceName = remember(activeRoute) { getRouteName(context, activeRoute) }
    val deviceIcon = remember(activeRoute) { getRouteIcon(activeRoute) }

    val durationMs = max(state.durationMs, 1L)
    
    // Optimización: Progreso predictivo
    val interpolatedProgressState = produceState(
        initialValue = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
        state.positionMs, state.durationMs, state.isPlaying
    ) {
        if (!state.isPlaying || state.durationMs <= 0) {
            value = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
            return@produceState
        }
        val startPos = state.positionMs
        val startTime = System.currentTimeMillis()
        while (true) {
            val now = System.currentTimeMillis()
            val elapsed = now - startTime
            val currentPos = startPos + elapsed
            value = (currentPos.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
            delay(16)
        }
    }

    var sliderPosition by remember { mutableFloatStateOf(interpolatedProgressState.value) }
    var isUserSeeking by remember { mutableStateOf(false) }
    val latestSeek by rememberUpdatedState(onSeek)

    LaunchedEffect(state.positionMs, isUserSeeking) {
        if (!isUserSeeking) {
            sliderPosition = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
        }
    }

    val displayProgressProvider = remember {
        {
            if (isUserSeeking) sliderPosition else interpolatedProgressState.value
        }
    }

    val pagerState = rememberPagerState(
        initialPage = state.currentSongIndex.coerceAtLeast(0),
        pageCount = { state.queue.size.coerceAtLeast(1) }
    )

    LaunchedEffect(state.currentSongIndex) {
        if (carouselEnabled && state.currentSongIndex >= 0 && state.currentSongIndex < pagerState.pageCount && 
            state.currentSongIndex != pagerState.currentPage && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(state.currentSongIndex)
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        if (carouselEnabled && pagerState.settledPage != state.currentSongIndex && state.queue.isNotEmpty()) {
            val selectedSong = state.queue.getOrNull(pagerState.settledPage)
            if (selectedSong != null) {
                onPlaySong(selectedSong)
            }
        }
    }

    if (showSleepSheet) {
        SleepTimerSheet(
            remainingMs = state.sleepRemainingMs,
            onSelect = onSleepTimerSelected,
            onCancelTimer = onSleepTimerCancel,
            onDismiss = { showSleepSheet = false }
        )
    }

    if (showMenuSheet && currentSong != null) {
        SongOverflowMenu(
            song = currentSong,
            source = SongMenuSource.Library,
            userPlaylists = userPlaylists,
            playlistIdsContainingSong = playlistMembershipBySong[currentSong.id] ?: emptySet(),
            isFavorite = state.isCurrentSongFavorite,
            showMenu = true,
            onDismissRequest = { showMenuSheet = false },
            onPlayNext = { onPlayNext(currentSong) },
            onAddToQueue = { onAddToQueue(currentSong) },
            onOpenAlbum = { onOpenAlbum(it); onCollapse() },
            onOpenArtist = { onArtistClick(it); onCollapse() },
            onAddToPlaylist = { onAddToPlaylist(it, currentSong.id) },
            onToggleFavorite = onToggleFavorite,
            onDeleteSong = { onHideSong(currentSong.id) },
            onOpenSleepTimer = { showSleepSheet = true },
            onViewQueue = { showQueueSheet = true },
            onEditClick = { showEditSheet = true },
            showDeleteOption = false
        )
    }

    if (showEditSheet && currentSong != null) {
        SongEditSheet(
            song = currentSong,
            onDismissRequest = { showEditSheet = false }
        )
    }

    DeviceControlSheet(
        isOpen = showDeviceSheet,
        onDismiss = { showDeviceSheet = false },
        containerColor = animatedContainerColor,
        onColor = animatedOnColor,
        isCasting = state.isCasting
    )

    PlayerQueueSheet(
        isOpen = showQueueSheet,
        onDismiss = { showQueueSheet = false },
        queue = state.queue,
        currentSongId = state.currentSongId,
        currentSongIndex = state.currentSongIndex,
        isShuffleEnabled = state.isShuffleEnabled,
        repeatMode = state.repeatMode,
        isFavorite = state.isCurrentSongFavorite,
        onToggleShuffle = onToggleShuffle,
        onToggleFavorite = onToggleFavorite,
        onCycleRepeat = onCycleRepeat,
        onMoveItem = onMoveQueueItem,
        onPlaySong = onPlaySong,
        onPlayPause = onPlayPause,
        onSkipNext = onNext,
        isPlaying = state.isPlaying,
        title = state.title,
        artist = state.artist,
        artworkUri = state.artworkUri,
        durationMs = state.durationMs,
        positionMs = state.positionMs,
        containerColor = animatedContainerColor,
        onColor = animatedOnColor
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val typography = MaterialTheme.typography
        val currentArtworkShape = rememberArtworkShape(artworkShape)

        val designParams = remember(maxHeight, maxWidth, isWideLayout, currentArtworkShape) {
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
            val artMaxHeight = (heroMinHeight * (if (isWideLayout) 0.35f else 0.22f + (lRoom * 0.20f)))
                .coerceIn(88.dp, if (isWideLayout) 280.dp else 420.dp)
            val artWidthFraction = if (isWideLayout) 0.85f else (0.46f + (lRoom * 0.54f)).coerceIn(
                minimumValue = if (isUltraShort) 0.42f else 0.5f,
                maximumValue = 1f
            )

            DesignParams(
                isVeryShortScreen = isVeryShort,
                isTinyScreen = isTiny,
                isUltraShortScreen = isUltraShort,
                isNarrowScreen = isNarrow,
                layoutRoom = lRoom,
                horizontalPadding = hPadding,
                verticalPadding = vPadding,
                heroSectionMinHeight = if (isWideLayout) 0.dp else heroMinHeight,
                artworkShape = currentArtworkShape,
                artworkMaxHeight = artMaxHeight,
                artworkWidthFraction = artWidthFraction,
                compactControls = lRoom < 0.34f || isWideLayout,
                heroElementGap = if (isWideLayout) 12.dp else lerp(4.dp, 16.dp, lRoom),
                titleBlockGap = if (isWideLayout) 1.dp else lerp(2.dp, 7.dp, lRoom),
                headerVerticalPadding = if (isWideLayout) 8.dp else lerp(0.dp, 16.dp, lRoom),
                titleBlockVerticalPadding = if (isWideLayout) 4.dp else lerp(0.dp, 10.dp, lRoom),
                sectionSpacer = if (isWideLayout) 8.dp else lerp(4.dp, 14.dp, lRoom),
                extraControlsSpacer = if (isWideLayout) 16.dp else if (isVeryShort) 28.dp else 42.dp,
                sideButtonSize = if (isWideLayout) 48.dp else lerp(44.dp, 64.dp, lRoom),
                sideButtonIconSize = if (isWideLayout) 24.dp else lerp(22.dp, 28.dp, lRoom),
                sideButtonHorizontalPadding = if (isWideLayout) 6.dp else lerp(4.dp, 12.dp, lRoom),
                mainButtonSize = if (isWideLayout) 64.dp else lerp(56.dp, 72.dp, lRoom),
                mainButtonIconSize = if (isWideLayout) 32.dp else lerp(28.dp, 36.dp, lRoom),
                mainButtonCornerRadius = if (isWideLayout) 20.dp else lerp(18.dp, 22.dp, lRoom),
                controlRowHeight = if (isWideLayout) 64.dp else lerp(56.dp, 72.dp, lRoom),
                titleStyle = if (isTiny || isWideLayout) {
                    typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                } else {
                    typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                },
                artistStyle = if (isTiny || isWideLayout) typography.bodyMedium else typography.bodyLarge,
                timeStyle = if (isUltraShort || isWideLayout) typography.labelMedium else typography.labelLarge,
                mainSliderSpacing = if (isWideLayout) 1.dp else lerp(2.dp, 8.dp, lRoom),
                playbackBandGap = if (isWideLayout) 4.dp else lerp(2.dp, 12.dp, lRoom),
                secondaryControlsPadding = if (isWideLayout) 2.dp else lerp(0.dp, 10.dp, lRoom)
            )
        }

        val sliderInteraction = remember { MutableInteractionSource() }
        val density = LocalDensity.current
        val sliderOffsetPx = remember(density) {
            with(density) { (-2f).dp.toPx() }
        }
        val waveHeightTarget by remember(isUserSeeking, state.isPlaying) {
            derivedStateOf {
                when {
                    isUserSeeking -> 0f
                    !state.isPlaying -> 0f
                    else -> 7f
                }
            }
        }
        val animatedWaveHeight by animateFloatAsState(
            targetValue = waveHeightTarget,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            label = "waveHeight"
        )

        val playerInsets = if (isWideLayout) {
            WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)
        } else {
            WindowInsets.systemBars
        }

        var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
        var controlsVisible by remember { mutableStateOf(true) }

        val isFloatingHeaderVisible by remember(state.title, controlsVisible, isWideLayout) {
            derivedStateOf {
                (!controlsVisible) && state.title.isNotBlank()
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = designParams.verticalPadding)
                .windowInsetsPadding(playerInsets)
                .onGloballyPositioned { rootCoordinates = it }
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
                                .padding(horizontal = designParams.horizontalPadding)
                                .padding(vertical = designParams.headerVerticalPadding),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.cd_close),
                                tint = animatedOnColor,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable(onClick = onCollapse)
                            )
                            Text(
                                text = stringResource(R.string.player_header_playing),
                                style = MaterialTheme.typography.labelLarge,
                                color = animatedOnColor.copy(alpha = 0.7f),
                                letterSpacing = 2.sp
                            )
                            IconButton(onClick = { showMenuSheet = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(R.string.cd_more_options),
                                    tint = animatedOnColor
                                )
                            }
                        }

                        val availableWidth = this@BoxWithConstraints.maxWidth - (designParams.horizontalPadding * 2)
                        val idealArtworkWidth = availableWidth * designParams.artworkWidthFraction
                        val artworkSize = idealArtworkWidth.coerceAtMost(designParams.artworkMaxHeight)
                        val artworkSizePx = with(LocalDensity.current) { artworkSize.roundToPx() }

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (carouselEnabled && state.queue.isNotEmpty()) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxWidth(),
                                    pageSize = androidx.compose.foundation.pager.PageSize.Fill,
                                    contentPadding = PaddingValues(0.dp),
                                    pageSpacing = 0.dp,
                                    beyondViewportPageCount = 0,
                                    key = { index -> state.queue.getOrNull(index)?.id ?: index.toLong() }
                                ) { page ->
                                    val song = state.queue.getOrNull(page)
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(song?.artworkUri)
                                                .size(artworkSizePx)
                                                .crossfade(true)
                                                .allowHardware(true)
                                                .build(),
                                            contentDescription = stringResource(R.string.cd_artwork),
                                            contentScale = ContentScale.Crop,
                                            placeholder = androidx.compose.ui.res.painterResource(id = R.drawable.carga),
                                            error = androidx.compose.ui.res.painterResource(id = R.drawable.carga),
                                            modifier = Modifier
                                                .size(artworkSize)
                                                .clip(designParams.artworkShape)
                                                .background(animatedContainerColor.copy(alpha = 0.35f))
                                        )
                                    }
                                }
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(state.artworkUri)
                                        .size(artworkSizePx)
                                        .crossfade(true)
                                        .allowHardware(true)
                                        .build(),
                                    contentDescription = stringResource(R.string.cd_artwork),
                                    contentScale = ContentScale.Crop,
                                    placeholder = androidx.compose.ui.res.painterResource(id = R.drawable.carga),
                                    error = androidx.compose.ui.res.painterResource(id = R.drawable.carga),
                                    modifier = Modifier
                                        .size(artworkSize)
                                        .clip(designParams.artworkShape)
                                        .background(animatedContainerColor.copy(alpha = 0.35f))
                                )
                            }
                        }

                        var showArtistDialog by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = designParams.horizontalPadding)
                                .padding(vertical = designParams.titleBlockVerticalPadding)
                        ) {
                            SongMetadata(
                                title = state.title,
                                artist = state.artist,
                                onColor = animatedOnColor,
                                subColor = animatedSubColor,
                                titleStyle = designParams.titleStyle,
                                artistStyle = designParams.artistStyle,
                                marqueeTextEnabled = marqueeTextEnabled,
                                textAlignCentered = textAlignCentered,
                                isUltraShortScreen = designParams.isUltraShortScreen,
                                isTinyScreen = designParams.isTinyScreen,
                                onArtistClick = {
                                    val song = state.queue.find { it.id == state.currentSongId }
                                    val artists = song?.artistNames ?: emptyList()
                                    if (artists.size > 1) {
                                        showArtistDialog = true
                                    } else if (artists.size == 1) {
                                        onArtistClick(com.soundly.data.model.generateArtistId(artists[0]))
                                    } else {
                                        state.queue.find { it.id == state.currentSongId }?.artistId?.let { onArtistClick(it) }
                                    }
                                }
                            )
                        }

                        if (showArtistDialog) {
                            val song = state.queue.find { it.id == state.currentSongId }
                            val artists = song?.artistNames ?: emptyList()
                            AlertDialog(
                                onDismissRequest = { showArtistDialog = false },
                                confirmButton = {},
                                dismissButton = {
                                    TextButton(onClick = { showArtistDialog = false }) { Text(stringResource(R.string.button_cancel)) }
                                },
                                title = { Text(stringResource(R.string.dialog_select_artist_title), fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        artists.forEach { name ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        showArtistDialog = false
                                                        onArtistClick(com.soundly.data.model.generateArtistId(name))
                                                    }
                                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Rounded.Person, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(Modifier.width(12.dp))
                                                Text(name, style = MaterialTheme.typography.bodyLarge)
                                            }
                                        }
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(24.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = designParams.horizontalPadding),
                            verticalArrangement = Arrangement.spacedBy(designParams.mainSliderSpacing)
                        ) {
                            IsolatedProgressSection(
                                progressProvider = displayProgressProvider,
                                progressBarType = progressBarType,
                                onValueChange = { value: Float ->
                                    sliderPosition = value
                                    isUserSeeking = true
                                },
                                onValueChangeFinished = {
                                    val seekTo = (sliderPosition * durationMs).toLong()
                                    latestSeek(seekTo)
                                    isUserSeeking = false
                                },
                                sliderOffsetPx = sliderOffsetPx,
                                onColor = animatedOnColor,
                                subColor = animatedSubColor,
                                showThumb = showThumb,
                                waveHeight = animatedWaveHeight,
                                progressBarThickness = progressBarThickness,
                                durationMs = durationMs,
                                timeStyle = designParams.timeStyle,
                                sliderInteraction = sliderInteraction
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(designParams.playbackBandGap))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(designParams.controlRowHeight)
                            .padding(horizontal = designParams.horizontalPadding)
                            .onGloballyPositioned { coordinates ->
                                val root = rootCoordinates ?: return@onGloballyPositioned
                                val bounds = root.localBoundingBoxOf(coordinates)
                                val viewportHeight = root.size.height
                                if (controlsVisible != (bounds.bottom > 0f && bounds.top < viewportHeight)) {
                                    controlsVisible = bounds.bottom > 0f && bounds.top < viewportHeight
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val onPrevClick = remember(carouselEnabled, pagerState.currentPage, onPrevious) {
                            {
                                if (carouselEnabled && pagerState.currentPage > 0) {
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                } else {
                                    onPrevious()
                                }
                                Unit
                            }
                        }
                        val onNextClick = remember(carouselEnabled, pagerState.currentPage, pagerState.pageCount, onNext) {
                            {
                                if (carouselEnabled && pagerState.currentPage < pagerState.pageCount - 1) {
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                } else {
                                    onNext()
                                }
                                Unit
                            }
                        }

                        PlayerSideButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.SkipPrevious,
                            onClick = onPrevClick,
                            iconColor = animatedOnColor,
                            buttonSize = designParams.sideButtonSize,
                            iconSize = designParams.sideButtonIconSize,
                            horizontalPadding = designParams.sideButtonHorizontalPadding
                        )
                        PlayerMainButton(
                            isPlaying = state.isPlaying,
                            onClick = onPlayPause,
                            containerColor = animatedButtonSurface,
                            iconColor = animatedOnColor,
                            buttonSize = designParams.mainButtonSize,
                            iconSize = designParams.mainButtonIconSize,
                            cornerRadius = designParams.mainButtonCornerRadius
                        )
                        PlayerSideButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.SkipNext,
                            onClick = onNextClick,
                            iconColor = animatedOnColor,
                            buttonSize = designParams.sideButtonSize,
                            iconSize = designParams.sideButtonIconSize,
                            horizontalPadding = designParams.sideButtonHorizontalPadding
                        )
                    }

                    Spacer(modifier = Modifier.height(designParams.playbackBandGap))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = designParams.horizontalPadding)
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
                            onColor = animatedOnColor,
                            compact = designParams.compactControls
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(designParams.extraControlsSpacer))

                Box(modifier = Modifier.padding(horizontal = designParams.horizontalPadding)) {
                    PlayerExtraControls(
                        onOpenDevice = { showDeviceSheet = true },
                        onShare = {
                            val currentSong = state.queue.find { it.id == state.currentSongId }
                            if (currentSong != null) {
                                val file = File(currentSong.path)
                                if (file.exists()) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_song_text))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_song_chooser_title)))
                                }
                            }
                        },
                        onOpenQueue = { showQueueSheet = true },
                        onColor = animatedOnColor,
                        deviceName = deviceName,
                        deviceIcon = deviceIcon
                    )
                }
            }

            item {
                val showLyrics = state.lyrics.isLoading || !state.lyrics.isEmpty
                AnimatedVisibility(
                    visible = showLyrics,
                    enter = expandVertically(animationSpec = tween(500, easing = FastOutSlowInEasing)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(500, easing = FastOutSlowInEasing)) + fadeOut()
                ) {
                    Column {
                        Spacer(Modifier.height(designParams.sectionSpacer))
                        Spacer(Modifier.height(designParams.sectionSpacer))

                        LyricsContainer(
                            lyrics = state.lyrics,
                            positionMs = state.positionMs,
                            onColor = animatedOnColor,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onExpand = { /* Delegado al padre o manejado aquí si es necesario */ },
                            modifier = Modifier.padding(horizontal = designParams.horizontalPadding)
                        )
                    }
                }
            }

            item {
                val artistsInfo = state.artistsInfo
                val mainArtistInfo = state.artistInfo
                val showArtist = artistsInfo.isNotEmpty() || mainArtistInfo.isLoading
                
                AnimatedVisibility(
                    visible = showArtist,
                    enter = expandVertically(animationSpec = tween(500, easing = FastOutSlowInEasing)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(500, easing = FastOutSlowInEasing)) + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(designParams.sectionSpacer)) {
                        Spacer(Modifier.height(designParams.sectionSpacer))
                        
                        ArtistsCarouselContainer(
                            artists = artistsInfo,
                            onColor = animatedOnColor,
                            isLoading = mainArtistInfo.isLoading,
                            onArtistClick = { name ->
                                onArtistClick(com.soundly.data.model.generateArtistId(name))
                            },
                            modifier = Modifier.padding(horizontal = designParams.horizontalPadding)
                        )
                    }
                }
            }
        }

        FloatingPlayerHeader(
            visible = isFloatingHeaderVisible,
            title = if (state.title.isNotBlank()) state.title else stringResource(R.string.info_value_no_title),
            artist = if (state.artist.isNotBlank()) state.artist else stringResource(R.string.info_value_unknown_artist),
            isPlaying = state.isPlaying,
            isFavorite = state.isCurrentSongFavorite,
            progress = displayProgressProvider,
            containerColor = animatedContainerColor,
            onColor = animatedOnColor,
            subColor = animatedTertiaryColor,
            onToggleFavorite = onToggleFavorite,
            onPlayPause = onPlayPause,
            onArtistClick = onArtistClick,
            isWideLayout = isWideLayout,
            state = state,
            deviceName = deviceName,
            deviceIcon = deviceIcon
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
    progress: () -> Float,
    containerColor: Color,
    onColor: Color,
    subColor: Color,
    onToggleFavorite: () -> Unit,
    onPlayPause: () -> Unit,
    onArtistClick: (Long) -> Unit = {},
    isWideLayout: Boolean = false,
    state: PlayerUiState? = null,
    deviceName: String = "Dispositivo",
    deviceIcon: ImageVector = Icons.Rounded.PhoneAndroid
) {
    val artistId = remember(state) { state?.queue?.find { it.id == state.currentSongId }?.artistId }
    val currentProgress = progress()

    val animatedProgress by animateFloatAsState(
        targetValue = currentProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "progress"
    )

    val saturatedColor = remember(containerColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(containerColor.toArgb(), hsv)
        hsv[1] = (hsv[1] * 1.4f).coerceIn(0f, 1f)
        Color(android.graphics.Color.HSVToColor(hsv))
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
                .background(saturatedColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        if (isWideLayout) WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Start)
                        else WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    )
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
                        var showArtistDialog by remember { mutableStateOf(false) }

                        Text(
                            text = artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = subColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable {
                                    val song = state?.queue?.find { it.id == state.currentSongId }
                                    val artists = song?.artistNames ?: emptyList()
                                    if (artists.size > 1) {
                                        showArtistDialog = true
                                    } else if (artists.size == 1) {
                                        onArtistClick(com.soundly.data.model.generateArtistId(artists[0]))
                                    } else {
                                        artistId?.let { onArtistClick(it) }
                                    }
                                }
                        )

                        if (showArtistDialog) {
                            val song = state?.queue?.find { it.id == state.currentSongId }
                            val artists = song?.artistNames ?: emptyList()
                            AlertDialog(
                                onDismissRequest = { showArtistDialog = false },
                                confirmButton = {},
                                dismissButton = {
                                    TextButton(onClick = { showArtistDialog = false }) { Text(stringResource(R.string.button_cancel)) }
                                },
                                title = { Text(stringResource(R.string.dialog_select_artist_title), fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        artists.forEach { name ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        showArtistDialog = false
                                                        onArtistClick(com.soundly.data.model.generateArtistId(name))
                                                    }
                                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Rounded.Person, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(Modifier.width(12.dp))
                                                Text(name, style = MaterialTheme.typography.bodyLarge)
                                            }
                                        }
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = deviceIcon,
                            contentDescription = stringResource(R.string.cd_device),
                            tint = onColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = deviceName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = onColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                            contentDescription = stringResource(R.string.cd_favorite),
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
                            contentDescription = if (isPlaying) stringResource(R.string.cd_pause) else stringResource(R.string.cd_play),
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
internal fun SleepTimerSheet(
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
                        stringResource(R.string.menu_sleep_timer),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        stringResource(R.string.timer_description),
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
                            text = stringResource(R.string.timer_active_format, hours, minutes, seconds),
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
                                Text(stringResource(R.string.timer_cancel))
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
                                    label = { Text(stringResource(R.string.timer_min_label, min)) }
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
                                Text(stringResource(R.string.timer_choose_time))
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(stringResource(R.string.button_close))
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
                }) { Text(stringResource(R.string.timer_schedule)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.button_cancel)) }
            },
            title = { Text(stringResource(R.string.timer_dialog_title)) },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@Composable
internal fun IsolatedProgressSection(
    progressProvider: () -> Float,
    progressBarType: ProgressBarType,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    sliderOffsetPx: Float,
    onColor: Color,
    subColor: Color,
    showThumb: Boolean,
    waveHeight: Float,
    progressBarThickness: Float,
    durationMs: Long,
    timeStyle: androidx.compose.ui.text.TextStyle,
    sliderInteraction: MutableInteractionSource
) {
    val currentProgress = progressProvider()
    
    Column(modifier = Modifier.fillMaxWidth()) {
        when (progressBarType) {
            ProgressBarType.WAVE, ProgressBarType.PLANE -> {
                SoundlyWavySlider(
                    value = currentProgress,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationX = sliderOffsetPx },
                    enabled = true,
                    valueRange = 0f..1f,
                    onValueChangeFinished = onValueChangeFinished,
                    activeColor = onColor,
                    inactiveColor = onColor.copy(alpha = 0.25f),
                    thumbColor = onColor,
                    showThumb = showThumb,
                    isWave = progressBarType == ProgressBarType.WAVE,
                    waveHeight = waveHeight.dp,
                    waveLength = 32.dp,
                    waveThickness = progressBarThickness.dp,
                    trackThickness = progressBarThickness.dp
                )
            }
            ProgressBarType.DEFAULT -> {
                Slider(
                    value = currentProgress,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = true,
                    valueRange = 0f..1f,
                    onValueChangeFinished = onValueChangeFinished,
                    interactionSource = sliderInteraction,
                    colors = SliderDefaults.colors(
                        thumbColor = onColor,
                        activeTrackColor = onColor,
                        inactiveTrackColor = onColor.copy(alpha = 0.25f)
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime((currentProgress * durationMs).toLong()),
                modifier = Modifier.padding(start = 4.dp),
                style = timeStyle,
                color = subColor
            )
            Text(
                text = formatTime(durationMs),
                modifier = Modifier.padding(end = 5.dp),
                style = timeStyle,
                color = subColor
            )
        }
    }
}
