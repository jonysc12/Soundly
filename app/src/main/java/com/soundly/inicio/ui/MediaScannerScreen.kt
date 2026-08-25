package com.soundly.inicio.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import com.soundly.R
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
import com.soundly.ui.componentes.agslFrostedGlass
import com.soundly.data.model.MusicScanReport
import com.soundly.data.model.Song
import com.soundly.data.repository.MusicRepository
import com.soundly.ui.screens.settings.SettingsLayout
import com.soundly.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class FolderGroup(
    val folderPath: String,
    val songs: List<Song>
)

private enum class DialogSection {
    Scan, Folders, Songs
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScannerScreen(
    onBack: () -> Unit,
    onScanConfirmed: () -> Unit,
    repository: MusicRepository
) {
    var ignoreTempFolders by remember { mutableStateOf(true) }
    var ignoreShortAudios by remember { mutableStateOf(false) }
    var showControlDialog by remember { mutableStateOf(false) }
    var dialogSection by remember { mutableStateOf(DialogSection.Scan) }
    var isScanning by remember { mutableStateOf(false) }
    var scanReport by remember { mutableStateOf<MusicScanReport?>(null) }
    var folderGroups by remember { mutableStateOf<List<FolderGroup>>(emptyList()) }
    var selectedFolder by remember { mutableStateOf<FolderGroup?>(null) }
    val discoveredSongs = remember { mutableStateListOf<String>() }
    val blockedFolders = remember { mutableStateListOf<String>() }
    val blockedSongIds = remember { mutableStateListOf<Long>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val settings = repository.getCurrentScannerSettings()
        ignoreTempFolders = settings.ignoreTempFolders
        ignoreShortAudios = settings.ignoreShortAudios
        blockedFolders.clear()
        blockedFolders.addAll(settings.blockedFolders)
        blockedSongIds.clear()
        blockedSongIds.addAll(settings.blockedSongIds)
    }

    fun loadFoldersForManage() {
        scope.launch {
            val songs = repository.getLastScannedSourceSongs()
            val grouped = songs
                .groupBy { song ->
                    val normalized = song.path.replace('\\', '/')
                    normalized.substringBeforeLast('/', missingDelimiterValue = normalized)
                }
                .map { (folder, folderSongs) ->
                    FolderGroup(folderPath = folder, songs = folderSongs.sortedBy { it.title.lowercase() })
                }
                .sortedBy { it.folderPath.lowercase() }
            withContext(Dispatchers.Main) {
                folderGroups = grouped
                dialogSection = DialogSection.Folders
            }
        }
    }

    fun startScan() {
        showControlDialog = true
        dialogSection = DialogSection.Scan
        selectedFolder = null
        isScanning = true
        scanReport = null
        discoveredSongs.clear()

        scope.launch {
            val report = repository.scanSongs(
                ignoreTempFolders = ignoreTempFolders,
                ignoreShortAudios = ignoreShortAudios,
                manuallyBlockedFolders = blockedFolders.toSet(),
                manuallyBlockedSongIds = blockedSongIds.toSet()
            ) { batch ->
                withContext(Dispatchers.Main) {
                    discoveredSongs.addAll(batch)
                    if (discoveredSongs.size > 140) {
                        val removeCount = discoveredSongs.size - 140
                        repeat(removeCount) { discoveredSongs.removeAt(0) }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                scanReport = report
                isScanning = false
            }
        }
    }

    val isDark = LocalIsDarkTheme.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        SettingsLayout(
            title = "",
            onBack = onBack
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.media_scanner_title),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.media_scanner_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.media_scanner_local_source),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.media_scanner_android_mediastore),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.media_scanner_settings),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.media_scanner_ignore_temp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    stringResource(R.string.media_scanner_ignore_temp_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = ignoreTempFolders,
                                onCheckedChange = { ignoreTempFolders = it })
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.media_scanner_ignore_short),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Switch(
                                checked = ignoreShortAudios,
                                onCheckedChange = { ignoreShortAudios = it })
                        }
                    }
                }
            }
        }

        // Botón flotante al estilo Onboarding
        val borderAlpha = if (isDark) 0.3f else 0.05f
        val containerColor =
            if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.5f)
        val contentColor = MaterialTheme.colorScheme.onSurface

        Box(
            modifier = Modifier
                .size(90.dp)
                .offset(
                    x = screenWidth * 0.82f - 45.dp,
                    y = screenHeight * 0.85f - 45.dp
                )
                .clip(CircleShape)
                .agslFrostedGlass(radius = 20f, tint = Color.Transparent)
                .border(
                    1.dp,
                    contentColor.copy(alpha = borderAlpha),
                    CircleShape
                )
                .background(containerColor, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (!isScanning) startScan() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.media_scanner_start_scan_cd),
                modifier = Modifier.size(36.dp),
                tint = contentColor
            )
        }
    }

    if (showControlDialog) {
        MediaScanControlDialog(
            section = dialogSection,
            isScanning = isScanning,
            scanReport = scanReport,
            discoveredSongs = discoveredSongs.toList(),
            ignoreTempFolders = ignoreTempFolders,
            ignoreShortAudios = ignoreShortAudios,
            folderGroups = folderGroups,
            selectedFolder = selectedFolder,
            blockedFolders = blockedFolders.toSet(),
            blockedSongIds = blockedSongIds.toSet(),
            onDismiss = {
                if (!isScanning) showControlDialog = false
            },
            onConfirm = {
                scope.launch {
                    repository.setScanConfirmed(true)
                    showControlDialog = false
                    // Solo confirmar puede avanzar a la siguiente pantalla.
                    onScanConfirmed()
                }
            },
            onOpenManage = { loadFoldersForManage() },
            onBackToScan = { dialogSection = DialogSection.Scan },
            onBackToFolders = { dialogSection = DialogSection.Folders },
            onOpenFolderSongs = { folder ->
                selectedFolder = folder
                dialogSection = DialogSection.Songs
            },
            onToggleFolder = { folder ->
                if (blockedFolders.contains(folder)) blockedFolders.remove(folder) else blockedFolders.add(folder)
            },
            onToggleSong = { songId ->
                if (blockedSongIds.contains(songId)) blockedSongIds.remove(songId) else blockedSongIds.add(songId)
            },
            onApplyManage = { startScan() }
        )
    }
}

