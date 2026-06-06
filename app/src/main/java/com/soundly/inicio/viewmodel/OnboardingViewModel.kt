package com.soundly.inicio.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soundly.inicio.data.OnboardingPreferences
import kotlinx.coroutines.launch

class OnboardingViewModel(
    application: Application
) : AndroidViewModel(application) {

    fun completeOnboarding(onCompleted: () -> Unit) {
        viewModelScope.launch {
            OnboardingPreferences.setSeen(getApplication())
            onCompleted()
        }
    }
}
