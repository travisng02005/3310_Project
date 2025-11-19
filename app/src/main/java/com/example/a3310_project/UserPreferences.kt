package com.example.a3310_project

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property for DataStore
val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val USERNAME_KEY = stringPreferencesKey("username")
        val GALLERY_KEY = stringSetPreferencesKey("gallery_uris")
    }

    val usernameFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[USERNAME_KEY] ?: "Jessica Nguyen" }

    val galleryFlow: Flow<Set<String>> = context.dataStore.data
        .map { prefs -> prefs[GALLERY_KEY] ?: emptySet() }

    suspend fun saveUsername(name: String) {
        context.dataStore.edit { prefs ->
            prefs[USERNAME_KEY] = name
        }
    }

    suspend fun saveGallery(uris: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[GALLERY_KEY] = uris.toSet()
        }
    }
}
