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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundly.R
import com.soundly.data.model.Artist

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeItemCancion(
    cancion: Cancion,
    onClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    menuContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                .clip(MaterialShapes.Cookie4Sided.toShape()),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(4.dp))

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
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeItemArtistaList(
    artist: Artist,
    caratulaUri: Uri?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.Start
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
                .clip(MaterialShapes.Arch.toShape()),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
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
