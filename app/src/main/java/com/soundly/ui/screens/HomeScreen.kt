package com.soundly.home.ui

import androidx.compose.ui.res.stringResource
import com.soundly.R
import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soundly.data.model.Song
import com.soundly.data.repository.MusicRepository
import com.soundly.data.repository.PlayerExpansionMode
import com.soundly.data.repository.ArtworkShape
import com.soundly.debug.DebugRecompose
import com.soundly.feature.biblioteca.BibliotecaPage
import com.soundly.feature.biblioteca.BibliotecaViewModel
import com.soundly.feature.home.HomePage
import com.soundly.feature.library.LibraryPage
import com.soundly.feature.library.LibraryViewModel
import com.soundly.feature.library.pages.AlbumDetailScreen
import com.soundly.feature.library.pages.FolderDetailScreen
import com.soundly.feature.library.pages.PlaylistDetailScreen
import com.soundly.feature.library.pages.ArtistDetailScreen
import com.soundly.feature.search.SearchPage
import com.soundly.feature.search.SearchViewModel
import com.soundly.player.PlaybackViewModel
import com.soundly.player.PlayerUiState
import com.soundly.ui.componentes.*
import com.soundly.ui.componentes.ProgressiveEasing
import com.soundly.ui.navigation.BackStackCoordinator
import com.soundly.ui.navigation.LocalBackStackCoordinator
import com.soundly.ui.screens.settings.Settings
import com.soundly.cloud.LegacyBlurView
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.CompositingStrategy
import kotlin.math.roundToInt

private sealed interface DetailDestination {
    data class Album(val id: Long) : DetailDestination
    data class Artist(val id: Long) : DetailDestination
    data class Playlist(val id: String) : DetailDestination
    data class Folder(val path: String) : DetailDestination
}

private fun DetailDestination.toPersistedValue(): String = when (this) {
    is DetailDestination.Album -> "album:$id"
    is DetailDestination.Artist -> "artist:$id"
    is DetailDestination.Playlist -> "playlist:$id"
    is DetailDestination.Folder -> "folder:$path"
}

private fun persistedValueToDetail(value: String): DetailDestination? {
    val parts = value.split(":", limit = 2)
    if (parts.size != 2) return null
    return when (parts[0]) {
        "album" -> parts[1].toLongOrNull()?.let(DetailDestination::Album)
        "artist" -> parts[1].toLongOrNull()?.let(DetailDestination::Artist)
        "playlist" -> DetailDestination.Playlist(parts[1])
        "folder" -> DetailDestination.Folder(parts[1])
        else -> null
    }
}

