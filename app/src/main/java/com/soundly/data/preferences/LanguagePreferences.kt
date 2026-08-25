package com.soundly.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "language_prefs")

object LanguagePreferences {
    private val LANGUAGE_CODE = stringPreferencesKey("language_code")

    fun getLanguageCode(context: Context): Flow<String?> {
        return context.dataStore.data.map { prefs ->
            prefs[LANGUAGE_CODE]
        }
    }

    suspend fun setLanguageCode(context: Context, code: String) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE_CODE] = code
        }
    }
}
