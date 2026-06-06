package com.soundly.inicio.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundly.inicio.data.ProfilePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.system.exitProcess

data class ProfileUiState(
    val imageUri: Uri? = null,
    val username: String = "",
    val step: Int = 1,
    val isUsernameConfirmed: Boolean = false,
    val isEditingImage: Boolean = false,
    val showBottomSheet: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun onImageSelected(uri: Uri?) {
        uri ?: return

        viewModelScope.launch {
            val inputStream = application.contentResolver.openInputStream(uri)
            val file = File(application.filesDir, "profile_image.jpg")

            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val savedUri = Uri.fromFile(file)

            _uiState.update {
                it.copy(imageUri = savedUri, isEditingImage = false)
            }
        }
    }

    fun onUsernameChanged(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    fun onContinueClick() {
        when (_uiState.value.step) {
            1 -> _uiState.update { it.copy(step = 2) }
            2 -> {
                _uiState.update { it.copy(isUsernameConfirmed = true, step = 3) }
            }
        }
    }
    
    fun onBackClick() {
        when (_uiState.value.step) {
            1 -> _uiState.update { it.copy(showBottomSheet = true) }
            2 -> _uiState.update { it.copy(step = 1) }
            3 -> _uiState.update { it.copy(step = 2, isUsernameConfirmed = false) }
        }
    }

    fun onEditClick() {
        _uiState.update {
            it.copy(
                step = 1,
                isUsernameConfirmed = false,
                isEditingImage = true
            )
        }
    }

    fun onProfileCreationConfirmed(onProfileCreated: () -> Unit) {
        viewModelScope.launch {
            ProfilePreferences.saveProfile(
                application,
                _uiState.value.username,
                _uiState.value.imageUri
            )
            onProfileCreated()
        }
    }

    fun onBottomSheetDismiss() {
        _uiState.update { it.copy(showBottomSheet = false) }
    }

    fun onExitConfirm() {
        exitProcess(0)
    }

    fun onEditImage() {
        _uiState.update { it.copy(isEditingImage = true) }
    }
}
