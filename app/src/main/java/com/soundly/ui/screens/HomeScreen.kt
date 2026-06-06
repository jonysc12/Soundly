package com.soundly.home.ui

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import com.soundly.data.model.Song
import com.soundly.player.PlayerUiState
import com.soundly.data.repository.MusicRepository
import com.soundly.debug.DebugRecompose
import com.soundly.feature.biblioteca.BibliotecaPage
import com.soundly.feature.biblioteca.BibliotecaViewModel
import com.soundly.feature.home.HomePage
import com.soundly.feature.library.LibraryPage
import com.soundly.feature.library.LibraryViewModel
import com.soundly.feature.library.pages.AlbumDetailScreen
import com.soundly.feature.library.pages.ArtistDetailScreen
import com.soundly.feature.search.SearchPage
import com.soundly.ui.componentes.*
import com.soundly.ui.screens.Settings
import com.soundly.player.PlaybackViewModel
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.launch

private const val SEARCH_PAGE_INDEX = 3
private const val TOTAL_PAGES = 4
private const val SNAP_THRESHOLD = 0.50f
private const val FLING_TRIGGER_VELOCITY_PX = 900f
private const val DRAG_DAMPING = 0.45f
private const val MAX_DRAG_STEP = 0.035f

private val SpringExpand = spring<Float>(dampingRatio = 0.92f, stiffness = 12f)
private val SpringCollapse = spring<Float>(dampingRatio = 0.94f, stiffness = 14f)
private val SpringPager = spring<Float>(dampingRatio = 0.78f, stiffness = 330f)

private sealed interface HomeDetailDestination {
    data class Album(val id: Long) : HomeDetailDestination
    data class Artist(val id: Long) : HomeDetailDestination
    data class Playlist(val id: String) : HomeDetailDestination
}

private fun HomeDetailDestination.toPersistedValue(): String = when (this) {
    is HomeDetailDestination.Album -> "album:$id"
    is HomeDetailDestination.Artist -> "artist:$id"
    is HomeDetailDestination.Playlist -> "playlist:$id"
}

private fun persistedValueToHomeDetail(value: String): HomeDetailDestination? {
    val parts = value.split(":", limit = 2)
    if (parts.size != 2) return null
    return when (parts[0]) {
        "album" -> parts[1].toLongOrNull()?.let(HomeDetailDestination::Album)
        "artist" -> parts[1].toLongOrNull()?.let(HomeDetailDestination::Artist)
        "playlist" -> HomeDetailDestination.Playlist(parts[1])
        else -> null
    }
}

