package com.soundly.ui.componentes

import android.content.Context
import android.media.AudioManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.util.Log
import com.soundly.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter

// ... (helpers)
private fun routeIcon(route: MediaRouter.RouteInfo): ImageVector {
    val name = route.name.lowercase()
    val deviceType = route.deviceType
    
    return when {
        route.isDefault -> Icons.Rounded.PhoneAndroid
        deviceType == MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH_A2DP ||
        name.contains("bluetooth") || name.contains("bt") -> Icons.Rounded.Bluetooth
        name.contains("headphone") || name.contains("headset") ||
        name.contains("auricular") || name.contains("audifono") -> Icons.Rounded.Headphones
        deviceType == MediaRouter.RouteInfo.DEVICE_TYPE_TV ||
        name.contains("tv") || name.contains("cast") || name.contains("chromecast") || name.contains("google home") -> Icons.Rounded.Cast
        deviceType == MediaRouter.RouteInfo.DEVICE_TYPE_SPEAKER ||
        name.contains("speaker") || name.contains("altavoz") || name.contains("parlante") -> Icons.Rounded.Speaker
        else -> Icons.Rounded.PhoneAndroid
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceControlSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    containerColor: Color,
    onColor: Color,
    isCasting: Boolean = false
) {
    if (LocalInspectionMode.current) {
        // Preview fallback to avoid MediaRouter crash
        Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
            Text("Device Control Preview", color = onColor)
        }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val mediaRouter = remember { MediaRouter.getInstance(context) }

    val selector = remember {
        MediaRouteSelector.Builder()
            .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
            .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
            .build()
    }

    var routes by remember { mutableStateOf<List<MediaRouter.RouteInfo>>(emptyList()) }
    var isSearching by remember { mutableStateOf(true) }
    var selectedRouteId by remember { mutableStateOf(mediaRouter.selectedRoute.id) }
    var currentVolume by remember { mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    val callback = remember {
        object : MediaRouter.Callback() {
            private fun refresh() {
                val foundRoutes = mediaRouter.routes.filter { it.matchesSelector(selector) }
                Log.d("DeviceControlSheet", "Búsqueda de dispositivos: ${foundRoutes.size} encontrados")
                foundRoutes.forEach { Log.d("DeviceControlSheet", " - Dispositivo: ${it.name} (${it.description ?: "Sin descripción"})") }
                routes = foundRoutes
                selectedRouteId = mediaRouter.selectedRoute.id
            }
            override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
                Log.d("DeviceControlSheet", "Nuevo dispositivo detectado: ${route.name}")
                refresh()
            }
            override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
                Log.d("DeviceControlSheet", "Dispositivo desconectado: ${route.name}")
                refresh()
            }
            override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
            override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) {
                Log.d("DeviceControlSheet", "Dispositivo seleccionado: ${route.name}")
                selectedRouteId = route.id
                refresh()
            }
        }
    }

    fun startScan() {
        isSearching = true
        mediaRouter.removeCallback(callback)
        mediaRouter.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)
        // Simulamos un periodo de búsqueda visual para feedback del usuario
        scope.launch {
            delay(3000)
            isSearching = false
        }
    }

    DisposableEffect(isOpen) {
        if (isOpen) {
            startScan()
            routes = mediaRouter.routes.filter { it.matchesSelector(selector) }
            selectedRouteId = mediaRouter.selectedRoute.id
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                }
            }
            context.contentResolver.registerContentObserver(
                android.provider.Settings.System.CONTENT_URI,
                true,
                observer
            )

            onDispose {
                mediaRouter.removeCallback(callback)
                context.contentResolver.unregisterContentObserver(observer)
            }
        } else {
            onDispose {}
        }
    }

    val displayRoutes = remember(routes, selectedRouteId) {
        routes.distinctBy { it.id }
            .sortedWith(compareBy({ it.id != selectedRouteId }, { !it.isDefault }, { it.name }))
    }

    if (isOpen) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = containerColor,
            dragHandle = { BottomSheetDefaults.DragHandle(color = onColor.copy(alpha = 0.4f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.device_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = onColor
                    )
                    
                    if (!isSearching) {
                        IconButton(onClick = { startScan() }) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.cd_refresh),
                                tint = onColor
                            )
                        }
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = onColor,
                            strokeWidth = 2.dp
                        )
                    }
                }
                
                Spacer(Modifier.height(20.dp))

                if (isSearching && displayRoutes.size <= 1) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = onColor.copy(alpha = 0.5f))
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.device_sheet_searching), color = onColor.copy(alpha = 0.6f))
                        }
                    }
                } else if (displayRoutes.size <= 1 && !isSearching) {
                    // Solo está el dispositivo actual (teléfono)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CastConnected,
                            contentDescription = null,
                            tint = onColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.device_sheet_no_devices_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = onColor
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.device_sheet_no_devices_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = onColor.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { startScan() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = onColor,
                                contentColor = containerColor
                            )
                        ) {
                            Icon(Icons.Rounded.Search, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.button_search_again))
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        displayRoutes.forEach { route ->
                            val isSelected = route.id == selectedRouteId
                            RouteItem(
                                name = if (route.isDefault || route.name.lowercase().contains("dispositivo")) stringResource(R.string.device_name_default) else route.name,
                                icon = routeIcon(route),
                                isSelected = isSelected,
                                isCasting = isCasting && isSelected && (route.name.lowercase().contains("cast") || route.deviceType == MediaRouter.RouteInfo.DEVICE_TYPE_TV),
                                containerColor = containerColor,
                                onColor = onColor,
                                onClick = {
                                    if (!isSelected) {
                                        mediaRouter.selectRoute(route)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
                Text(
                    text = stringResource(R.string.device_sheet_volume),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = onColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (currentVolume == 0) Icons.AutoMirrored.Rounded.VolumeOff 
                        else Icons.AutoMirrored.Rounded.VolumeDown,
                        contentDescription = null,
                        tint = onColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                    Slider(
                        value = currentVolume.toFloat(),
                        onValueChange = { v ->
                            currentVolume = v.toInt()
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, v.toInt(), 0)
                        },
                        valueRange = 0f..maxVolume.toFloat(),
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = onColor,
                            activeTrackColor = onColor,
                            inactiveTrackColor = onColor.copy(alpha = 0.2f)
                        )
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = onColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteItem(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    isCasting: Boolean,
    containerColor: Color,
    onColor: Color,
    onClick: () -> Unit
) {
    Surface(
        color = onColor.copy(alpha = if (isSelected) 0.15f else 0.07f),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(1.5.dp, onColor.copy(alpha = 0.45f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) onColor else onColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) containerColor else onColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = onColor
                )
                if (isSelected) {
                    Text(
                        text = if (isCasting) stringResource(R.string.device_status_casting) else stringResource(R.string.device_status_connected),
                        style = MaterialTheme.typography.labelSmall,
                        color = onColor.copy(alpha = 0.6f)
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = onColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
