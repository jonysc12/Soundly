package com.soundly.ui.screens.settings.pages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundly.data.preferences.LanguagePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class GeneralViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    val selectedLanguageCode: StateFlow<String?> = LanguagePreferences.getLanguageCode(context)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )
}
