package com.soundly.feature.biblioteca.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.soundly.R
import com.soundly.data.model.Playlist
import com.soundly.ui.componentes.listas.ItemBibliotecaCreatePlaylist
import com.soundly.ui.componentes.listas.ItemBibliotecaPlaylistList

@Composable
fun PlaylistsListPage(
    playlists: List<Playlist>,
    onPlaylistClick: (String) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onPlaylistLongClick: (Playlist) -> Unit = {},
    pinnedPlaylists: Set<String> = emptySet()
) {
    val navStackHeight = com.soundly.ui.componentes.LocalNavStackHeight.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(bottom = navStackHeight + 16.dp)
    ) {
        item(key = "create_playlist") {
            ItemBibliotecaCreatePlaylist(onClick = onCreatePlaylistClick)
        }

        if (playlists.isEmpty()) {
            item(key = "empty_state") {
                BibliotecaInlineEmptyState(
                    title = stringResource(R.string.playlists_empty_title),
                    message = stringResource(R.string.playlists_empty_message)
                )
            }
        }

        items(playlists, key = { it.id }) { playlist ->
            ItemBibliotecaPlaylistList(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist.id) },
                onLongClick = { onPlaylistLongClick(playlist) }
            )
        }
    }
}
