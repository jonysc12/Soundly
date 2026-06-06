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
import com.soundly.data.model.Artist
import com.soundly.ui.componentes.listas.ItemBibliotecaArtistList

@Composable
fun ArtistsListPage(
    artists: List<Artist>,
    artistArtProvider: (Long) -> Uri?,
    onArtistClick: (Long) -> Unit,
    onArtistLongClick: (Artist) -> Unit = {},
    pinnedArtists: Set<String> = emptySet()
) {
    if (artists.isEmpty()) {
        BibliotecaEmptyState(
            title = "Sin artistas favoritos",
            message = "Los artistas que añadas desde Library aparecerán aquí."
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(artists, key = { it.id }) { artist ->
            ItemBibliotecaArtistList(
                artist = artist,
                caratulaUri = artistArtProvider(artist.id),
                onClick = { onArtistClick(artist.id) },
                onLongClick = { onArtistLongClick(artist) },
                isPinned = artist.id.toString() in pinnedArtists
            )
        }
    }
}
