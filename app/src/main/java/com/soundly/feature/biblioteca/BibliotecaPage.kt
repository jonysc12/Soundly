package com.soundly.feature.biblioteca

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soundly.R
import com.soundly.data.model.Playlist
import kotlinx.coroutines.delay
import androidx.compose.runtime.produceState
import androidx.compose.foundation.layout.Box
import com.soundly.feature.biblioteca.pages.AlbumsListPage
import com.soundly.feature.biblioteca.pages.ArtistsListPage
import com.soundly.feature.biblioteca.pages.FoldersListPage
import com.soundly.feature.biblioteca.pages.PlaylistsListPage
import com.soundly.feature.biblioteca.pages.TodoPage
import com.soundly.feature.library.LibraryViewModel
import com.soundly.feature.library.pages.AlbumDetailScreen
import com.soundly.feature.library.pages.ArtistDetailScreen
import com.soundly.feature.library.pages.FolderDetailScreen
import com.soundly.feature.library.pages.PlaylistDetailScreen
import com.soundly.ui.componentes.LibraryFilter
import com.soundly.ui.componentes.OptionPillsRow
import com.soundly.ui.navigation.LocalBackStackCoordinator

private sealed interface BibliotecaDetailDestination {
    data class PlaylistDetail(val id: String) : BibliotecaDetailDestination
    data class AlbumDetail(val id: Long) : BibliotecaDetailDestination
    data class ArtistDetail(val id: Long) : BibliotecaDetailDestination
    data class FolderDetail(val path: String) : BibliotecaDetailDestination
}

private fun BibliotecaDetailDestination.toPersistedValue(): String = when (this) {
    is BibliotecaDetailDestination.PlaylistDetail -> "playlist:$id"
    is BibliotecaDetailDestination.AlbumDetail -> "album:$id"
    is BibliotecaDetailDestination.ArtistDetail -> "artist:$id"
    is BibliotecaDetailDestination.FolderDetail -> "folder:$path"
}

private fun persistedValueToBibliotecaDetail(value: String): BibliotecaDetailDestination? {
    val parts = value.split(":", limit = 2)
    if (parts.size != 2) return null
    return when (parts[0]) {
        "playlist" -> BibliotecaDetailDestination.PlaylistDetail(parts[1])
        "album" -> parts[1].toLongOrNull()?.let(BibliotecaDetailDestination::AlbumDetail)
        "artist" -> parts[1].toLongOrNull()?.let(BibliotecaDetailDestination::ArtistDetail)
        "folder" -> BibliotecaDetailDestination.FolderDetail(parts[1])
        else -> null
    }
}

private val bibliotecaDetailStackSaver = listSaver<SnapshotStateList<BibliotecaDetailDestination>, String>(
    save = { stack -> stack.map(BibliotecaDetailDestination::toPersistedValue) },
    restore = { values ->
        mutableStateListOf<BibliotecaDetailDestination>().apply {
            values.mapNotNullTo(this, ::persistedValueToBibliotecaDetail)
        }
    }
)

