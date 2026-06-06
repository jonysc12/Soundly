package com.soundly.feature.biblioteca.pages

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soundly.data.model.Album
import com.soundly.data.model.Artist
import com.soundly.data.model.Playlist
import com.soundly.data.model.FolderSummary
import com.soundly.ui.componentes.listas.ItemBibliotecaAlbumList
import com.soundly.ui.componentes.listas.ItemBibliotecaArtistList
import com.soundly.ui.componentes.listas.ItemBibliotecaCreatePlaylist
import com.soundly.ui.componentes.listas.ItemBibliotecaPlaylistList
import com.soundly.ui.componentes.listas.ItemFolderList
import com.soundly.ui.componentes.listas.ItemFolderListTodo

@Composable
fun TodoPage(
    playlists: List<Playlist>,
    albums: List<Album>,
    artists: List<Artist>,
    favoriteFolders: List<FolderSummary> = emptyList(),
    pinnedPlaylists: Set<String> = emptySet(),
    pinnedAlbums: Set<String> = emptySet(),
    pinnedArtists: Set<String> = emptySet(),
    pinnedFolders: Set<String> = emptySet(),
    albumArtProvider: (Long) -> Uri,
    artistArtProvider: (Long) -> Uri?,
    onPlaylistClick: (String) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (Long) -> Unit,
    onFolderClick: (String) -> Unit = {},
    onCreatePlaylistClick: () -> Unit,
    onPlaylistLongClick: (Playlist) -> Unit = {},
    onAlbumLongClick: (Album) -> Unit = {},
    onArtistLongClick: (Artist) -> Unit = {},
    onFolderLongClick: (FolderSummary) -> Unit = {}
) {
    val isEmpty = playlists.isEmpty() && albums.isEmpty() && artists.isEmpty() && favoriteFolders.isEmpty()

    // Separate pinned and unpinned
    val pPlaylists = playlists.filter { it.id in pinnedPlaylists }
    val uPlaylists = playlists.filter { it.id !in pinnedPlaylists }

    val pFolders = favoriteFolders.filter { it.path in pinnedFolders }
    val uFolders = favoriteFolders.filter { it.path !in pinnedFolders }

    val pAlbums = albums.filter { it.id.toString() in pinnedAlbums }
    val uAlbums = albums.filter { it.id.toString() !in pinnedAlbums }

    val pArtists = artists.filter { it.id.toString() in pinnedArtists }
    val uArtists = artists.filter { it.id.toString() !in pinnedArtists }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item(key = "create_playlist") {
            ItemBibliotecaCreatePlaylist(onClick = onCreatePlaylistClick)
        }

        if (isEmpty) {
            item(key = "empty_state") {
                BibliotecaInlineEmptyState(
                    title = "Tu biblioteca está vacía",
                    message = "Empieza creando una playlist o marcando contenido como favorito para verlo aquí."
                )
            }
        }

        // --- SECTION: ALL PINNED ITEMS ---
        items(pPlaylists, key = { "pinned_playlist_${it.id}" }) { playlist ->
            ItemBibliotecaPlaylistList(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist.id) },
                onLongClick = { onPlaylistLongClick(playlist) },
                isPinned = true
            )
        }

        items(pFolders, key = { "pinned_folder_${it.path}" }) { folder ->
            ItemFolderListTodo(
                folderName = folder.name,
                songCount = folder.songCount,
                onClick = { onFolderClick(folder.path) },
                onLongClick = { onFolderLongClick(folder) },
                isPinned = true
            )
        }

        items(pAlbums, key = { "pinned_album_${it.id}" }) { album ->
            ItemBibliotecaAlbumList(
                album = album,
                caratulaUri = albumArtProvider(album.id),
                onClick = { onAlbumClick(album.id) },
                onLongClick = { onAlbumLongClick(album) },
                isPinned = true
            )
        }

        items(pArtists, key = { "pinned_artist_${it.id}" }) { artist ->
            ItemBibliotecaArtistList(
                artist = artist,
                caratulaUri = artistArtProvider(artist.id),
                onClick = { onArtistClick(artist.id) },
                onLongClick = { onArtistLongClick(artist) },
                isPinned = true
            )
        }

        // --- SECTION: UNPINNED ITEMS ---
        items(uPlaylists, key = { "playlist_${it.id}" }) { playlist ->
            ItemBibliotecaPlaylistList(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist.id) },
                onLongClick = { onPlaylistLongClick(playlist) },
                isPinned = false
            )
        }

        items(uFolders, key = { "folder_${it.path}" }) { folder ->
            ItemFolderListTodo(
                folderName = folder.name,
                songCount = folder.songCount,
                onClick = { onFolderClick(folder.path) },
                onLongClick = { onFolderLongClick(folder) },
                isPinned = false
            )
        }

        items(uAlbums, key = { "album_${it.id}" }) { album ->
            ItemBibliotecaAlbumList(
                album = album,
                caratulaUri = albumArtProvider(album.id),
                onClick = { onAlbumClick(album.id) },
                onLongClick = { onAlbumLongClick(album) },
                isPinned = false
            )
        }

        items(uArtists, key = { "artist_${it.id}" }) { artist ->
            ItemBibliotecaArtistList(
                artist = artist,
                caratulaUri = artistArtProvider(artist.id),
                onClick = { onArtistClick(artist.id) },
                onLongClick = { onArtistLongClick(artist) },
                isPinned = false
            )
        }
    }
}

@Composable
internal fun BibliotecaEmptyState(
    title: String,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun BibliotecaInlineEmptyState(
    title: String,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