private val homeDetailStackSaver = listSaver<SnapshotStateList<HomeDetailDestination>, String>(
    save = { stack -> stack.map(HomeDetailDestination::toPersistedValue) },
    restore = { values ->
        mutableStateListOf<HomeDetailDestination>().apply {
            values.mapNotNullTo(this, ::persistedValueToHomeDetail)
        }
    }
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(repository: MusicRepository) {
    DebugRecompose("HomeScreen", logEvery = 15)
    var isUserHeaderAnimated by remember { mutableStateOf(false) }
    var isHomeDetailVisible by remember { mutableStateOf(false) }
    var isHomeArtistEdgeToEdge by remember { mutableStateOf(false) }
    var isLibraryAlbumDetailVisible by remember { mutableStateOf(false) }
    var isLibraryArtistEdgeToEdge by remember { mutableStateOf(false) }
    var isBibliotecaDetailVisible by remember { mutableStateOf(false) }
    var isBibliotecaArtistEdgeToEdge by remember { mutableStateOf(false) }

    val homeDetailStack = rememberSaveable(saver = homeDetailStackSaver) {
        mutableStateListOf<HomeDetailDestination>()
    }
    val currentHomeDetail = homeDetailStack.lastOrNull()
    isHomeDetailVisible = currentHomeDetail != null
    isHomeArtistEdgeToEdge = currentHomeDetail is HomeDetailDestination.Artist

    val popHomeDetail: () -> Unit = remember(homeDetailStack) {
        {
            if (homeDetailStack.isNotEmpty()) {
                homeDetailStack.removeAt(homeDetailStack.lastIndex)
            }
        }
    }
    val openHomeDetail: (HomeDetailDestination) -> Unit = remember(homeDetailStack) {
        { destination ->
            if (homeDetailStack.lastOrNull() != destination) {
                homeDetailStack.add(destination)
            }
        }
    }

    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val libraryViewModel: LibraryViewModel = hiltViewModel()
    val bibliotecaViewModel: BibliotecaViewModel = hiltViewModel()

    val pagerState = rememberPagerState(pageCount = { TOTAL_PAGES })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val expandProgress = remember { Animatable(0f) }
    val screenHeightPx = remember { mutableFloatStateOf(0f) }
    val screenWidthPx = remember { mutableFloatStateOf(0f) }
    val miniLeftPx = remember { mutableFloatStateOf(0f) }
    val miniTopPx = remember { mutableFloatStateOf(0f) }
    val miniWidthPx = remember { mutableFloatStateOf(0f) }
    val miniHeightPx = remember { mutableFloatStateOf(0f) }

    val snapToFull: () -> Unit = remember { { scope.launch { expandProgress.animateTo(1f, SpringExpand) } } }
    val snapToMini: () -> Unit = remember { { scope.launch { expandProgress.animateTo(0f, SpringCollapse) } } }

    val onFling: (velocityPx: Float) -> Unit = remember(snapToFull, snapToMini) {
        { velocityPx ->
            scope.launch {
                val isFlingStrongEnough = kotlin.math.abs(velocityPx) >= FLING_TRIGGER_VELOCITY_PX
                if (isFlingStrongEnough) {
                    if (velocityPx < 0f) snapToFull() else snapToMini()
                } else {
                    if (expandProgress.value >= SNAP_THRESHOLD) snapToFull() else snapToMini()
                }
            }
        }
    }

    val onDrag: (Float) -> Unit = remember {
        { delta ->
            scope.launch {
                val smoothedDelta = (delta * DRAG_DAMPING).coerceIn(-MAX_DRAG_STEP, MAX_DRAG_STEP)
                expandProgress.snapTo((expandProgress.value + smoothedDelta).coerceIn(0f, 1f))
            }
        }
    }

    BackHandler(enabled = expandProgress.value > 0.05f) {
        snapToMini()
    }

    val isSearchExpanded by remember { derivedStateOf { pagerState.currentPage == SEARCH_PAGE_INDEX } }
    val showHeader by remember {
        derivedStateOf {
            !((pagerState.currentPage == 0 && isHomeDetailVisible) ||
                (pagerState.currentPage == 1 && isLibraryAlbumDetailVisible) ||
                (pagerState.currentPage == 2 && isBibliotecaDetailVisible))
        }
    }
    val applyTopSafeInset by remember {
        derivedStateOf {
            !((pagerState.currentPage == 0 && isHomeArtistEdgeToEdge) ||
                (pagerState.currentPage == 1 && isLibraryArtistEdgeToEdge) ||
                (pagerState.currentPage == 2 && isBibliotecaArtistEdgeToEdge))
        }
    }

    val onSearchToggle: () -> Unit = remember(pagerState, scope) {
        {
            scope.launch {
                val target = if (pagerState.currentPage == SEARCH_PAGE_INDEX) SEARCH_PAGE_INDEX - 1 else SEARCH_PAGE_INDEX
                pagerState.animateScrollToPage(target, animationSpec = SpringPager)
            }
        }
    }

    val miniDragModifier = remember {
        Modifier.onGloballyPositioned { coords ->
            miniLeftPx.floatValue = coords.positionInRoot().x
            miniTopPx.floatValue = coords.positionInRoot().y
            miniWidthPx.floatValue = coords.size.width.toFloat()
            miniHeightPx.floatValue = coords.size.height.toFloat()
        }
    }

    val onPlayPause: () -> Unit = remember { { playbackViewModel.onPlayPause() } }
    val onLibraryPlaySong: (Song, List<Song>) -> Unit = remember(playbackViewModel) {
        { song, queue -> playbackViewModel.play(song, queue) }
    }
    val onLibraryPlayCollection: (List<Song>, Boolean) -> Unit = remember(playbackViewModel) {
        { queue, startShuffled -> playbackViewModel.playCollection(queue, startShuffled) }
    }
    val onSettingsClick: () -> Unit = remember(context) {
        {
            context.startActivity(Intent(context, Settings::class.java))
        }
    }

    BackHandler(enabled = isHomeDetailVisible && pagerState.currentPage == 0) {
        popHomeDetail()
    }

    SubcomposeLayout(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                screenHeightPx.floatValue = coords.size.height.toFloat()
                screenWidthPx.floatValue = coords.size.width.toFloat()
            }
    ) { constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        // 1. Dock (solo nav + placeholder invisible)
        val dockPlaceables = subcompose("dock") {
            val density = LocalDensity.current
            val imeBottom = WindowInsets.ime.getBottom(density)
            val navBottom = WindowInsets.navigationBars.getBottom(density)
            // Gap de 12dp encima del teclado, solo cuando el teclado está visible
            val keyboardGap = if (imeBottom > navBottom) with(density) { 12.dp.roundToPx() } else 0
            val bottomPadding = with(density) { (navBottom + keyboardGap).toDp() }
            PlayerDock(
                playbackViewModel = playbackViewModel,
                pagerState = pagerState,
                onPlayPause = onPlayPause,
                onSearchToggle = onSearchToggle,
                onMiniPlayerClick = snapToFull,
                miniPlayerModifier = miniDragModifier,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = bottomPadding)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
            )
        }.map { it.measure(looseConstraints) }

        val dockHeight = dockPlaceables.maxOfOrNull { it.height } ?: 0

        // 2. Contenido
        val contentPlaceables = subcompose("content") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (applyTopSafeInset) {
                            Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                        } else {
                            Modifier
                        }
                    )
            ) {
                AnimatedContent(
                    targetState = pagerState.currentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 9.dp),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "pageHeader",
                ) { page ->
                    if (showHeader) {
                        when (page) {
                            0 -> SoundlyUserHeader(
                                mode = HeaderMode.HOME,
                                triggerAnimation = !isUserHeaderAnimated,
                                onAnimationComplete = { isUserHeaderAnimated = true },
                                onSettingsClick = onSettingsClick,
                            )
                            1 -> SoundlyUserHeader(
                                mode = HeaderMode.LIBRARY,
                                onSettingsClick = onSettingsClick,
                            )
                            2 -> SoundlyUserHeader(
                                mode = HeaderMode.BIBLIOTECA,
                                onSettingsClick = onSettingsClick,
                            )
                            else -> Spacer(Modifier.height(0.dp))
                        }
                    } else {
                        Spacer(Modifier.height(0.dp))
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    beyondViewportPageCount = 1,
                    userScrollEnabled = !isSearchExpanded,
                ) { page ->
                    when (page) {
                        0 -> {
                            val homeUiState by hiltViewModel<com.soundly.feature.home.HomeViewModel>().uiState.collectAsState()
                            val userName by bibliotecaViewModel.userName.collectAsState()
                            val userImageUri by bibliotecaViewModel.userImageUri.collectAsState()

                            AnimatedContent(
                                targetState = currentHomeDetail,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "HomeContent"
                            ) { destination ->
                                when (destination) {
                                    is HomeDetailDestination.Album -> {
                                        AlbumDetailScreen(
                                            albumId = destination.id,
                                            viewModel = libraryViewModel,
                                            onAlbumClick = { openHomeDetail(HomeDetailDestination.Album(it)) },
                                            onArtistClick = { openHomeDetail(HomeDetailDestination.Artist(it)) },
                                            onBack = popHomeDetail,
                                            onPlaySong = onLibraryPlaySong,
                                            onPlayCollection = onLibraryPlayCollection
                                        )
                                    }
                                    is HomeDetailDestination.Artist -> {
                                        ArtistDetailScreen(
                                            artistId = destination.id,
                                            viewModel = libraryViewModel,
                                            onAlbumClick = { openHomeDetail(HomeDetailDestination.Album(it)) },
                                            onArtistClick = { openHomeDetail(HomeDetailDestination.Artist(it)) },
                                            onBack = popHomeDetail,
                                            applySystemBarStyle = pagerState.currentPage == 0,
                                            onPlaySong = onLibraryPlaySong,
                                            onPlayCollection = onLibraryPlayCollection
                                        )
                                    }
                                    is HomeDetailDestination.Playlist -> {
                                        val playlist = homeUiState.userPlaylists.find { it.id == destination.id }
                                        val songs by bibliotecaViewModel.getSongsForPlaylist(destination.id).collectAsState(initial = emptyList())
                                        if (playlist != null) {
                                            AlbumDetailScreen(
                                                playlist = playlist,
                                                songs = songs,
                                                ownerName = userName,
                                                viewModel = libraryViewModel,
                                                ownerImageUri = userImageUri,
                                                albumArtProvider = libraryViewModel::getAlbumArtUri,
                                                onAlbumClick = { openHomeDetail(HomeDetailDestination.Album(it)) },
                                                onArtistClick = { openHomeDetail(HomeDetailDestination.Artist(it)) },
                                                onBack = popHomeDetail,
                                                onPlaySong = onLibraryPlaySong,
                                                onPlayCollection = onLibraryPlayCollection
                                            )
                                        }
                                    }
                                    null -> {
                                        HomePage(
                                            onAlbumClick = { openHomeDetail(HomeDetailDestination.Album(it)) },
                                            onArtistClick = { openHomeDetail(HomeDetailDestination.Artist(it)) },
                                            onPlaylistClick = { openHomeDetail(HomeDetailDestination.Playlist(it)) }
                                        )
                                    }
                                }
                            }
                        }
                        1 -> LibraryPage(
                            viewModel = libraryViewModel,
                            onAlbumDetailVisibilityChanged = { isLibraryAlbumDetailVisible = it },
                            onArtistEdgeToEdgeChanged = { isLibraryArtistEdgeToEdge = it },
                            isHostPageVisible = pagerState.currentPage == 1,
                            onPlaySong = onLibraryPlaySong,
                            onPlayCollection = onLibraryPlayCollection
                        )
                        2 -> BibliotecaPage(
                            viewModel = bibliotecaViewModel,
                            libraryViewModel = libraryViewModel,
                            onDetailVisibilityChanged = { isBibliotecaDetailVisible = it },
                            onArtistEdgeToEdgeChanged = { isBibliotecaArtistEdgeToEdge = it },
                            isHostPageVisible = pagerState.currentPage == 2,
                            onPlaySong = onLibraryPlaySong,
                            onPlayCollection = onLibraryPlayCollection
                        )
                        3 -> SearchPage()
                    }
                }
            }
        }.map { it.measure(constraints) }

        // 3. Overlay (ÚNICO MiniPlayer real)
        val fullPlayerPlaceables = subcompose("fullplayer") {
            PlayerOverlayLayer(
                playbackViewModel = playbackViewModel,
                progress = expandProgress.value,
                onPlayPause = onPlayPause,
                onCollapse = snapToMini,
                onDrag = onDrag,
                onFling = onFling,
                miniLeftPx = miniLeftPx.floatValue,
                miniTopPx = miniTopPx.floatValue,
                miniWidthPx = miniWidthPx.floatValue,
                miniHeightPx = miniHeightPx.floatValue,
                screenHeightPx = screenHeightPx.floatValue,
                screenWidthPx = screenWidthPx.floatValue,
                onMiniTap = snapToFull,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f),
            )
        }.map { it.measure(constraints.copy(minHeight = 0)) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            contentPlaceables.forEach { it.placeRelative(0, 0) }
            dockPlaceables.forEach { it.placeRelative(0, constraints.maxHeight - dockHeight) }
            fullPlayerPlaceables.forEach { it.placeRelativeWithLayer(0, 0) }
        }
    }
}

