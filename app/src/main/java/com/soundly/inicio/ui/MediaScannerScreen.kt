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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import com.soundly.data.model.MusicScanReport
import com.soundly.data.model.Song
import com.soundly.data.repository.MusicRepository
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Escaneo de medios",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Importa la musica guardada en tu dispositivo con una configuracion simple.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                            text = "Fuente local",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "MediaStore de Android",
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Ajustes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ignorar carpetas temporales", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Excluye audios de WhatsApp, Telegram y cache.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = ignoreTempFolders, onCheckedChange = { ignoreTempFolders = it })
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ignorar Audios de menos de 60 segundos", style = MaterialTheme.typography.bodyLarge)
                        }
                        Switch(checked = ignoreShortAudios, onCheckedChange = { ignoreShortAudios = it })
                    }

                    Button(
                        onClick = { if (!isScanning) startScan() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Iniciar escaneo")
                    }
                }
            }
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
            shape = RoundedCornerShape(22.dp),
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
                        text = "scan music",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (section) {
                            DialogSection.Scan -> "Escaneo y resumen"
                            DialogSection.Folders -> "Administrar carpetas"
                            DialogSection.Songs -> "Administrar canciones"
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
                            ) { Text("Confirmar") }

                            FilledTonalButton(
                                onClick = onOpenManage,
                                enabled = !isScanning,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) { Text("Administrar") }
                        }

                        DialogSection.Folders -> {
                            Button(
                                onClick = onApplyManage,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E88E5),
                                    contentColor = Color.White
                                )
                            ) { Text("Aplicar") }

                            FilledTonalButton(
                                onClick = onBackToScan,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) { Text("Volver") }
                        }

                        DialogSection.Songs -> {
                            FilledTonalButton(
                                onClick = onBackToFolders,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) { Text("Volver") }
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
                    isScanning -> "Escaneando archivos..."
                    scanReport != null -> "Escaneo completo"
                    else -> "Listo para escanear"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }

        val entries = buildList {
            add("Filtro carpetas temporales: ${if (ignoreTempFolders) "Activo" else "Inactivo"}")
            add("Filtro audios < 60s: ${if (ignoreShortAudios) "Activo" else "Inactivo"}")
            addAll(discoveredSongs.takeLast(24))
            if (!isScanning && scanReport != null) {
                add("----- Resultado del escaneo -----")
                add("Escaneadas: ${scanReport.scannedSongs}")
                add("Importadas: ${scanReport.importedSongs}")
                add("Bloqueadas por carpetas: ${scanReport.blockedByTempFolders}")
                add("Bloqueadas por audios < 60s: ${scanReport.blockedByShortDuration}")
                add("Total bloqueadas: ${scanReport.blockedSongs}")
            }
        }

        Text("Detalle del escaneo", style = MaterialTheme.typography.titleSmall)
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
            "Selecciona carpetas para excluir y abre su contenido para controlar canciones individuales.",
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
                            "Canciones: ${folder.songs.size} | Bloqueadas manualmente: ${folder.songs.count { blockedSongIds.contains(it.id) }}",
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
                            contentDescription = "Ver contenido"
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
        Text(selectedFolder?.folderPath ?: "No hay carpeta seleccionada", style = MaterialTheme.typography.bodySmall)
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
