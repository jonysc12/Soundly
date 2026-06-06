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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soundly.R
import com.soundly.data.model.Playlist
import com.soundly.feature.biblioteca.pages.AlbumsListPage
import com.soundly.feature.biblioteca.pages.ArtistsListPage
import com.soundly.feature.biblioteca.pages.FoldersListPage
import com.soundly.feature.biblioteca.pages.PlaylistsListPage
import com.soundly.feature.biblioteca.pages.TodoPage
import com.soundly.feature.library.LibraryViewModel
import com.soundly.feature.library.pages.AlbumDetailScreen
import com.soundly.feature.library.pages.ArtistDetailScreen
import com.soundly.ui.componentes.LibraryFilter
import com.soundly.ui.componentes.OptionPillsRow

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
    isHostPageVisible: Boolean = true,
    onPlaySong: (com.soundly.data.model.Song, List<com.soundly.data.model.Song>) -> Unit = { _, _ -> },
    onPlayCollection: (List<com.soundly.data.model.Song>, Boolean) -> Unit = { _, _ -> }
) {
    val detailStack = rememberSaveable(saver = bibliotecaDetailStackSaver) {
        mutableStateListOf<BibliotecaDetailDestination>()
    }
    val currentDetail = detailStack.lastOrNull()
    val isDetailVisible = currentDetail != null
    val isArtistDetailVisible = currentDetail is BibliotecaDetailDestination.ArtistDetail

    val playlists by viewModel.playlists.collectAsState()
    val userImageUri by viewModel.userImageUri.collectAsState()
    val userName by viewModel.userName.collectAsState()

    val openDetail: (BibliotecaDetailDestination) -> Unit = remember(detailStack) {
        { destination ->
            if (detailStack.lastOrNull() != destination) {
                detailStack.add(destination)
            }
        }
    }
    val popDetail: () -> Unit = remember(detailStack) {
        {
            if (detailStack.isNotEmpty()) {
                detailStack.removeAt(detailStack.lastIndex)
            }
        }
    }

    LaunchedEffect(isDetailVisible) {
        onDetailVisibilityChanged(isDetailVisible)
    }
    LaunchedEffect(isArtistDetailVisible) {
        onArtistEdgeToEdgeChanged(isArtistDetailVisible)
    }

    DisposableEffect(Unit) {
        onDispose {
            onDetailVisibilityChanged(false)
            onArtistEdgeToEdgeChanged(false)
        }
    }

    BackHandler(enabled = isDetailVisible, onBack = popDetail)

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
                onPlaySong = onPlaySong,
                onPlayCollection = onPlayCollection
            )

            is BibliotecaDetailDestination.ArtistDetail -> ArtistDetailScreen(
                artistId = destination.id,
                viewModel = libraryViewModel,
                onAlbumClick = { openDetail(BibliotecaDetailDestination.AlbumDetail(it)) },
                onArtistClick = { openDetail(BibliotecaDetailDestination.ArtistDetail(it)) },
                onBack = popDetail,
                applySystemBarStyle = isHostPageVisible,
                onPlaySong = onPlaySong,
                onPlayCollection = onPlayCollection
            )

            is BibliotecaDetailDestination.PlaylistDetail -> {
                val playlist = playlists.firstOrNull { it.id == destination.id }
                val songs by viewModel.getSongsForPlaylist(destination.id).collectAsState(initial = emptyList())

                if (playlist != null) {
                    AlbumDetailScreen(
                        playlist = playlist,
                        songs = songs,
                        ownerName = userName,
                        viewModel = libraryViewModel,
                        ownerImageUri = userImageUri,
                        albumArtProvider = viewModel::getAlbumArtUri,
                        onAlbumClick = { openDetail(BibliotecaDetailDestination.AlbumDetail(it)) },
                        onArtistClick = { openDetail(BibliotecaDetailDestination.ArtistDetail(it)) },
                        onBack = popDetail,
                        onPlaySong = onPlaySong,
                        onPlayCollection = onPlayCollection
                    )
                }
            }

            is BibliotecaDetailDestination.FolderDetail -> {
                AlbumDetailScreen(
                    folderPath = destination.path,
                    viewModel = viewModel,
                    libraryViewModel = libraryViewModel,
                    onAlbumClick = { openDetail(BibliotecaDetailDestination.AlbumDetail(it)) },
                    onArtistClick = { openDetail(BibliotecaDetailDestination.ArtistDetail(it)) },
                    onBack = popDetail,
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
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val albums by viewModel.favoriteAlbums.collectAsState()
    val artists by viewModel.favoriteArtists.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val favoriteFolderPaths by viewModel.favoriteFolderPaths.collectAsState()
    val favoriteFolders = remember(folders, favoriteFolderPaths) {
        folders.filter { it.path in favoriteFolderPaths }
    }
    
    val showCreatePlaylistSheet = rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val showEditPlaylistSheet = remember { androidx.compose.runtime.mutableStateOf(false) }
    val editingPlaylist = remember { androidx.compose.runtime.mutableStateOf<Playlist?>(null) }
    
    val showMenu = remember { androidx.compose.runtime.mutableStateOf(false) }
    val menuData = remember { androidx.compose.runtime.mutableStateOf<LibraryItemMenuData?>(null) }

    val pinnedPlaylists by viewModel.pinnedPlaylistIds.collectAsState()
    val pinnedAlbums by viewModel.pinnedAlbumIds.collectAsState()
    val pinnedArtists by viewModel.pinnedArtistIds.collectAsState()
    val pinnedFolders by viewModel.pinnedFolderPaths.collectAsState()

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
        onCreatePlaylist = viewModel::createPlaylist
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
                    menuData.value = LibraryItemMenuData(
                        title = playlist.name,
                        subtitle = if (playlist.isAutoGenerated) "Colección automática" else "${playlist.songCount} canciones",
                        artworkUri = if (playlist.isAutoGenerated) R.drawable.playlist_favicon else playlist.artworkUri,
                        type = if (playlist.isAutoGenerated) LibraryItemMenuType.AutoPlaylist(playlist.id) else LibraryItemMenuType.UserPlaylist(playlist.id),
                        isPinned = playlist.id in pinnedPlaylists
                    )
                    showMenu.value = true
                },
                onAlbumLongClick = { album ->
                    menuData.value = LibraryItemMenuData(
                        title = album.name,
                        subtitle = "${album.artist} • ${album.songCount} canciones",
                        artworkUri = viewModel.getAlbumArtUri(album.id),
                        type = LibraryItemMenuType.Album(album.id),
                        isPinned = album.id.toString() in pinnedAlbums
                    )
                    showMenu.value = true
                },
                onArtistLongClick = { artist ->
                    menuData.value = LibraryItemMenuData(
                        title = artist.name,
                        subtitle = "Artista • ${artist.songCount} canciones",
                        artworkUri = viewModel.getArtistArtUri(artist.id),
                        type = LibraryItemMenuType.Artist(artist.id),
                        isPinned = artist.id.toString() in pinnedArtists
                    )
                    showMenu.value = true
                },
                onFolderLongClick = { folder ->
                    menuData.value = LibraryItemMenuData(
                        title = folder.name,
                        subtitle = "Carpeta • ${folder.songCount} canciones",
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
                    menuData.value = LibraryItemMenuData(
                        title = playlist.name,
                        subtitle = if (playlist.isAutoGenerated) "Colección automática" else "${playlist.songCount} canciones",
                        artworkUri = if (playlist.isAutoGenerated) R.drawable.playlist_favicon else playlist.artworkUri,
                        type = if (playlist.isAutoGenerated) LibraryItemMenuType.AutoPlaylist(playlist.id) else LibraryItemMenuType.UserPlaylist(playlist.id),
                        isPinned = playlist.id in pinnedPlaylists
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
                        subtitle = "${album.artist} • ${album.songCount} canciones",
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
                        subtitle = "Artista • ${artist.songCount} canciones",
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
                        subtitle = "Carpeta • ${folder.songCount} canciones",
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
