package com.example.a3310_project

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property for DataStore
val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val USERNAME_KEY = stringPreferencesKey("username")
        val GALLERY_KEY = stringSetPreferencesKey("gallery_uris")
        val LOGGED_IN_USER_ID_KEY = stringPreferencesKey("logged_in_user_id")
        val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
    }

    val usernameFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[USERNAME_KEY] ?: "Jessica Nguyen" }

    val galleryFlow: Flow<Set<String>> = context.dataStore.data
        .map { prefs -> prefs[GALLERY_KEY] ?: emptySet() }

    val loggedInUserIdFlow: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[LOGGED_IN_USER_ID_KEY] }

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[IS_LOGGED_IN_KEY] ?: false }

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

    suspend fun saveLoggedInUser(userId: String) {
        context.dataStore.edit { prefs ->
            prefs[LOGGED_IN_USER_ID_KEY] = userId
            prefs[IS_LOGGED_IN_KEY] = true
        }
    }

    suspend fun clearLoggedInUser() {
        context.dataStore.edit { prefs ->
            prefs.remove(LOGGED_IN_USER_ID_KEY)
            prefs[IS_LOGGED_IN_KEY] = false
        }
    }
}
