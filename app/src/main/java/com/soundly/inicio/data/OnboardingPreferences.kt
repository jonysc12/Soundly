package com.soundly.inicio.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "inicio_prefs")

object OnboardingPreferences {

    private val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")

    suspend fun setSeen(context: Context) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_SEEN] = true
        }
    }

    suspend fun isSeen(context: Context): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[ONBOARDING_SEEN] ?: false
    }
}
