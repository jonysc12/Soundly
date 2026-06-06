package com.soundly.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.soundly.ui.theme.SoundlyTheme

class Settings : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoundlyTheme {
                SettingsScreen()
            }
        }
    }
}

@Composable
private fun SettingsScreen() {
    Box(modifier = Modifier.fillMaxSize())
}