@Composable
private fun MediaScanControlDialog(
    section: DialogSection,
    isScanning: Boolean,
    scanReport: MusicScanReport?,
    discoveredSongs: List<String>,
    ignoreTempFolders: Boolean,
    ignoreShortAudios: Boolean,
    folderGroups: List<FolderGroup>,
    selectedFolder: FolderGroup?,
    blockedFolders: Set<String>,
    blockedSongIds: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onOpenManage: () -> Unit,
    onBackToScan: () -> Unit,
    onBackToFolders: () -> Unit,
    onOpenFolderSongs: (FolderGroup) -> Unit,
    onToggleFolder: (String) -> Unit,
    onToggleSong: (Long) -> Unit,
    onApplyManage: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.media_scanner_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (section) {
                            DialogSection.Scan -> stringResource(R.string.media_scanner_dialog_scan_summary)
                            DialogSection.Folders -> stringResource(R.string.media_scanner_dialog_manage_folders)
                            DialogSection.Songs -> stringResource(R.string.media_scanner_dialog_manage_songs)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))

                AnimatedContent(
                    targetState = section,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "MediaScanDialogSection"
                ) { currentSection ->
                    when (currentSection) {
                        DialogSection.Scan -> ScanSection(
                            isScanning = isScanning,
                            scanReport = scanReport,
                            discoveredSongs = discoveredSongs,
                            ignoreTempFolders = ignoreTempFolders,
                            ignoreShortAudios = ignoreShortAudios
                        )

                        DialogSection.Folders -> FolderSection(
                            folders = folderGroups,
                            blockedFolders = blockedFolders,
                            blockedSongIds = blockedSongIds,
                            onToggleFolder = onToggleFolder,
                            onOpenFolderSongs = onOpenFolderSongs
                        )

                        DialogSection.Songs -> SongsSection(
                            selectedFolder = selectedFolder,
                            blockedSongIds = blockedSongIds,
                            onToggleSong = onToggleSong
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (section) {
                        DialogSection.Scan -> {
                            Button(
                                onClick = onConfirm,
                                enabled = !isScanning && scanReport != null,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E88E5),
                                    contentColor = Color.White
                                )
                            ) { Text(stringResource(R.string.media_scanner_dialog_confirm)) }

                            FilledTonalButton(
                                onClick = onOpenManage,
                                enabled = !isScanning,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) { Text(stringResource(R.string.media_scanner_dialog_manage)) }
                        }

                        DialogSection.Folders -> {
                            Button(
                                onClick = onApplyManage,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E88E5),
                                    contentColor = Color.White
                                )
                            ) { Text(stringResource(R.string.media_scanner_dialog_apply)) }

                            FilledTonalButton(
                                onClick = onBackToScan,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) { Text(stringResource(R.string.media_scanner_dialog_back)) }
                        }

                        DialogSection.Songs -> {
                            FilledTonalButton(
                                onClick = onBackToFolders,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) { Text(stringResource(R.string.media_scanner_dialog_back)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ScanSection(
    isScanning: Boolean,
    scanReport: MusicScanReport?,
    discoveredSongs: List<String>,
    ignoreTempFolders: Boolean,
    ignoreShortAudios: Boolean
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                isScanning -> CircularWavyProgressIndicator(modifier = Modifier.size(34.dp))
                scanReport != null -> CircularWavyProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(34.dp)
                )
                else -> CircularWavyProgressIndicator(
                    progress = { 0f },
                    modifier = Modifier.size(34.dp)
                )
            }
            Text(
                text = when {
                    isScanning -> stringResource(R.string.media_scanner_dialog_scanning)
                    scanReport != null -> stringResource(R.string.media_scanner_dialog_complete)
                    else -> stringResource(R.string.media_scanner_dialog_ready)
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }

        val entries = buildList {
            add(context.getString(R.string.media_scanner_report_filter_temp, if (ignoreTempFolders) context.getString(R.string.active) else context.getString(R.string.inactive)))
            add(context.getString(R.string.media_scanner_report_filter_short, if (ignoreShortAudios) context.getString(R.string.active) else context.getString(R.string.inactive)))
            addAll(discoveredSongs.takeLast(24))
            if (!isScanning && scanReport != null) {
                add(context.getString(R.string.media_scanner_report_result_header))
                add(context.getString(R.string.media_scanner_report_scanned, scanReport.scannedSongs))
                add(context.getString(R.string.media_scanner_report_imported, scanReport.importedSongs))
                add(context.getString(R.string.media_scanner_report_blocked_temp, scanReport.blockedByTempFolders))
                add(context.getString(R.string.media_scanner_report_blocked_short, scanReport.blockedByShortDuration))
                add(context.getString(R.string.media_scanner_report_total_blocked, scanReport.blockedSongs))
            }
        }

        Text(stringResource(R.string.media_scanner_dialog_detail), style = MaterialTheme.typography.titleSmall)
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
            items(entries) { item ->
                Text("- $item", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
@Composable
private fun FolderSection(
    folders: List<FolderGroup>,
    blockedFolders: Set<String>,
    blockedSongIds: Set<Long>,
    onToggleFolder: (String) -> Unit,
    onOpenFolderSongs: (FolderGroup) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(R.string.media_scanner_dialog_folders_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
            items(folders) { folder ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = blockedFolders.contains(folder.folderPath),
                        onCheckedChange = { onToggleFolder(folder.folderPath) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(folder.folderPath, style = MaterialTheme.typography.bodySmall)
                        Text(
                            stringResource(R.string.media_scanner_dialog_songs_count, folder.songs.size, folder.songs.count { blockedSongIds.contains(it.id) }),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { onOpenFolderSongs(folder) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = stringResource(R.string.media_scanner_dialog_see_content_cd)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
            }
        }
    }
}

@Composable
private fun SongsSection(
    selectedFolder: FolderGroup?,
    blockedSongIds: Set<Long>,
    onToggleSong: (Long) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(selectedFolder?.folderPath ?: stringResource(R.string.media_scanner_dialog_no_folder_selected), style = MaterialTheme.typography.bodySmall)
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
            items(selectedFolder?.songs ?: emptyList()) { song ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = blockedSongIds.contains(song.id),
                        onCheckedChange = { onToggleSong(song.id) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, style = MaterialTheme.typography.bodyMedium)
                        Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
            }
        }
    }
}
