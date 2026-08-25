package com.soundly.player

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.playbackDataStore by preferencesDataStore(name = "playback_state")