@Composable
fun BibliotecaPage(
    viewModel: BibliotecaViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    onDetailVisibilityChanged: (Boolean) -> Unit = {},
    onArtistEdgeToEdgeChanged: (Boolean) -> Unit = {},
    onPlaySong: (com.soundly.data.model.Song, List<com.soundly.data.model.Song>) -> Unit = { _, _ -> },
    onPlayCollection: (List<com.soundly.data.model.Song>, Boolean) -> Unit = { _, _ -> },
    onViewQueue: () -> Unit = {},
    externalArtistRequest: Long? = null,
    onExternalRequestConsumed: () -> Unit = {},
    onCurrentArtistChanged: (Long?) -> Unit = {},
    isHostPageVisible: Boolean = true
) {
    val detailStack = rememberSaveable(saver = bibliotecaDetailStackSaver) {
        mutableStateListOf<BibliotecaDetailDestination>()
    }
    val currentDetail = detailStack.lastOrNull()

    val onDetailVisibilityChangedStable = remember(onDetailVisibilityChanged) { { v: Boolean -> onDetailVisibilityChanged(v) } }
    val onArtistEdgeToEdgeChangedStable = remember(onArtistEdgeToEdgeChanged) { { v: Boolean -> onArtistEdgeToEdgeChanged(v) } }
    val onPlaySongStable = remember(onPlaySong) { { s: com.soundly.data.model.Song, q: List<com.soundly.data.model.Song> -> onPlaySong(s, q) } }
    val onPlayCollectionStable = remember(onPlayCollection) { { q: List<com.soundly.data.model.Song>, b: Boolean -> onPlayCollection(q, b) } }
    val onViewQueueStable = remember(onViewQueue) { { onViewQueue() } }

    LaunchedEffect(currentDetail) {
        onCurrentArtistChanged((currentDetail as? BibliotecaDetailDestination.ArtistDetail)?.id)
    }

    val isDetailVisible = currentDetail != null
    val isArtistDetailVisible = currentDetail is BibliotecaDetailDestination.ArtistDetail

    val openDetail: (BibliotecaDetailDestination) -> Unit = remember(detailStack) {
        { destination ->
            if (detailStack.lastOrNull() != destination) {
                detailStack.add(destination)
            }
        }
    }

    LaunchedEffect(externalArtistRequest) {
        externalArtistRequest?.let {
            openDetail(BibliotecaDetailDestination.ArtistDetail(it))
            onExternalRequestConsumed()
        }
    }

    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val userImageUri by viewModel.userImageUri.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val popDetail: () -> Unit = {
        if (detailStack.isNotEmpty()) {
            detailStack.removeAt(detailStack.lastIndex)
        }
    }

    LaunchedEffect(isDetailVisible) {
        onDetailVisibilityChangedStable(isDetailVisible)
    }
    LaunchedEffect(isArtistDetailVisible) {
        onArtistEdgeToEdgeChangedStable(isArtistDetailVisible)
    }

    val backStackCoordinator = LocalBackStackCoordinator.current
    val backHandlerEnabled = detailStack.isNotEmpty() && isHostPageVisible && !backStackCoordinator.isOverlayActive

    DisposableEffect(Unit) {
        onDispose {
            onDetailVisibilityChangedStable(false)
            onArtistEdgeToEdgeChangedStable(false)
        }
    }

    BackHandler(enabled = backHandlerEnabled, onBack = popDetail)

    AnimatedContent(
        targetState = currentDetail,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "BibliotecaContent"
    ) { destination ->
        when (destination) {
            is BibliotecaDetailDestination.AlbumDetail -> AlbumDetailScreen(
                albumId = destination.id,
                viewModel = libraryViewModel,
                onAlbumClick = { openDetail(BibliotecaDetailDestination.AlbumDetail(it)) },
                onArtistClick = { openDetail(BibliotecaDetailDestination.ArtistDetail(it)) },
                onBack = popDetail,
                onViewQueue = onViewQueueStable,
                onPlaySong = onPlaySongStable,
                onPlayCollection = onPlayCollectionStable
            )

            is BibliotecaDetailDestination.ArtistDetail -> ArtistDetailScreen(
                artistId = destination.id,
                viewModel = libraryViewModel,
                onAlbumClick = { openDetail(BibliotecaDetailDestination.AlbumDetail(it)) },
                onArtistClick = { openDetail(BibliotecaDetailDestination.ArtistDetail(it)) },
                onBack = popDetail,
                onViewQueue = onViewQueueStable,
                onPlaySong = onPlaySongStable,
                onPlayCollection = onPlayCollectionStable
            )

            is BibliotecaDetailDestination.PlaylistDetail -> {
                val playlist = playlists.firstOrNull { it.id == destination.id }
                val songs by viewModel.getSongsForPlaylist(destination.id).collectAsState(initial = emptyList())

                if (playlist != null) {
                    PlaylistDetailScreen(
                        playlist = playlist,
                        songs = songs,
                        ownerName = userName,
                        viewModel = libraryViewModel,
                        ownerImageUri = userImageUri,
                        albumArtProvider = viewModel::getAlbumArtUri,
                        onAlbumClick = { openDetail(BibliotecaDetailDestination.AlbumDetail(it)) },
                        onArtistClick = { openDetail(BibliotecaDetailDestination.ArtistDetail(it)) },
                        onBack = popDetail,
                        onViewQueue = onViewQueue,
                        onPlaySong = onPlaySong,
                        onPlayCollection = onPlayCollection
                    )
                }
            }

            is BibliotecaDetailDestination.FolderDetail -> {
                FolderDetailScreen(
                    folderPath = destination.path,
                    viewModel = viewModel,
                    libraryViewModel = libraryViewModel,
                    onAlbumClick = { openDetail(BibliotecaDetailDestination.AlbumDetail(it)) },
                    onArtistClick = { openDetail(BibliotecaDetailDestination.ArtistDetail(it)) },
                    onBack = popDetail,
                    onViewQueue = onViewQueue,
                    onPlaySong = onPlaySong,
                    onPlayCollection = onPlayCollection
                )
            }

            null -> BibliotecaMainContent(
                viewModel = viewModel,
                onPlaylistClick = { openDetail(BibliotecaDetailDestination.PlaylistDetail(it)) },
                onAlbumClick = { openDetail(BibliotecaDetailDestination.AlbumDetail(it)) },
                onArtistClick = { openDetail(BibliotecaDetailDestination.ArtistDetail(it)) },
                onFolderClick = { openDetail(BibliotecaDetailDestination.FolderDetail(it)) }
            )
        }
    }
}

