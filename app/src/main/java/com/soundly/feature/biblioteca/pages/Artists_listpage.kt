package com.soundly.feature.biblioteca.pages

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.soundly.R
import com.soundly.data.model.Artist
import com.soundly.player.ArtistUiState
import com.soundly.ui.componentes.listas.ItemBibliotecaArtistList

@Composable
fun ArtistsListPage(
    artists: List<Artist>,
    artistArtProvider: (Long) -> Uri?,
    onArtistClick: (Long) -> Unit,
    onArtistLongClick: (Artist) -> Unit = {},
    pinnedArtists: Set<String> = emptySet(),
    featuredArtistInfo: ArtistUiState = ArtistUiState()
) {
    val navStackHeight = com.soundly.ui.componentes.LocalNavStackHeight.current
    if (artists.isEmpty()) {
        BibliotecaEmptyState(
            title = stringResource(R.string.artists_empty_title),
            message = stringResource(R.string.artists_empty_message)
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(bottom = navStackHeight + 16.dp)
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

        if (featuredArtistInfo.name.isNotBlank() && !featuredArtistInfo.isLoading) {
            item {
                Spacer(Modifier.height(32.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(24.dp))
                
                FullArtistBioView(info = featuredArtistInfo)
                
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun FullArtistBioView(info: ArtistUiState) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(info.imageUrl)
                        .crossfade(true)
                        .allowHardware(true)
                        .build(),
                    contentDescription = info.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.info_bio_prefix),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = info.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = info.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (isExpanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp
        )
        
        if (info.description.length > 200) {
            Text(
                text = if (isExpanded) stringResource(R.string.button_see_less) else stringResource(R.string.button_see_more_bio),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable { isExpanded = !isExpanded }
            )
        }
    }
}
