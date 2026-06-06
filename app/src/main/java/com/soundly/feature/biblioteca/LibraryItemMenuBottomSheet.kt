package com.soundly.feature.biblioteca

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.soundly.R

sealed class LibraryItemMenuType {
    data class UserPlaylist(val id: String) : LibraryItemMenuType()
    data class AutoPlaylist(val id: String) : LibraryItemMenuType()
    data class Album(val id: Long) : LibraryItemMenuType()
    data class Artist(val id: Long) : LibraryItemMenuType()
    data class Folder(val path: String) : LibraryItemMenuType()
}

data class LibraryItemMenuData(
    val title: String,
    val subtitle: String,
    val artworkUri: Any?, // Uri, Int (Resource), or null
    val type: LibraryItemMenuType,
    val isPinned: Boolean,
    val isFromTodo: Boolean = false,
    val isFavorite: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryItemMenuBottomSheet(
    visible: Boolean,
    data: LibraryItemMenuData?,
    onDismiss: () -> Unit,
    onPinClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    if (!visible || data == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.32f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header
            LibraryItemSheetHeader(data = data, onClose = onDismiss)

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Actions
            if (data.type is LibraryItemMenuType.Folder && !data.isFromTodo) {
                // Special case for folders in the general folder section
                MenuItemWithIcon(
                    text = if (data.isFavorite) "Quitar de favoritos" else "Añadir a fav",
                    icon = if (data.isFavorite) Icons.Rounded.Delete else Icons.Rounded.Folder,
                    onClick = { onDeleteClick(); onDismiss() }
                )
            } else {
                // Normal case for other items or folders in the favorite section
                MenuItemWithIcon(
                    text = if (data.isPinned) "Desfijar" else "Fijar",
                    icon = Icons.Rounded.PushPin,
                    onClick = { onPinClick(); onDismiss() }
                )
            }

            if (data.type is LibraryItemMenuType.UserPlaylist) {
                MenuItemWithIcon(
                    text = "Editar playlist",
                    icon = Icons.Rounded.Edit,
                    onClick = { onEditClick(); onDismiss() }
                )
            }

            // Delete/Unfavorite Action
            val (deleteText, showDelete) = when (data.type) {
                is LibraryItemMenuType.UserPlaylist -> "Eliminar playlist" to true
                is LibraryItemMenuType.Album -> "Eliminar de favoritos" to true
                is LibraryItemMenuType.Artist -> "Eliminar de favoritos" to true
                is LibraryItemMenuType.Folder -> if (data.isFromTodo) "Eliminar de favoritos" to true else "" to false
                else -> "" to false
            }

            if (showDelete) {
                MenuItemWithIcon(
                    text = deleteText,
                    icon = Icons.Rounded.Delete,
                    tintOverride = MaterialTheme.colorScheme.error,
                    onClick = { onDeleteClick(); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun LibraryItemSheetHeader(data: LibraryItemMenuData, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (data.type is LibraryItemMenuType.Folder) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                AsyncImage(
                    model = data.artworkUri ?: R.drawable.playlist_favicon,
                    contentDescription = data.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
        }

        Spacer(modifier = Modifier.size(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                text = data.subtitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onClose,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier
                .clip(CircleShape)
                .padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Cerrar",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MenuItemWithIcon(
    text: String,
    icon: ImageVector,
    tintOverride: Color? = null,
    onClick: () -> Unit
) {
    val contentColor = tintOverride ?: MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            fontWeight = FontWeight.Medium
        )
    }
}