@Composable
private fun PlayerDock(
    playbackViewModel: PlaybackViewModel,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onPlayPause: () -> Unit,
    onSearchToggle: () -> Unit,
    onMiniPlayerClick: () -> Unit,
    miniPlayerModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    DebugRecompose("PlayerDock", logEvery = 20)
    val playerState by playbackViewModel.uiState.collectAsStateWithLifecycle()
    val miniPlayerState = rememberMiniPlayerState(playerState)
    val dominantColor = rememberDominantColor(playerState.artworkUri)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accentColor = adaptDominantInstant(
        rawColor = dominantColor,
        isDarkTheme = isDark,
        fallback = MaterialTheme.colorScheme.surface
    )
    val hasTrack = playerState.title.isNotBlank() || playerState.durationMs > 0

    SoundlyNavStack(
        pagerState = pagerState,
        miniPlayerState = miniPlayerState,
        onPlayPause = onPlayPause,
        onSearchToggle = onSearchToggle,
        onMiniPlayerClick = onMiniPlayerClick,
        miniPlayerModifier = miniPlayerModifier,
        accentColor = accentColor,
        showMini = hasTrack,
        modifier = modifier
    )
}

@Composable
private fun PlayerOverlayLayer(
    playbackViewModel: PlaybackViewModel,
    progress: Float,
    onPlayPause: () -> Unit,
    onCollapse: () -> Unit,
    onDrag: (Float) -> Unit,
    onFling: (velocityPx: Float) -> Unit,
    miniLeftPx: Float,
    miniTopPx: Float,
    miniWidthPx: Float,
    miniHeightPx: Float,
    screenHeightPx: Float,
    screenWidthPx: Float,
    onMiniTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DebugRecompose("PlayerOverlayLayer", logEvery = 20)
    val playerState by playbackViewModel.uiState.collectAsStateWithLifecycle()
    val miniPlayerState = rememberMiniPlayerState(playerState)
    val hasTrack = playerState.title.isNotBlank() || playerState.durationMs > 0

    if (!hasTrack) return

    FullPlayerOverlay(
        progress = progress,
        miniPlayerState = miniPlayerState,
        fullPlayerState = playerState,
        onPlayPause = onPlayPause,
        onNext = playbackViewModel::onNext,
        onPrevious = playbackViewModel::onPrevious,
        onSeek = playbackViewModel::onSeek,
        onToggleShuffle = playbackViewModel::onToggleShuffle,
        onToggleFavorite = playbackViewModel::onToggleFavorite,
        onCycleRepeat = playbackViewModel::onCycleRepeat,
        onSleepTimerSelected = playbackViewModel::scheduleSleepTimer,
        onSleepTimerCancel = playbackViewModel::cancelSleepTimer,
        onCollapse = onCollapse,
        onDrag = onDrag,
        onFling = onFling,
        miniLeftPx = miniLeftPx,
        miniTopPx = miniTopPx,
        miniWidthPx = miniWidthPx,
        miniHeightPx = miniHeightPx,
        screenHeightPx = screenHeightPx,
        screenWidthPx = screenWidthPx,
        onMiniTap = onMiniTap,
        modifier = modifier
    )
}

@Composable
private fun rememberMiniPlayerState(playerState: PlayerUiState): MiniPlayerState {
    return remember(
        playerState.title,
        playerState.artist,
        playerState.isPlaying,
        playerState.artworkUri,
        playerState.positionMs,
        playerState.durationMs
    ) {
        val progress = if (playerState.durationMs > 0) {
            (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        MiniPlayerState(
            songName = if (playerState.title.isNotBlank()) playerState.title else "Song name",
            artistName = if (playerState.artist.isNotBlank()) playerState.artist else "Artist",
            isPlaying = playerState.isPlaying,
            artwork = playerState.artworkUri,
            progress = progress
        )
    }
}
