package com.soundly.ui.screens.settings.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundly.data.repository.HomeSectionType
import com.soundly.data.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeSettingsViewModel @Inject constructor(
    private val repository: UserSettingsRepository
) : ViewModel() {

    val homeSectionsOrder: StateFlow<List<HomeSectionType>> = repository.homeSectionsOrderFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeSectionType.values().toList()
        )

    val showSubtitles: StateFlow<Boolean> = repository.showHomeSectionSubtitlesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun updateOrder(newOrder: List<HomeSectionType>) {
        viewModelScope.launch {
            repository.updateHomeSectionsOrder(newOrder)
        }
    }

    fun updateShowSubtitles(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowHomeSectionSubtitles(show)
        }
    }
}