private val detailStackSaver = listSaver<SnapshotStateList<DetailDestination>, String>(
    save = { stack -> stack.map(DetailDestination::toPersistedValue) },
    restore = { values ->
        mutableStateListOf<DetailDestination>().apply {
            values.mapNotNullTo(this, ::persistedValueToDetail)
        }
    }
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(repository: MusicRepository) {
    DebugRecompose("HomeScreen", logEvery = 15)
    
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val isWideLayout = configuration.screenWidthDp >= 600 || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val homeListState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    var isLibraryAlbumDetailVisible by remember { mutableStateOf(false) }
    var isLibraryArtistEdgeToEdge by remember { mutableStateOf(false) }
    var isBibliotecaDetailVisible by remember { mutableStateOf(false) }
    var isBibliotecaArtistEdgeToEdge by remember { mutableStateOf(false) }

    var currentHomeArtistId by remember { mutableStateOf<Long?>(null) }
    var currentLibraryArtistId by remember { mutableStateOf<Long?>(null) }
    var currentBibliotecaArtistId by remember { mutableStateOf<Long?>(null) }
    var currentSearchArtistId by remember { mutableStateOf<Long?>(null) }

    var externalArtistRequest by remember { mutableStateOf<Long?>(null) }

    val homeDetailStack = rememberSaveable(saver = detailStackSaver) {
        mutableStateListOf<DetailDestination>()
    }
    val currentHomeDetail = homeDetailStack.lastOrNull()

    val searchDetailStack = rememberSaveable(saver = detailStackSaver) {
        mutableStateListOf<DetailDestination>()
    }
    val currentSearchDetail = searchDetailStack.lastOrNull()

    val popHomeDetail: () -> Unit = remember(homeDetailStack) {
        { if (homeDetailStack.isNotEmpty()) homeDetailStack.removeAt(homeDetailStack.lastIndex) }
    }
    val openHomeDetail: (DetailDestination) -> Unit = remember(homeDetailStack) {
        { destination -> if (homeDetailStack.lastOrNull() != destination) homeDetailStack.add(destination) }
    }

    val popSearchDetail: () -> Unit = remember(searchDetailStack) {
        { if (searchDetailStack.isNotEmpty()) searchDetailStack.removeAt(searchDetailStack.lastIndex) }
    }
    val openSearchDetail: (DetailDestination) -> Unit = remember(searchDetailStack) {
        { destination -> if (searchDetailStack.lastOrNull() != destination) searchDetailStack.add(destination) }
    }

    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val libraryViewModel: LibraryViewModel = hiltViewModel()
    val bibliotecaViewModel: BibliotecaViewModel = hiltViewModel()
    val searchViewModel: SearchViewModel = hiltViewModel()
    
    val animationsViewModel: com.soundly.ui.screens.settings.pages.AnimationsViewModel = hiltViewModel()
    val showHomePage by animationsViewModel.showHomePage.collectAsStateWithLifecycle()
    val expansionMode by animationsViewModel.expansionMode.collectAsStateWithLifecycle()
    
    val totalPages = if (showHomePage) 4 else 3
    val searchPageIndex = totalPages - 1
    
    val pagerState = rememberPagerState(pageCount = { totalPages })

    val tabHistory = rememberSaveable(saver = listSaver<SnapshotStateList<Int>, Int>(save = { it.toList() }, restore = { mutableStateListOf<Int>().apply { addAll(it) } })) {
        mutableStateListOf<Int>()
    }

    LaunchedEffect(pagerState.currentPage) {
        if (tabHistory.lastOrNull() != pagerState.currentPage) {
            tabHistory.add(pagerState.currentPage)
        }
        if (pagerState.currentPage == searchPageIndex) {
            currentSearchArtistId = (currentSearchDetail as? DetailDestination.Artist)?.id
        } else if (showHomePage && pagerState.currentPage == 0) {
            currentHomeArtistId = (currentHomeDetail as? DetailDestination.Artist)?.id
        }
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val isSearchExpanded by remember(pagerState.currentPage, searchPageIndex) { derivedStateOf { pagerState.currentPage == searchPageIndex } }
    val showHeaderProvider = remember(pagerState.currentPage, showHomePage, searchPageIndex) {
        {
            val actualPage = if (showHomePage) pagerState.currentPage else pagerState.currentPage + 1
            
            !((actualPage == 0 && homeDetailStack.isNotEmpty()) ||
                (actualPage == 1 && isLibraryAlbumDetailVisible) ||
                (actualPage == 2 && isBibliotecaDetailVisible) ||
                (actualPage == 3 && searchDetailStack.isNotEmpty()))
        }
    }

    val miniLeftPx = remember { mutableFloatStateOf(0f) }
    val miniTopPx = remember { mutableFloatStateOf(0f) }
    val miniWidthPx = remember { mutableFloatStateOf(0f) }
    val miniHeightPx = remember { mutableFloatStateOf(0f) }

    val navLeftPx = remember { mutableFloatStateOf(0f) }
    val navTopPx = remember { mutableFloatStateOf(0f) }
    val navWidthPx = remember { mutableFloatStateOf(0f) }
    val navHeightPx = remember { mutableFloatStateOf(0f) }

    val playerProgress = remember { Animatable(0f) }
    // Aislamiento de estado para evitar recomposiciones costosas durante la animación de expansión
    val playerProgressState = remember { derivedStateOf { playerProgress.value } }

    val expandPlayer = {
        scope.launch { 
            // Acceso directo al ViewModel para obtener la velocidad sin recomponer HomeScreen
            val speed = if (animationsViewModel.expansionMode.value == PlayerExpansionMode.ELEVATION) 
                animationsViewModel.elevationSpeed.value.duration 
            else animationsViewModel.expansionSpeed.value.duration
            playerProgress.animateTo(1f, tween(durationMillis = speed, easing = LinearEasing)) 
        }
    }
    val collapsePlayer = {
        scope.launch { 
            val speed = if (animationsViewModel.expansionMode.value == PlayerExpansionMode.ELEVATION) 
                animationsViewModel.elevationSpeed.value.duration 
            else animationsViewModel.expansionSpeed.value.duration
            playerProgress.animateTo(0f, tween(durationMillis = speed, easing = LinearEasing)) 
        }
    }

    val onDragPlayer: (Float) -> Unit = { delta ->
        scope.launch { playerProgress.snapTo((playerProgress.value + delta).coerceIn(0f, 1f)) }
    }

    val onFlingPlayer: (Float) -> Unit = { velocity ->
        if (velocity < -1000f || (velocity < 1000f && playerProgress.value > 0.5f)) expandPlayer() else collapsePlayer()
    }

    val onPlayPause: () -> Unit = remember { { playbackViewModel.onPlayPause() } }
    val onLibraryPlaySong: (Song, List<Song>) -> Unit = remember { { song, queue -> playbackViewModel.play(song, queue) } }
    val onLibraryPlayCollection: (List<Song>, Boolean) -> Unit = remember { { queue, startShuffled -> playbackViewModel.playCollection(queue, startShuffled) } }
    val onSettingsClick: () -> Unit = remember { { context.startActivity(Intent(context, Settings::class.java)) } }
    
    val onAlbumDetailVisibilityChanged = remember { { visible: Boolean -> isLibraryAlbumDetailVisible = visible } }
    val onArtistEdgeToEdgeChanged = remember { { edgeToEdge: Boolean -> isLibraryArtistEdgeToEdge = edgeToEdge } }
    val onBibliotecaDetailVisibilityChanged = remember { { visible: Boolean -> isBibliotecaDetailVisible = visible } }
    val onBibliotecaArtistEdgeToEdgeChanged = remember { { edgeToEdge: Boolean -> isBibliotecaArtistEdgeToEdge = edgeToEdge } }
    val onCurrentLibraryArtistChanged = remember { { id: Long? -> currentLibraryArtistId = id } }
    val onCurrentBibliotecaArtistChanged = remember { { id: Long? -> currentBibliotecaArtistId = id } }
    val onExternalRequestConsumed = remember { { externalArtistRequest = null } }

    val playerUiState by playbackViewModel.uiState.collectAsStateWithLifecycle()
    
    // Optimización Suprema: Aislamiento de metadatos y progreso
    // Los metadatos solo cambian cuando la canción cambia (pocas veces)
    val miniPlayerMetadata by remember(playerUiState.currentSongId, playerUiState.isPlaying, playerUiState.title) {
        derivedStateOf {
            MiniPlayerMetadata(
                songName = if (playerUiState.title.isNotBlank()) playerUiState.title else "Song name",
                artistName = if (playerUiState.artist.isNotBlank()) playerUiState.artist else "Artist",
                isPlaying = playerUiState.isPlaying,
                artwork = playerUiState.artworkUri
            )
        }
    }
    
    // El progreso cambia cada 500ms. Usamos UpdatedState para que la lambda sea ESTABLE
    // y no dispare recomposiciones de HomeScreen al cambiar el tiempo.
    val latestPositionMs = rememberUpdatedState(playerUiState.positionMs)
    val latestDurationMs = rememberUpdatedState(playerUiState.durationMs)
    val miniPlayerProgressLambda = remember {
        {
            if (latestDurationMs.value > 0) {
                (latestPositionMs.value.toFloat() / latestDurationMs.value.toFloat()).coerceIn(0f, 1f)
            } else 0f
        }
    }

    val dominantColor = rememberDominantColor(playerUiState.artworkUri)
    val userPlaylists by libraryViewModel.userPlaylists.collectAsStateWithLifecycle()
    val userName by bibliotecaViewModel.userName.collectAsStateWithLifecycle()
    val userImageUri by bibliotecaViewModel.userImageUri.collectAsStateWithLifecycle()
    val playlistMembership by libraryViewModel.playlistMembershipBySong.collectAsStateWithLifecycle()

    var showQueueSheetGlobal by remember { mutableStateOf(false) }

    val onPopHomeDetail = remember(homeDetailStack) { { if (homeDetailStack.isNotEmpty()) homeDetailStack.removeAt(homeDetailStack.lastIndex) } }
    val onOpenHomeDetail = remember(homeDetailStack) { { dest: DetailDestination -> if (homeDetailStack.lastOrNull() != dest) homeDetailStack.add(dest) } }
    val onPopSearchDetail = remember(searchDetailStack) { { if (searchDetailStack.isNotEmpty()) searchDetailStack.removeAt(searchDetailStack.lastIndex) } }
    val onOpenSearchDetail = remember(searchDetailStack) { { dest: DetailDestination -> if (searchDetailStack.lastOrNull() != dest) searchDetailStack.add(dest) } }
    val onShowQueue = remember { { showQueueSheetGlobal = true } }

    val backHandlerEnabled by remember { derivedStateOf { playerProgress.value > 0.05f } }
    
    val popTabHistory = {
        if (tabHistory.size > 1) {
            tabHistory.removeAt(tabHistory.lastIndex)
            val prevPage = tabHistory.last()
            scope.launch { pagerState.animateScrollToPage(prevPage) }
            true
        } else false
    }

    BackHandler(enabled = !backHandlerEnabled && showHomePage && currentHomeDetail != null && pagerState.currentPage == 0) { popHomeDetail() }
    BackHandler(enabled = !backHandlerEnabled && currentSearchDetail != null && pagerState.currentPage == searchPageIndex) { popSearchDetail() }
    
    val searchQuery by searchViewModel.query.collectAsStateWithLifecycle()

    // Tab history back handler
    val canPopTab by remember(backHandlerEnabled, tabHistory.size, pagerState.currentPage, currentHomeDetail, isLibraryAlbumDetailVisible, isLibraryArtistEdgeToEdge, isBibliotecaDetailVisible, isBibliotecaArtistEdgeToEdge, currentSearchDetail, searchQuery) {
        derivedStateOf {
            !backHandlerEnabled && 
            tabHistory.size > 1 &&
            ((pagerState.currentPage == 0 && currentHomeDetail == null) ||
             (pagerState.currentPage == 1 && !isLibraryAlbumDetailVisible && !isLibraryArtistEdgeToEdge) ||
             (pagerState.currentPage == 2 && !isBibliotecaDetailVisible && !isBibliotecaArtistEdgeToEdge) ||
             (pagerState.currentPage == 3 && currentSearchDetail == null && searchQuery.isEmpty()))
        }
    }
    BackHandler(enabled = canPopTab) { popTabHistory() }

    // El BackHandler del reproductor se maneja internamente en FullPlayerOverlay/ExpandablePlayer
    // No es necesario duplicarlo aquí.

    val backStackCoordinator = remember(backHandlerEnabled) {
        BackStackCoordinator(isOverlayActive = backHandlerEnabled)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val playerTargetWidth = if (isWideLayout) {
            val sw = with(density) { configuration.screenWidthDp.dp.toPx() }
            (sw * 0.42f).coerceIn(380.dp.toPx(density), 520.dp.toPx(density))
        } else 0f

        // OPTIMIZACIÓN: Estabilización de altura para evitar recomposición masiva
        // Solo actualizamos el CompositionLocal si el cambio de altura es REAL (evita fluctuaciones de float)
        val navHeightDp by remember(density) { 
            derivedStateOf { (navHeightPx.floatValue / density.density).roundToInt().dp }
        }

        CompositionLocalProvider(
            LocalNavStackHeight provides navHeightDp,
            LocalBackStackCoordinator provides backStackCoordinator
        ) {
            Box(Modifier.fillMaxSize().graphicsLayer { 
                val p = playerProgress.value
                val easedProgress = ProgressiveEasing.transform(p)
                
                val effectiveEased = if (expansionMode == PlayerExpansionMode.ELEVATION) {
                    val startExpansionThreshold = 0.08f
                    if (easedProgress < startExpansionThreshold) 0f
                    else {
                        val ep = (easedProgress - startExpansionThreshold) / (1f - startExpansionThreshold)
                        ProgressiveEasing.transform(ep)
                    }
                } else {
                    easedProgress
                }
                
                translationX = playerTargetWidth * effectiveEased 
                compositingStrategy = CompositingStrategy.Auto
            }) {
                HomeMainContent(
                    pagerState = pagerState,
                    showHomePage = showHomePage,
                    showHeaderProvider = showHeaderProvider,
                    onSettingsClick = onSettingsClick,
                    currentHomeDetail = currentHomeDetail,
                    currentSearchDetail = currentSearchDetail,
                    isSearchExpanded = isSearchExpanded,
                    homeListState = homeListState,
                    libraryViewModel = libraryViewModel,
                    bibliotecaViewModel = bibliotecaViewModel,
                    searchViewModel = searchViewModel,
                    userPlaylists = userPlaylists,
                    userName = userName,
                    userImageUri = userImageUri,
                    onAlbumDetailVisibilityChanged = onAlbumDetailVisibilityChanged,
                    onArtistEdgeToEdgeChanged = onArtistEdgeToEdgeChanged,
                    onBibliotecaDetailVisibilityChanged = onBibliotecaDetailVisibilityChanged,
                    onBibliotecaArtistEdgeToEdgeChanged = onBibliotecaArtistEdgeToEdgeChanged,
                    onCurrentLibraryArtistChanged = onCurrentLibraryArtistChanged,
                    onCurrentBibliotecaArtistChanged = onCurrentBibliotecaArtistChanged,
                    externalArtistRequest = externalArtistRequest,
                    onExternalRequestConsumed = onExternalRequestConsumed,
                    onOpenHomeDetail = onOpenHomeDetail,
                    onPopHomeDetail = onPopHomeDetail,
                    onOpenSearchDetail = onOpenSearchDetail,
                    onPopSearchDetail = onPopSearchDetail,
                    onShowQueue = onShowQueue,
                    onPlaySong = onLibraryPlaySong,
                    onPlayCollection = onLibraryPlayCollection,
                    onSearchClick = { mode ->
                        searchViewModel.onModeChange(mode)
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.pageCount - 1)
                        }
                    }
                )
            }
        }

        PlayerDock(
            playbackViewModel = playbackViewModel, 
            searchViewModel = searchViewModel, 
            animationsViewModel = animationsViewModel, 
            pagerState = pagerState,
            onPlayPause = onPlayPause, 
            onSearchToggle = { scope.launch { pagerState.animateScrollToPage(if (pagerState.currentPage == searchPageIndex) searchPageIndex - 1 else searchPageIndex) } },
            onMiniPlayerClick = { expandPlayer() }, 
            miniPlayerModifier = Modifier.onGloballyPositioned { c -> 
                miniLeftPx.floatValue = c.positionInRoot().x
                miniTopPx.floatValue = c.positionInRoot().y
                miniWidthPx.floatValue = c.size.width.toFloat()
                miniHeightPx.floatValue = c.size.height.toFloat() 
            }.graphicsLayer { alpha = 0f },
            modifier = Modifier.align(Alignment.BottomCenter).onGloballyPositioned { c -> 
                // OPTIMIZACIÓN: Solo actualizar si hay cambios significativos en tamaño
                if (navHeightPx.floatValue != c.size.height.toFloat()) {
                    navHeightPx.floatValue = c.size.height.toFloat()
                }
                navLeftPx.floatValue = c.positionInRoot().x
                navTopPx.floatValue = c.positionInRoot().y
                navWidthPx.floatValue = c.size.width.toFloat()
            }
                .graphicsLayer { 
                    val easedProgress = ProgressiveEasing.transform(playerProgress.value)
                    translationX = playerTargetWidth * easedProgress 
                }.fillMaxWidth().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)).padding(horizontal = 20.dp).padding(bottom = 12.dp),
            onPrevious = { playbackViewModel.onPrevious() },
            showHomePage = showHomePage,
            miniPlayerMetadata = miniPlayerMetadata,
            miniPlayerProgress = miniPlayerProgressLambda
        )

        if (playerUiState.title.isNotBlank() || playerUiState.durationMs > 0) {
            FullPlayerOverlay(
                progressState = playerProgressState,
                miniPlayerMetadata = miniPlayerMetadata,
                miniPlayerProgress = miniPlayerProgressLambda,
                fullPlayerState = playerUiState,
                animationsViewModel = animationsViewModel,
                onPlayPause = onPlayPause,
                onNext = { playbackViewModel.onNext() },
                onPrevious = { playbackViewModel.onPrevious() },
                onSeek = { position: Long -> playbackViewModel.onSeek(position) },
                onToggleShuffle = { playbackViewModel.onToggleShuffle() },
                onToggleFavorite = { playbackViewModel.onToggleFavorite() },
                onCycleRepeat = { playbackViewModel.onCycleRepeat() },
                onSleepTimerSelected = { minutes: Int -> playbackViewModel.scheduleSleepTimer(minutes) },
                onSleepTimerCancel = { playbackViewModel.cancelSleepTimer() },
                onMoveQueueItem = { from: Int, to: Int -> playbackViewModel.moveQueueItem(from, to) },
                onPlaySong = { song: Song -> playbackViewModel.play(song, playerUiState.queue) },
                onPlayNext = { song: Song -> playbackViewModel.playNext(song) },
                onAddToQueue = { song: Song -> playbackViewModel.addToQueue(song) },
                onAddToPlaylist = { pId: String, sId: Long -> libraryViewModel.addSongToPlaylist(pId, sId) },
                onHideSong = { sId: Long -> libraryViewModel.hideSong(sId) },
                onOpenAlbum = { aId: Long -> if (playerProgress.value >= 0.8f) { openHomeDetail(DetailDestination.Album(aId)); collapsePlayer() } },
                onArtistClick = { aId: Long ->
                    if (playerProgress.value >= 0.8f) {
                        val alreadyOpen = when (pagerState.currentPage) {
                            0 -> currentHomeArtistId == aId
                            1 -> currentLibraryArtistId == aId
                            2 -> currentBibliotecaArtistId == aId
                            3 -> currentSearchArtistId == aId
                            else -> false
                        }
                        if (!alreadyOpen) {
                            when (pagerState.currentPage) {
                                0 -> openHomeDetail(DetailDestination.Artist(aId))
                                3 -> openSearchDetail(DetailDestination.Artist(aId))
                                1, 2 -> externalArtistRequest = aId
                            }
                        }
                        collapsePlayer()
                    }
                },
                userPlaylists = userPlaylists,
                playlistMembershipBySong = playlistMembership,
                accentColor = dominantColor,
                onCollapse = { collapsePlayer() },
                onDrag = onDragPlayer,
                onFling = onFlingPlayer,
                onDismiss = { playbackViewModel.stop() },
                miniLeftPx = miniLeftPx.floatValue,
                miniTopPx = miniTopPx.floatValue,
                miniWidthPx = miniWidthPx.floatValue,
                miniHeightPx = miniHeightPx.floatValue,
                screenHeightPx = configuration.screenHeightDp.dp.toPx(density),
                screenWidthPx = configuration.screenWidthDp.dp.toPx(density),
                onMiniTap = { expandPlayer() },
                isWideLayout = isWideLayout
            )
        }
    }

    PlayerQueueSheet(
        isOpen = showQueueSheetGlobal, onDismiss = { showQueueSheetGlobal = false }, queue = playerUiState.queue, currentSongId = playerUiState.currentSongId, currentSongIndex = playerUiState.currentSongIndex, isShuffleEnabled = playerUiState.isShuffleEnabled, repeatMode = playerUiState.repeatMode, isFavorite = playerUiState.isCurrentSongFavorite,
        onToggleShuffle = { playbackViewModel.onToggleShuffle() }, onToggleFavorite = { playbackViewModel.onToggleFavorite() }, onCycleRepeat = { playbackViewModel.onCycleRepeat() }, onMoveItem = { f, t -> playbackViewModel.moveQueueItem(f, t) }, onPlaySong = { s -> playbackViewModel.play(s, playerUiState.queue) }, onPlayPause = onPlayPause, onSkipNext = { playbackViewModel.onNext() },
        isPlaying = playerUiState.isPlaying, title = playerUiState.title, artist = playerUiState.artist, artworkUri = playerUiState.artworkUri, durationMs = playerUiState.durationMs, positionMs = playerUiState.positionMs, containerColor = MaterialTheme.colorScheme.surface, onColor = MaterialTheme.colorScheme.onSurface
    )
}


