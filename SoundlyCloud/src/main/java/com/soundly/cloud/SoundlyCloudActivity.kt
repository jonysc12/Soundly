package com.soundly.cloud

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization
import java.util.logging.Level
import java.util.logging.Logger

class SoundlyCloudActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Desactivar logs ruidosos de jaudiotagger
        Logger.getLogger("org.jaudiotagger").level = Level.OFF
        
        createNotificationChannel()
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        
        val themeModeStr = intent.getStringExtra("theme_mode")
        val dynamicColors = intent.getBooleanExtra("dynamic_colors", false)

        setContent {
            val isDark = when(themeModeStr) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { isDark },
                    navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { isDark }
                )
                window.isNavigationBarContrastEnforced = false
            }

            SoundlyCloudTheme(darkTheme = isDark, dynamicColor = dynamicColors) {
                SoundlyCloudScreen()
            }
        }
    }

    private fun createNotificationChannel() {
        val channelId = "soundly_downloads"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, "Descargas", NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }
}
