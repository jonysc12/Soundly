package com.soundly.ui.screens.settings.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundly.data.repository.AnimationSpeed
import com.soundly.data.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val repository: UserSettingsRepository
) : ViewModel() {

    val lyricsExpansionSpeed: StateFlow<AnimationSpeed> = repository.lyricsExpansionSpeedFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnimationSpeed.NORMAL
        )

    val useLyricsAgslAnimation: StateFlow<Boolean> = repository.useLyricsAgslAnimationFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setLyricsExpansionSpeed(speed: AnimationSpeed) {
        viewModelScope.launch {
            repository.updateLyricsExpansionSpeed(speed)
        }
    }

    fun setUseLyricsAgslAnimation(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateUseLyricsAgslAnimation(enabled)
        }
    }
}
