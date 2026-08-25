package com.soundly.inicio.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "profile_prefs")

object ProfilePreferences {

    private val PROFILE_CREATED = booleanPreferencesKey("profile_created")
    private val USERNAME = stringPreferencesKey("username")
    private val IMAGE_URI = stringPreferencesKey("image_uri")
    private val LAST_UPDATED = longPreferencesKey("last_updated")

    suspend fun saveProfile(
        context: Context,
        username: String,
        imageUri: Uri?
    ) {
        context.dataStore.edit { prefs ->
            prefs[PROFILE_CREATED] = true
            prefs[USERNAME] = username
            prefs[IMAGE_URI] = imageUri?.toString() ?: ""
            prefs[LAST_UPDATED] = System.currentTimeMillis()
        }
    }

    suspend fun getUsername(context: Context): String {
        val prefs = context.dataStore.data.first()
        return prefs[USERNAME] ?: ""
    }

    suspend fun getImageUri(context: Context): Uri? {
        val prefs = context.dataStore.data.first()
        val uriString = prefs[IMAGE_URI] ?: ""
        return if (uriString.isNotEmpty()) Uri.parse(uriString) else null
    }

    suspend fun isProfileCreated(context: Context): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[PROFILE_CREATED] ?: false
    }

    fun getProfileFlow(context: Context): Flow<Triple<String, Uri?, Long>> {
        return context.dataStore.data.map { prefs ->
            val username = prefs[USERNAME] ?: ""
            val uriString = prefs[IMAGE_URI] ?: ""
            val uri = if (uriString.isNotEmpty()) Uri.parse(uriString) else null
            val lastUpdated = prefs[LAST_UPDATED] ?: 0L
            Triple(username, uri, lastUpdated)
        }
    }
}