private fun Dp.toPx(density: Density): Float = with(density) { toPx() }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeMainContent(
    pagerState: androidx.compose.foundation.pager.PagerState,
    showHomePage: Boolean,
    showHeaderProvider: () -> Boolean,
    onSettingsClick: () -> Unit,
    currentHomeDetail: DetailDestination?,
    currentSearchDetail: DetailDestination?,
    isSearchExpanded: Boolean,
    homeListState: androidx.compose.foundation.lazy.LazyListState,
    libraryViewModel: LibraryViewModel,
    bibliotecaViewModel: BibliotecaViewModel,
    searchViewModel: SearchViewModel,
    userPlaylists: List<com.soundly.data.model.Playlist>,
    userName: String,
    userImageUri: android.net.Uri?,
    onAlbumDetailVisibilityChanged: (Boolean) -> Unit,
    onArtistEdgeToEdgeChanged: (Boolean) -> Unit,
    onBibliotecaDetailVisibilityChanged: (Boolean) -> Unit,
    onBibliotecaArtistEdgeToEdgeChanged: (Boolean) -> Unit,
    onCurrentLibraryArtistChanged: (Long?) -> Unit,
    onCurrentBibliotecaArtistChanged: (Long?) -> Unit,
    externalArtistRequest: Long?,
    onExternalRequestConsumed: () -> Unit,
    onOpenHomeDetail: (DetailDestination) -> Unit,
    onPopHomeDetail: () -> Unit,
    onOpenSearchDetail: (DetailDestination) -> Unit,
    onPopSearchDetail: () -> Unit,
    onShowQueue: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayCollection: (List<Song>, Boolean) -> Unit,
    onSearchClick: (com.soundly.feature.search.SearchMode) -> Unit
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        val actualPage = if (showHomePage) pagerState.currentPage else pagerState.currentPage + 1
        val showHeader = showHeaderProvider()
        val headerVisible = showHeader && actualPage <= 3

        // El Header se mantiene fuera del Pager para una transición suave y unificada
        androidx.compose.animation.AnimatedVisibility(
            visible = headerVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(modifier = Modifier
                .fillMaxWidth()
            ) {
                val headerMode = when (actualPage) {
                    0 -> HeaderMode.HOME
                    1 -> HeaderMode.LIBRARY
                    2 -> HeaderMode.BIBLIOTECA
                    3 -> HeaderMode.SEARCH
                    else -> HeaderMode.HOME
                }

                SoundlyUserHeader(
                    mode = headerMode,
                    showIcon = headerMode != HeaderMode.SEARCH,
                    onSettingsClick = onSettingsClick,
                    useStatusBarsPadding = true
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            beyondViewportPageCount = 1,
            userScrollEnabled = !isSearchExpanded,
            key = { page -> page }
        ) { page ->
            val pageIndex = if (showHomePage) page else page + 1
            val isActivePage = pagerState.currentPage == page

            when (pageIndex) {
                0 -> HomeDetailPage(
                    currentDetail = currentHomeDetail,
                    libraryViewModel = libraryViewModel,
                    bibliotecaViewModel = bibliotecaViewModel,
                    userPlaylists = userPlaylists,
                    userName = userName,
                    userImageUri = userImageUri,
                    homeListState = homeListState,
                    onOpenDetail = onOpenHomeDetail,
                    onPopDetail = onPopHomeDetail,
                    onShowQueue = onShowQueue,
                    onPlaySong = onPlaySong,
                    onPlayCollection = onPlayCollection,
                    onSearchClick = onSearchClick
                )
                1 -> LibraryPage(
                    viewModel = libraryViewModel,
                    onAlbumDetailVisibilityChanged = onAlbumDetailVisibilityChanged,
                    onArtistEdgeToEdgeChanged = onArtistEdgeToEdgeChanged,
                    onCurrentArtistChanged = onCurrentLibraryArtistChanged,
                    onViewQueue = onShowQueue,
                    onPlaySong = onPlaySong,
                    onPlayCollection = onPlayCollection,
                    externalArtistRequest = externalArtistRequest,
                    onExternalRequestConsumed = onExternalRequestConsumed,
                    isHostPageVisible = isActivePage
                )
                2 -> BibliotecaPage(
                    viewModel = bibliotecaViewModel,
                    libraryViewModel = libraryViewModel,
                    onDetailVisibilityChanged = onBibliotecaDetailVisibilityChanged,
                    onArtistEdgeToEdgeChanged = onArtistEdgeToEdgeChanged,
                    onCurrentArtistChanged = onCurrentBibliotecaArtistChanged,
                    onViewQueue = onShowQueue,
                    onPlaySong = onPlaySong,
                    onPlayCollection = onPlayCollection,
                    externalArtistRequest = externalArtistRequest,
                    onExternalRequestConsumed = onExternalRequestConsumed,
                    isHostPageVisible = isActivePage
                )
                3 -> SearchDetailPage(
                    currentDetail = currentSearchDetail,
                    libraryViewModel = libraryViewModel,
                    bibliotecaViewModel = bibliotecaViewModel,
                    searchViewModel = searchViewModel,
                    userPlaylists = userPlaylists,
                    userName = userName,
                    userImageUri = userImageUri,
                    onOpenDetail = onOpenSearchDetail,
                    onPopDetail = onPopSearchDetail,
                    onShowQueue = onShowQueue,
                    onPlaySong = onPlaySong,
                    onPlayCollection = onPlayCollection,
                    onBackToBiblioteca = { scope.launch { pagerState.animateScrollToPage(if (showHomePage) 2 else 1) } }
                )
            }
        }
    }
}

@Composable
private fun HomeDetailPage(
    currentDetail: DetailDestination?,
    libraryViewModel: LibraryViewModel,
    bibliotecaViewModel: BibliotecaViewModel,
    userPlaylists: List<com.soundly.data.model.Playlist>,
    userName: String,
    userImageUri: android.net.Uri?,
    homeListState: androidx.compose.foundation.lazy.LazyListState,
    onOpenDetail: (DetailDestination) -> Unit,
    onPopDetail: () -> Unit,
    onShowQueue: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayCollection: (List<Song>, Boolean) -> Unit,
    onSearchClick: (com.soundly.feature.search.SearchMode) -> Unit
) {
    AnimatedContent(
        targetState = currentDetail,
        label = "HomeContent",
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = Modifier.fillMaxSize()
    ) { dest ->
        when (dest) {
            is DetailDestination.Album -> AlbumDetailScreen(albumId = dest.id, viewModel = libraryViewModel, onAlbumClick = { onOpenDetail(DetailDestination.Album(it)) }, onArtistClick = { onOpenDetail(DetailDestination.Artist(it)) }, onBack = onPopDetail, onViewQueue = onShowQueue, onPlaySong = onPlaySong, onPlayCollection = onPlayCollection)
            is DetailDestination.Artist -> ArtistDetailScreen(artistId = dest.id, viewModel = libraryViewModel, onAlbumClick = { onOpenDetail(DetailDestination.Album(it)) }, onArtistClick = { onOpenDetail(DetailDestination.Artist(it)) }, onBack = onPopDetail, onViewQueue = onShowQueue, onPlaySong = onPlaySong, onPlayCollection = onPlayCollection)
            is DetailDestination.Playlist -> {
                val playlist = userPlaylists.find { it.id == dest.id } 
                    ?: if (dest.id == MusicRepository.TOP_MONTH_RECAP_ID) {
                        com.soundly.data.model.Playlist(
                            id = MusicRepository.TOP_MONTH_RECAP_ID,
                            name = stringResource(R.string.recap_playlist_title),
                            songCount = 0,
                            isAutoGenerated = true,
                            artworkUri = null,
                            showOnHome = true,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else null
                
                val playlistSongs by libraryViewModel.getSongsForPlaylist(dest.id).collectAsStateWithLifecycle(initialValue = emptyList())
                if (playlist != null) {
                    val finalPlaylist = if (dest.id == MusicRepository.TOP_MONTH_RECAP_ID) {
                        playlist.copy(songCount = playlistSongs.size)
                    } else playlist

                    PlaylistDetailScreen(
                        playlist = finalPlaylist,
                        songs = playlistSongs,
                        ownerName = userName,
                        viewModel = libraryViewModel,
                        ownerImageUri = userImageUri,
                        albumArtProvider = libraryViewModel::getAlbumArtUri,
                        onAlbumClick = { onOpenDetail(DetailDestination.Album(it)) },
                        onArtistClick = { onOpenDetail(DetailDestination.Artist(it)) },
                        onBack = onPopDetail,
                        onViewQueue = onShowQueue,
                        onPlaySong = onPlaySong,
                        onPlayCollection = onPlayCollection
                    )
                } else Box(Modifier.fillMaxSize())
            }
            is DetailDestination.Folder -> {
                FolderDetailScreen(
                    folderPath = dest.path,
                    viewModel = bibliotecaViewModel,
                    libraryViewModel = libraryViewModel,
                    onAlbumClick = { onOpenDetail(DetailDestination.Album(it)) },
                    onArtistClick = { onOpenDetail(DetailDestination.Artist(it)) },
                    onBack = onPopDetail,
                    onViewQueue = onShowQueue,
                    onPlaySong = onPlaySong,
                    onPlayCollection = onPlayCollection
                )
            }
            null -> HomePage(
                listState = homeListState, 
                onAlbumClick = { onOpenDetail(DetailDestination.Album(it)) }, 
                onArtistClick = { onOpenDetail(DetailDestination.Artist(it)) }, 
                onPlaylistClick = { onOpenDetail(DetailDestination.Playlist(it)) }, 
                onViewQueue = onShowQueue,
                onSearchClick = onSearchClick
            )
        }
    }
}

@Composable
private fun SearchDetailPage(
    currentDetail: DetailDestination?,
    libraryViewModel: LibraryViewModel,
    bibliotecaViewModel: BibliotecaViewModel,
    searchViewModel: SearchViewModel,
    userPlaylists: List<com.soundly.data.model.Playlist>,
    userName: String,
    userImageUri: android.net.Uri?,
    onOpenDetail: (DetailDestination) -> Unit,
    onPopDetail: () -> Unit,
    onShowQueue: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayCollection: (List<Song>, Boolean) -> Unit,
    onBackToBiblioteca: () -> Unit
) {
    AnimatedContent(
        targetState = currentDetail,
        label = "SearchDetailContent",
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = Modifier.fillMaxSize()
    ) { dest ->
        when (dest) {
            is DetailDestination.Album -> AlbumDetailScreen(albumId = dest.id, viewModel = libraryViewModel, onAlbumClick = { onOpenDetail(DetailDestination.Album(it)) }, onArtistClick = { onOpenDetail(DetailDestination.Artist(it)) }, onBack = onPopDetail, onViewQueue = onShowQueue, onPlaySong = onPlaySong, onPlayCollection = onPlayCollection)
            is DetailDestination.Artist -> ArtistDetailScreen(artistId = dest.id, viewModel = libraryViewModel, onAlbumClick = { onOpenDetail(DetailDestination.Album(it)) }, onArtistClick = { onOpenDetail(DetailDestination.Artist(it)) }, onBack = onPopDetail, onViewQueue = onShowQueue, onPlaySong = onPlaySong, onPlayCollection = onPlayCollection)
            is DetailDestination.Playlist -> {
                val playlist = userPlaylists.find { it.id == dest.id } 
                    ?: if (dest.id == MusicRepository.TOP_MONTH_RECAP_ID) {
                        com.soundly.data.model.Playlist(
                            id = MusicRepository.TOP_MONTH_RECAP_ID,
                            name = stringResource(R.string.recap_playlist_title),
                            songCount = 0,
                            isAutoGenerated = true,
                            artworkUri = null,
                            showOnHome = true,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else null
                
                val playlistSongs by libraryViewModel.getSongsForPlaylist(dest.id).collectAsStateWithLifecycle(initialValue = emptyList())
                if (playlist != null) {
                    val finalPlaylist = if (dest.id == MusicRepository.TOP_MONTH_RECAP_ID) {
                        playlist.copy(songCount = playlistSongs.size)
                    } else playlist

                    PlaylistDetailScreen(
                        playlist = finalPlaylist,
                        songs = playlistSongs,
                        ownerName = userName,
                        viewModel = libraryViewModel,
                        ownerImageUri = userImageUri,
                        albumArtProvider = libraryViewModel::getAlbumArtUri,
                        onAlbumClick = { onOpenDetail(DetailDestination.Album(it)) },
                        onArtistClick = { onOpenDetail(DetailDestination.Artist(it)) },
                        onBack = onPopDetail,
                        onViewQueue = onShowQueue,
                        onPlaySong = onPlaySong,
                        onPlayCollection = onPlayCollection
                    )
                } else Box(Modifier.fillMaxSize())
            }
            is DetailDestination.Folder -> {
                FolderDetailScreen(
                    folderPath = dest.path,
                    viewModel = bibliotecaViewModel,
                    libraryViewModel = libraryViewModel,
                    onAlbumClick = { onOpenDetail(DetailDestination.Album(it)) },
                    onArtistClick = { onOpenDetail(DetailDestination.Artist(it)) },
                    onBack = onPopDetail,
                    onViewQueue = onShowQueue,
                    onPlaySong = onPlaySong,
                    onPlayCollection = onPlayCollection
                )
            }
            null -> SearchPage(viewModel = searchViewModel, onSongClick = onPlaySong, onAlbumClick = { onOpenDetail(DetailDestination.Album(it)) }, onArtistClick = { onOpenDetail(DetailDestination.Artist(it)) }, onPlaylistClick = { onOpenDetail(DetailDestination.Playlist(it)) }, onBackToBiblioteca = onBackToBiblioteca, onViewQueue = onShowQueue)
        }
    }
}

@Composable
private fun PlayerDock(
    playbackViewModel: PlaybackViewModel, 
    searchViewModel: SearchViewModel, 
    animationsViewModel: com.soundly.ui.screens.settings.pages.AnimationsViewModel, 
    pagerState: androidx.compose.foundation.pager.PagerState,
    onPlayPause: () -> Unit, 
    onSearchToggle: () -> Unit, 
    onMiniPlayerClick: () -> Unit, 
    miniPlayerModifier: Modifier, 
    modifier: Modifier = Modifier,
    onPrevious: () -> Unit,
    showHomePage: Boolean,
    miniPlayerMetadata: MiniPlayerMetadata,
    miniPlayerProgress: () -> Float
) {
    DebugRecompose("PlayerDock", logEvery = 20)
    
    // REFACTOR: Recolectar estados aquí para evitar recomponer HomeScreen
    val miniPlayerStyle by animationsViewModel.miniPlayerStyle.collectAsStateWithLifecycle()
    val miniArtworkShape by animationsViewModel.miniArtworkShape.collectAsStateWithLifecycle()
    val miniProgressBarType by animationsViewModel.miniProgressBarType.collectAsStateWithLifecycle()
    val miniProgressBarThickness by animationsViewModel.miniProgressBarThickness.collectAsStateWithLifecycle()
    val showMiniPrevious by animationsViewModel.showMiniPrevious.collectAsStateWithLifecycle()
    val swipeToDismiss by animationsViewModel.swipeToDismiss.collectAsStateWithLifecycle()
    val vividColors by animationsViewModel.vividColors.collectAsStateWithLifecycle()

    val playerState by playbackViewModel.uiState.collectAsStateWithLifecycle()
    val dominantColor = rememberDominantColor(playerState.artworkUri)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accentColor = adaptDominantInstant(rawColor = dominantColor, isDarkTheme = isDark, fallback = MaterialTheme.colorScheme.surface)
    val hasTrack = playerState.title.isNotBlank() || playerState.durationMs > 0

    SoundlyNavStack(
        pagerState = pagerState, miniPlayerMetadata = miniPlayerMetadata, miniPlayerProgress = miniPlayerProgress, onPlayPause = onPlayPause, onNext = { playbackViewModel.onNext() }, onSearchToggle = onSearchToggle, onMiniPlayerClick = onMiniPlayerClick,
        miniPlayerModifier = miniPlayerModifier, accentColor = accentColor, showMini = hasTrack, searchViewModel = searchViewModel, miniPlayerStyle = miniPlayerStyle, modifier = modifier,
        artworkShape = miniArtworkShape, miniProgressBarType = miniProgressBarType, 
        miniProgressBarThickness = miniProgressBarThickness, showMiniPrevious = showMiniPrevious,
        swipeToDismiss = swipeToDismiss, onPrevious = onPrevious,
        vividColors = vividColors,
        onDismiss = { playbackViewModel.stop() },
        showHomePage = showHomePage
    )
}
