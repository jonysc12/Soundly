package com.soundly

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import com.soundly.data.repository.MusicRepository
import com.soundly.ui.theme.SoundlyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: MusicRepository

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Evita el flash negro durante animación del teclado
        val isNight = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        window.decorView.setBackgroundColor(if (isNight) 0xFF0A0A0A.toInt() else 0xFFFFFFFF.toInt())

        setContent {
            SoundlyTheme {

                // 🔥 Quita el glow clásico y permite stretch moderno
                CompositionLocalProvider(
                    LocalOverscrollConfiguration provides null
                ) {
                    App(repository)
                }
            }
        }
    }
}