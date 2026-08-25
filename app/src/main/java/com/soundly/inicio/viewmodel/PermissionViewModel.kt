package com.soundly.inicio.viewmodel

import android.Manifest
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PermissionUiState(
    val storagePermissionState: PermissionState = PermissionState(
        permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE,
        isAvailable = true, // Always available
        iconProvider = { Icons.Rounded.FolderOpen }
    ),
    val notificationPermissionState: PermissionState = PermissionState(
        permission = Manifest.permission.POST_NOTIFICATIONS,
        isAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
        iconProvider = { Icons.Rounded.NotificationsNone }
    ),
    val bluetoothPermissionState: PermissionState = PermissionState(
        permission = Manifest.permission.BLUETOOTH_CONNECT,
        isAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        iconProvider = { Icons.Rounded.Sensors }
    )
)

data class PermissionState(
    val permission: String,
    val isGranted: Boolean = false,
    val isAvailable: Boolean,
    val iconProvider: () -> ImageVector,
    val tempIcon: ImageVector? = null
)

class PermissionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState = _uiState.asStateFlow()

    fun onPermissionResult(permission: String, isGranted: Boolean) {
        _uiState.update { currentState ->
            when (permission) {
                currentState.storagePermissionState.permission -> currentState.copy(
                    storagePermissionState = currentState.storagePermissionState.copy(
                        isGranted = isGranted,
                        tempIcon = if (isGranted) Icons.Rounded.Check else Icons.Rounded.Close
                    )
                )
                currentState.notificationPermissionState.permission -> currentState.copy(
                    notificationPermissionState = currentState.notificationPermissionState.copy(
                        isGranted = isGranted,
                        tempIcon = if (isGranted) Icons.Rounded.Check else Icons.Rounded.Close
                    )
                )
                currentState.bluetoothPermissionState.permission -> currentState.copy(
                    bluetoothPermissionState = currentState.bluetoothPermissionState.copy(
                        isGranted = isGranted,
                        tempIcon = if (isGranted) Icons.Rounded.Check else Icons.Rounded.Close
                    )
                )
                else -> currentState
            }
        }

        viewModelScope.launch {
            delay(2000)
            _uiState.update { currentState ->
                when (permission) {
                    currentState.storagePermissionState.permission -> currentState.copy(
                        storagePermissionState = currentState.storagePermissionState.copy(tempIcon = null)
                    )
                    currentState.notificationPermissionState.permission -> currentState.copy(
                        notificationPermissionState = currentState.notificationPermissionState.copy(tempIcon = null)
                    )
                    currentState.bluetoothPermissionState.permission -> currentState.copy(
                        bluetoothPermissionState = currentState.bluetoothPermissionState.copy(tempIcon = null)
                    )
                    else -> currentState
                }
            }
        }
    }

    fun updateGrantedStatus(
        storageGranted: Boolean,
        notificationGranted: Boolean,
        bluetoothGranted: Boolean
    ) {
        _uiState.update {
            it.copy(
                storagePermissionState = it.storagePermissionState.copy(isGranted = storageGranted),
                notificationPermissionState = it.notificationPermissionState.copy(
                    isGranted = notificationGranted && it.notificationPermissionState.isAvailable
                ),
                bluetoothPermissionState = it.bluetoothPermissionState.copy(
                    isGranted = bluetoothGranted && it.bluetoothPermissionState.isAvailable
                )
            )
        }
    }
}
