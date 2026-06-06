package com.soundly.feature.biblioteca.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soundly.data.model.FolderSummary
import com.soundly.ui.componentes.listas.ItemFolderList

@Composable
fun FoldersListPage(
    folders: List<FolderSummary>,
    onFolderClick: (String) -> Unit,
    onFolderLongClick: (FolderSummary) -> Unit = {},
    pinnedFolders: Set<String> = emptySet()
) {
    if (folders.isEmpty()) {
        BibliotecaEmptyState(
            title = "Sin carpetas con música",
            message = "Cuando Soundly detecte archivos locales, verás aquí su agrupación por carpetas."
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(folders, key = { it.path }) { folder ->
            ItemFolderList(
                folderName = folder.name,
                songCount = folder.songCount,
                onClick = { onFolderClick(folder.path) },
                onLongClick = { onFolderLongClick(folder) },
                isPinned = folder.path in pinnedFolders
            )
        }
    }
}