@Composable
private fun BibliotecaMainContent(
    viewModel: BibliotecaViewModel,
    onPlaylistClick: (String) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (Long) -> Unit,
    onFolderClick: (String) -> Unit
) {
    val context = LocalContext.current
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val albums by viewModel.favoriteAlbums.collectAsStateWithLifecycle()
    val artists by viewModel.favoriteArtists.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val favoriteFolderPaths by viewModel.favoriteFolderPaths.collectAsStateWithLifecycle()
    val favoriteFolders = remember(folders, favoriteFolderPaths) {
        folders.filter { it.path in favoriteFolderPaths }
    }
    
    val showCreatePlaylistSheet = rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val showEditPlaylistSheet = remember { androidx.compose.runtime.mutableStateOf(false) }
    val editingPlaylist = remember { androidx.compose.runtime.mutableStateOf<Playlist?>(null) }
    
    val showMenu = remember { androidx.compose.runtime.mutableStateOf(false) }
    val menuData = remember { androidx.compose.runtime.mutableStateOf<LibraryItemMenuData?>(null) }

    val pinnedPlaylists by viewModel.pinnedPlaylistIds.collectAsStateWithLifecycle()
    val pinnedAlbums by viewModel.pinnedAlbumIds.collectAsStateWithLifecycle()
    val pinnedArtists by viewModel.pinnedArtistIds.collectAsStateWithLifecycle()
    val pinnedFolders by viewModel.pinnedFolderPaths.collectAsStateWithLifecycle()

    LibraryItemMenuBottomSheet(
        visible = showMenu.value,
        data = menuData.value,
        onDismiss = { showMenu.value = false },
        onPinClick = {
            menuData.value?.let { data ->
                val typeStr = when (data.type) {
                    is LibraryItemMenuType.UserPlaylist, is LibraryItemMenuType.AutoPlaylist -> "playlist"
                    is LibraryItemMenuType.Album -> "album"
                    is LibraryItemMenuType.Artist -> "artist"
                    is LibraryItemMenuType.Folder -> "folder"
                }
                val id = when (val t = data.type) {
                    is LibraryItemMenuType.UserPlaylist -> t.id
                    is LibraryItemMenuType.AutoPlaylist -> t.id
                    is LibraryItemMenuType.Album -> t.id.toString()
                    is LibraryItemMenuType.Artist -> t.id.toString()
                    is LibraryItemMenuType.Folder -> t.path
                }
                if (typeStr.isNotEmpty() && id.isNotEmpty()) {
                    viewModel.togglePin(typeStr, id)
                }
            }
        },
        onShowOnHomeClick = {
            menuData.value?.let { data ->
                val id = when (val t = data.type) {
                    is LibraryItemMenuType.UserPlaylist -> t.id
                    is LibraryItemMenuType.AutoPlaylist -> t.id
                    else -> null
                }
                id?.let { viewModel.togglePlaylistShowOnHome(it) }
            }
        },
        onEditClick = {
            menuData.value?.let { data ->
                if (data.type is LibraryItemMenuType.UserPlaylist) {
                    editingPlaylist.value = playlists.find { it.id == data.type.id }
                    showEditPlaylistSheet.value = true
                }
            }
        },
        onDeleteClick = {
            menuData.value?.let { data ->
                when (val t = data.type) {
                    is LibraryItemMenuType.UserPlaylist -> viewModel.deletePlaylist(t.id)
                    is LibraryItemMenuType.AutoPlaylist -> { /* Cannot delete auto playlists */ }
                    is LibraryItemMenuType.Album -> viewModel.toggleAlbumFavorite(t.id)
                    is LibraryItemMenuType.Artist -> viewModel.toggleArtistFavorite(t.id)
                    is LibraryItemMenuType.Folder -> viewModel.toggleFavoriteFolder(t.path)
                }
            }
        }
    )

    CreatePlaylistBottomSheet(
        visible = showCreatePlaylistSheet.value,
        onDismiss = { showCreatePlaylistSheet.value = false },
        onCreatePlaylist = viewModel::createPlaylist,
        onImportPlaylist = viewModel::importPlaylist,
        importState = importState,
        onClearImportState = viewModel::clearImportState,
        onCreatePlaylistWithSongs = viewModel::createPlaylistWithSongs
    )

    CreatePlaylistBottomSheet(
        visible = showEditPlaylistSheet.value,
        onDismiss = { 
            showEditPlaylistSheet.value = false
            editingPlaylist.value = null
        },
        onCreatePlaylist = viewModel::createPlaylist, // unused in edit mode
        initialPlaylistId = editingPlaylist.value?.id,
        initialName = editingPlaylist.value?.name,
        initialArtworkUri = editingPlaylist.value?.artworkUri,
        onUpdatePlaylist = viewModel::updatePlaylist
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        OptionPillsRow(
            selected = selectedFilter,
            onSelect = viewModel::setSelectedFilter
        )

        Spacer(modifier = Modifier.height(20.dp))

        when (selectedFilter) {
            null -> TodoPage(
                playlists = playlists,
                albums = albums,
                artists = artists,
                favoriteFolders = favoriteFolders,
                pinnedPlaylists = pinnedPlaylists,
                pinnedAlbums = pinnedAlbums,
                pinnedArtists = pinnedArtists,
                pinnedFolders = pinnedFolders,
                albumArtProvider = viewModel::getAlbumArtUri,
                artistArtProvider = viewModel::getArtistArtUri,
                onPlaylistClick = onPlaylistClick,
                onAlbumClick = onAlbumClick,
                onArtistClick = onArtistClick,
                onFolderClick = onFolderClick,
                onCreatePlaylistClick = { showCreatePlaylistSheet.value = true },
                onPlaylistLongClick = { playlist ->
                    val playlistName = if (playlist.id == com.soundly.data.repository.MusicRepository.LIKED_SONGS_PLAYLIST_ID) {
                        context.getString(R.string.liked_songs_title)
                    } else {
                        playlist.name
                    }
                    menuData.value = LibraryItemMenuData(
                        title = playlistName,
                        subtitle = if (playlist.isAutoGenerated) context.getString(R.string.library_auto_collection) else context.getString(R.string.songs_count, playlist.songCount),
                        artworkUri = if (playlist.isAutoGenerated) R.drawable.playlist_favicon else playlist.artworkUri,
                        type = if (playlist.isAutoGenerated) LibraryItemMenuType.AutoPlaylist(playlist.id) else LibraryItemMenuType.UserPlaylist(playlist.id),
                        isPinned = playlist.id in pinnedPlaylists,
                        isFromHome = playlist.showOnHome
                    )
                    showMenu.value = true
                },
                onAlbumLongClick = { album ->
                    menuData.value = LibraryItemMenuData(
                        title = album.name,
                        subtitle = "${album.artist} • ${context.getString(R.string.songs_count, album.songCount)}",
                        artworkUri = viewModel.getAlbumArtUri(album.id),
                        type = LibraryItemMenuType.Album(album.id),
                        isPinned = album.id.toString() in pinnedAlbums
                    )
                    showMenu.value = true
                },
                onArtistLongClick = { artist ->
                    menuData.value = LibraryItemMenuData(
                        title = artist.name,
                        subtitle = "${context.getString(R.string.library_label_artist)} • ${context.getString(R.string.songs_count, artist.songCount)}",
                        artworkUri = viewModel.getArtistArtUri(artist.id),
                        type = LibraryItemMenuType.Artist(artist.id),
                        isPinned = artist.id.toString() in pinnedArtists
                    )
                    showMenu.value = true
                },
                onFolderLongClick = { folder ->
                    menuData.value = LibraryItemMenuData(
                        title = folder.name,
                        subtitle = "${context.getString(R.string.library_label_folder)} • ${context.getString(R.string.songs_count, folder.songCount)}",
                        artworkUri = null,
                        type = LibraryItemMenuType.Folder(folder.path),
                        isPinned = folder.path in pinnedFolders,
                        isFromTodo = true,
                        isFavorite = true
                    )
                    showMenu.value = true
                }
            )

            LibraryFilter.PLAYLISTS -> PlaylistsListPage(
                playlists = playlists,
                onPlaylistClick = onPlaylistClick,
                onCreatePlaylistClick = { showCreatePlaylistSheet.value = true },
                onPlaylistLongClick = { playlist ->
                    val playlistName = if (playlist.id == com.soundly.data.repository.MusicRepository.LIKED_SONGS_PLAYLIST_ID) {
                        context.getString(R.string.liked_songs_title)
                    } else {
                        playlist.name
                    }
                    menuData.value = LibraryItemMenuData(
                        title = playlistName,
                        subtitle = if (playlist.isAutoGenerated) context.getString(R.string.library_auto_collection) else context.getString(R.string.songs_count, playlist.songCount),
                        artworkUri = if (playlist.isAutoGenerated) R.drawable.playlist_favicon else playlist.artworkUri,
                        type = if (playlist.isAutoGenerated) LibraryItemMenuType.AutoPlaylist(playlist.id) else LibraryItemMenuType.UserPlaylist(playlist.id),
                        isPinned = playlist.id in pinnedPlaylists,
                        isFromHome = playlist.showOnHome
                    )
                    showMenu.value = true
                },
                pinnedPlaylists = pinnedPlaylists
            )

            LibraryFilter.ALBUMS -> AlbumsListPage(
                albums = albums,
                albumArtProvider = viewModel::getAlbumArtUri,
                onAlbumClick = onAlbumClick,
                onAlbumLongClick = { album ->
                    menuData.value = LibraryItemMenuData(
                        title = album.name,
                        subtitle = "${album.artist} • ${context.getString(R.string.songs_count, album.songCount)}",
                        artworkUri = viewModel.getAlbumArtUri(album.id),
                        type = LibraryItemMenuType.Album(album.id),
                        isPinned = album.id.toString() in pinnedAlbums
                    )
                    showMenu.value = true
                },
                pinnedAlbums = pinnedAlbums
            )

            LibraryFilter.ARTISTS -> ArtistsListPage(
                artists = artists,
                artistArtProvider = viewModel::getArtistArtUri,
                onArtistClick = onArtistClick,
                onArtistLongClick = { artist ->
                    menuData.value = LibraryItemMenuData(
                        title = artist.name,
                        subtitle = "${context.getString(R.string.library_label_artist)} • ${context.getString(R.string.songs_count, artist.songCount)}",
                        artworkUri = viewModel.getArtistArtUri(artist.id),
                        type = LibraryItemMenuType.Artist(artist.id),
                        isPinned = artist.id.toString() in pinnedArtists
                    )
                    showMenu.value = true
                },
                pinnedArtists = pinnedArtists
            )

            LibraryFilter.FOLDERS -> FoldersListPage(
                folders = folders,
                onFolderClick = onFolderClick,
                onFolderLongClick = { folder ->
                    menuData.value = LibraryItemMenuData(
                        title = folder.name,
                        subtitle = "${context.getString(R.string.library_label_folder)} • ${context.getString(R.string.songs_count, folder.songCount)}",
                        artworkUri = null,
                        type = LibraryItemMenuType.Folder(folder.path),
                        isPinned = folder.path in pinnedFolders,
                        isFromTodo = false,
                        isFavorite = folder.path in favoriteFolderPaths
                    )
                    showMenu.value = true
                },
                pinnedFolders = pinnedFolders
            )
        }
    }
}
