package com.soundly

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.soundly.data.repository.MusicRepository
import com.soundly.inicio.ui.AppNavHost

@Composable
fun App(repository: MusicRepository) {
    val navController = rememberNavController()
    AppNavHost(navController, repository)
}
