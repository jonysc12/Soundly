package com.soundly.inicio.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.soundly.inicio.data.MediaScannerPreferences
import com.soundly.inicio.data.OnboardingPreferences
import com.soundly.inicio.data.ProfilePreferences

class LauncherViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context = getApplication<Application>()

    // 1️⃣ Onboarding
    suspend fun isOnboardingSeen(): Boolean {
        return OnboardingPreferences.isSeen(context)
    }

    // 2️⃣ Permisos
    fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    // 3️⃣ Perfil
    suspend fun isProfileCreated(): Boolean {
        return ProfilePreferences.isProfileCreated(context)
    }

    // 4️⃣ Escaneo confirmado
    suspend fun isMediaScanConfirmed(): Boolean {
        return MediaScannerPreferences.isScanConfirmed(context)
    }
}
