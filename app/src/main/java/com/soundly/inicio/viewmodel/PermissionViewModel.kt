package com.soundly.inicio.viewmodel

import android.Manifest
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NotificationsNone
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
        iconProvider = { Icons.Default.FolderOpen }
    ),
    val notificationPermissionState: PermissionState = PermissionState(
        permission = Manifest.permission.POST_NOTIFICATIONS,
        isAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
        iconProvider = { Icons.Default.NotificationsNone }
    ),
    val bluetoothPermissionState: PermissionState = PermissionState(
        permission = Manifest.permission.BLUETOOTH_CONNECT,
        isAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        iconProvider = { Icons.Default.Bluetooth }
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
        viewModelScope.launch {
            val (newState, tempIcon) = if (isGranted) {
                Pair(
                    when (permission) {
                        _uiState.value.storagePermissionState.permission -> _uiState.value.copy(
                            storagePermissionState = _uiState.value.storagePermissionState.copy(isGranted = true, tempIcon = Icons.Default.Check)
                        )
                        _uiState.value.notificationPermissionState.permission -> _uiState.value.copy(
                            notificationPermissionState = _uiState.value.notificationPermissionState.copy(isGranted = true, tempIcon = Icons.Default.Check)
                        )
                        _uiState.value.bluetoothPermissionState.permission -> _uiState.value.copy(
                            bluetoothPermissionState = _uiState.value.bluetoothPermissionState.copy(isGranted = true, tempIcon = Icons.Default.Check)
                        )
                        else -> _uiState.value
                    },
                    Icons.Default.Check
                )
            } else {
                Pair(
                    when (permission) {
                        _uiState.value.storagePermissionState.permission -> _uiState.value.copy(
                            storagePermissionState = _uiState.value.storagePermissionState.copy(isGranted = false, tempIcon = Icons.Default.Close)
                        )
                        _uiState.value.notificationPermissionState.permission -> _uiState.value.copy(
                            notificationPermissionState = _uiState.value.notificationPermissionState.copy(isGranted = false, tempIcon = Icons.Default.Close)
                        )
                        _uiState.value.bluetoothPermissionState.permission -> _uiState.value.copy(
                            bluetoothPermissionState = _uiState.value.bluetoothPermissionState.copy(isGranted = false, tempIcon = Icons.Default.Close)
                        )
                        else -> _uiState.value
                    },
                    Icons.Default.Close
                )
            }

            _uiState.value = newState

            delay(2000)

            val finalState = when (permission) {
                _uiState.value.storagePermissionState.permission -> _uiState.value.copy(
                    storagePermissionState = _uiState.value.storagePermissionState.copy(tempIcon = null)
                )
                _uiState.value.notificationPermissionState.permission -> _uiState.value.copy(
                    notificationPermissionState = _uiState.value.notificationPermissionState.copy(tempIcon = null)
                )
                _uiState.value.bluetoothPermissionState.permission -> _uiState.value.copy(
                    bluetoothPermissionState = _uiState.value.bluetoothPermissionState.copy(tempIcon = null)
                )
                else -> _uiState.value
            }
            _uiState.value = finalState
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
