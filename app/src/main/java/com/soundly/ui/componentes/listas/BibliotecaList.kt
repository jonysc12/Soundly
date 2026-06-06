package com.soundly.ui.componentes.listas

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundly.R
import com.soundly.data.model.Album
import com.soundly.data.model.Artist
import com.soundly.data.model.Playlist
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.rounded.PushPin

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun ItemBibliotecaArtistList(
    artist: Artist,
    caratulaUri: Uri?,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isPinned: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(caratulaUri)
                .crossfade(true)
                .build(),
            placeholder = painterResource(id = R.drawable.carga),
            error = painterResource(id = R.drawable.carga),
            contentDescription = "Imagen del artista",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniCapsule(
                    icon = Icons.Rounded.MusicNote,
                    text = "${artist.songCount} " + if (artist.songCount == 1) "canción" else "canciones"
                )
                MiniCapsule(
                    icon = Icons.Rounded.Album,
                    text = "${artist.albumCount} " + if (artist.albumCount == 1) "álbum" else "álbumes"
                )
            }
        }
        if (isPinned) {
            Icon(
                imageVector = Icons.Rounded.PushPin,
                contentDescription = "Fijado",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemBibliotecaAlbumList(
    album: Album,
    caratulaUri: Uri,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isPinned: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(caratulaUri)
                .crossfade(true)
                .build(),
            placeholder = painterResource(id = R.drawable.carga),
            error = painterResource(id = R.drawable.carga),
            contentDescription = "Carátula del álbum",
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(22.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = album.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniCapsule(
                    icon = Icons.Rounded.MusicNote,
                    text = "${album.songCount} " + if (album.songCount == 1) "canción" else "canciones"
                )
                MiniCapsule(
                    icon = Icons.Rounded.Album,
                    text = "Álbum"
                )
            }
        }
        if (isPinned) {
            Icon(
                imageVector = Icons.Rounded.PushPin,
                contentDescription = "Fijado",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemBibliotecaPlaylistList(
    playlist: Playlist,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isPinned: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = if (playlist.isAutoGenerated) {
                R.drawable.playlist_favicon
            } else {
                playlist.artworkUri ?: R.drawable.playlist_favicon
            },
            placeholder = painterResource(id = R.drawable.playlist_favicon),
            error = painterResource(id = R.drawable.playlist_favicon),
            contentDescription = "Playlist",
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(22.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (playlist.isAutoGenerated) "Colección automática de likes" else "Playlist",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniCapsule(
                    icon = Icons.Rounded.MusicNote,
                    text = "${playlist.songCount} " + if (playlist.songCount == 1) "canción" else "canciones"
                )
                MiniCapsule(
                    icon = Icons.Rounded.Album,
                    text = if (playlist.isAutoGenerated) "Autogenerada" else "Playlist"
                )
            }
        }
        if (isPinned) {
            Icon(
                imageVector = Icons.Rounded.PushPin,
                contentDescription = "Fijado",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ItemBibliotecaCreatePlaylist(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Crear playlist",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Nueva playlist",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Crea una colección con tu propia portada",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            MiniCapsule(
                icon = Icons.Rounded.Add,
                text = "Playlist"
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemFolderList(
    folderName: String,
    songCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isPinned: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)) // 👈 primero clip
            .background(MaterialTheme.colorScheme.surfaceVariant) // 👈 luego background
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ){
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = "Carpeta",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folderName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            MiniCapsule(
                icon = Icons.Rounded.MusicNote,
                text = "$songCount " + if (songCount == 1) "canción" else "canciones"
            )
        }

        if (isPinned) {
            Icon(
                imageVector = Icons.Rounded.PushPin,
                contentDescription = "Fijado",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemFolderListTodo(
    folderName: String,
    songCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isPinned: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)) // 👈 primero clip
             // 👈 luego background
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ){
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = "Carpeta",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folderName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            MiniCapsule(
                icon = Icons.Rounded.MusicNote,
                text = "$songCount " + if (songCount == 1) "canción" else "canciones"
            )
        }

        if (isPinned) {
            Icon(
                imageVector = Icons.Rounded.PushPin,
                contentDescription = "Fijado",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

