package com.soundly.ui.screens.settings.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundly.data.repository.AnimationSpeed
import com.soundly.data.repository.ArtworkShape
import com.soundly.data.repository.MiniPlayerStyle
import com.soundly.data.repository.MiniProgressBarThickness
import com.soundly.data.repository.MiniProgressBarType
import com.soundly.data.repository.PlayerExpansionMode
import com.soundly.data.repository.PlayerType
import com.soundly.data.repository.ProgressBarType
import com.soundly.data.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimationsViewModel @Inject constructor(
    private val repository: UserSettingsRepository
) : ViewModel() {

    val expansionMode: StateFlow<PlayerExpansionMode> = repository.playerExpansionModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerExpansionMode.EXPANSION
        )

    val expansionSpeed: StateFlow<AnimationSpeed> = repository.expansionSpeedFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnimationSpeed.NORMAL
        )

    val elevationSpeed: StateFlow<AnimationSpeed> = repository.elevationSpeedFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnimationSpeed.NORMAL
        )

    val miniPlayerStyle: StateFlow<MiniPlayerStyle> = repository.miniPlayerStyleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MiniPlayerStyle.SOLID
        )

    val progressBarType: StateFlow<ProgressBarType> = repository.progressBarTypeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProgressBarType.WAVE
        )

    val showThumb: StateFlow<Boolean> = repository.showThumbFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val progressBarThickness: StateFlow<Float> = repository.progressBarThicknessFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 7f
        )

    val artworkShape: StateFlow<ArtworkShape> = repository.artworkShapeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ArtworkShape.DEFAULT
        )

    val playerType: StateFlow<PlayerType> = repository.playerTypeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerType.CLASSIC
        )

    val miniArtworkShape: StateFlow<ArtworkShape> = repository.miniArtworkShapeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ArtworkShape.DEFAULT
        )

    val miniProgressBarType: StateFlow<MiniProgressBarType> = repository.miniProgressBarTypeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MiniProgressBarType.WAVE
        )

    val miniProgressBarThickness: StateFlow<MiniProgressBarThickness> = repository.miniProgressBarThicknessFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MiniProgressBarThickness.NORMAL
        )

    val showMiniPrevious: StateFlow<Boolean> = repository.showMiniPreviousFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val swipeToDismiss: StateFlow<Boolean> = repository.swipeToDismissFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val vividColors: StateFlow<Boolean> = repository.vividColorsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

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

    val showHomePage: StateFlow<Boolean> = repository.showHomePageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val textAlignCentered: StateFlow<Boolean> = repository.textAlignCenteredFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val marqueeTextEnabled: StateFlow<Boolean> = repository.marqueeTextEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val carouselEnabled: StateFlow<Boolean> = repository.carouselEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setExpansionMode(mode: PlayerExpansionMode) {
        viewModelScope.launch {
            repository.updatePlayerExpansionMode(mode)
        }
    }

    fun setExpansionSpeed(speed: AnimationSpeed) {
        viewModelScope.launch {
            repository.updateExpansionSpeed(speed)
        }
    }

    fun setElevationSpeed(speed: AnimationSpeed) {
        viewModelScope.launch {
            repository.updateElevationSpeed(speed)
        }
    }

    fun setMiniPlayerStyle(style: MiniPlayerStyle) {
        viewModelScope.launch {
            repository.updateMiniPlayerStyle(style)
        }
    }

    fun setProgressBarType(type: ProgressBarType) {
        viewModelScope.launch {
            // Prevent changes if Modern player is active
            if (playerType.value == PlayerType.MODERN) return@launch
            repository.updateProgressBarType(type)
        }
    }

    fun setShowThumb(show: Boolean) {
        viewModelScope.launch {
            // Prevent changes if Modern player is active
            if (playerType.value == PlayerType.MODERN) return@launch
            repository.updateShowThumb(show)
        }
    }

    fun setProgressBarThickness(thickness: Float) {
        viewModelScope.launch {
            // Prevent changes if Modern player is active
            if (playerType.value == PlayerType.MODERN) return@launch
            repository.updateProgressBarThickness(thickness)
        }
    }

    fun setArtworkShape(shape: ArtworkShape) {
        viewModelScope.launch {
            repository.updateArtworkShape(shape)
        }
    }

    fun setPlayerType(type: PlayerType) {
        viewModelScope.launch {
            repository.updatePlayerType(type)
            if (type == PlayerType.MODERN) {
                repository.updateProgressBarType(ProgressBarType.PLANE)
                repository.updateShowThumb(false)
                repository.updateProgressBarThickness(11f)
            }
        }
    }

    fun setMiniArtworkShape(shape: ArtworkShape) {
        viewModelScope.launch {
            repository.updateMiniArtworkShape(shape)
        }
    }

    fun setMiniProgressBarType(type: MiniProgressBarType) {
        viewModelScope.launch {
            repository.updateMiniProgressBarType(type)
        }
    }

    fun setMiniProgressBarThickness(thickness: MiniProgressBarThickness) {
        viewModelScope.launch {
            repository.updateMiniProgressBarThickness(thickness)
        }
    }

    fun setShowMiniPrevious(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowMiniPrevious(show)
        }
    }

    fun setSwipeToDismiss(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSwipeToDismiss(enabled)
        }
    }

    fun setVividColors(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateVividColors(enabled)
        }
    }

    fun setTextAlignCentered(centered: Boolean) {
        viewModelScope.launch {
            repository.updateTextAlignCentered(centered)
        }
    }

    fun setMarqueeTextEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateMarqueeTextEnabled(enabled)
        }
    }

    fun setCarouselEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateCarouselEnabled(enabled)
        }
    }
}
