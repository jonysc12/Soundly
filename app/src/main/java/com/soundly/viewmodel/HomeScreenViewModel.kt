package com.soundly.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundly.inicio.data.ProfilePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val username: String = "",
    val imageUri: Uri? = null
)

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(
                username = ProfilePreferences.getUsername(application),
                imageUri = ProfilePreferences.getImageUri(application)
            )
        }
    }
}