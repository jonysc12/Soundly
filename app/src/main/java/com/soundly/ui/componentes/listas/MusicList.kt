package com.soundly.ui.componentes.listas

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundly.R
import com.soundly.data.model.Album
import com.soundly.data.model.Artist

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Representa una canción con opción de carátula real o drawable temporal.
 */
data class Cancion(
    val caratulaUri: Uri? = null, // Carátula real de MediaStore
    val caratulaRes: Int? = null, // Drawable temporal si no hay URI
    val titulo: String,
    val artista: String
)

@Composable
fun ItemCancion(
    cancion: Cancion,
    onClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    menuContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // -----------------------------
        // Imagen de la canción
        // -----------------------------
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(cancion.caratulaUri)
                .crossfade(true)
                .build(),
            placeholder = painterResource(id = R.drawable.carga),
            error = painterResource(id = R.drawable.carga),
            contentDescription = "Carátula de la canción",
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // -----------------------------
        // Título y artista
        // -----------------------------
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cancion.titulo,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = cancion.artista,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // -----------------------------
        // Menú lateral
        // -----------------------------
        if (menuContent != null) {
            menuContent()
        } else {
            IconButton(onClick = { onMenuClick() }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = "Más opciones"
                )
            }
        }
    }
}

@Composable
fun ItemAlbum(
    album: Album,
    caratulaUri: Uri,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
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
                .fillMaxWidth()
                .aspectRatio(1f) // <- Fuerza cuadrado
                .clip(RoundedCornerShape(22.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 4.dp),

            ) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MiniCapsule(
                    icon = Icons.Rounded.MusicNote,
                    text = album.songCount.toString()
                )


            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ItemArtista(
    artist: Artist,
    caratulaUri: Uri?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
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
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialShapes.Cookie4Sided.toShape()), // 👈 FIX
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp) // 👈 aquí
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),

        ) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MiniCapsule(
                    icon = Icons.Rounded.MusicNote,
                    text = artist.songCount.toString()
                )

                MiniCapsule(
                    icon = Icons.Rounded.Album,
                    text = artist.albumCount.toString() // 👈 aquí
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ItemArtistaList(
    artist: Artist,
    caratulaUri: Uri?,
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
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(caratulaUri)
                .crossfade(true)
                .build(),
            placeholder = painterResource(id = R.drawable.carga),
            error = painterResource(id = R.drawable.carga),
            contentDescription = "Imagen del artista",
            modifier = Modifier
                .size(68.dp)
                .clip(MaterialShapes.Cookie6Sided.toShape()),
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
                    text = artist.songCount.toString()
                )
                MiniCapsule(
                    icon = Icons.Rounded.Album,
                    text = artist.albumCount.toString()
                )
            }
        }
    }
}

@Composable
fun MiniCapsule(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
@Composable
fun ItemCancionAlbum(
    cancion: Cancion,
    trackNumber: Int? = null, // ← número opcional
    onClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    menuContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // -----------------------------
        // Número contador
        // -----------------------------
        if (trackNumber != null) {
            Text(
                text = trackNumber.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(12.dp))
        }

        // -----------------------------
        // Imagen de la canción
        // -----------------------------
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(cancion.caratulaUri)
                .crossfade(true)
                .build(),
            placeholder = painterResource(id = R.drawable.carga),
            error = painterResource(id = R.drawable.carga),
            contentDescription = "Carátula de la canción",
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // -----------------------------
        // Título y artista
        // -----------------------------
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cancion.titulo,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = cancion.artista,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // -----------------------------
        // Menú lateral
        // -----------------------------
        if (menuContent != null) {
            menuContent()
        } else {
            IconButton(onClick = { onMenuClick() }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = "Más opciones"
                )
            }
        }
    }
}

