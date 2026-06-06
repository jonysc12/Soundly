package com.soundly.feature.biblioteca.pages

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soundly.data.model.Album
import com.soundly.ui.componentes.listas.ItemBibliotecaAlbumList

@Composable
fun AlbumsListPage(
    albums: List<Album>,
    albumArtProvider: (Long) -> Uri,
    onAlbumClick: (Long) -> Unit,
    onAlbumLongClick: (Album) -> Unit = {},
    pinnedAlbums: Set<String> = emptySet()
) {
    if (albums.isEmpty()) {
        BibliotecaEmptyState(
            title = "Sin álbumes favoritos",
            message = "Los álbumes que marques desde Library se guardarán aquí."
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            ItemBibliotecaAlbumList(
                album = album,
                caratulaUri = albumArtProvider(album.id),
                onClick = { onAlbumClick(album.id) },
                onLongClick = { onAlbumLongClick(album) },
                isPinned = album.id.toString() in pinnedAlbums
            )
        }
    }
}
