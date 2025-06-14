package com.example.skyposter

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import work.socialhub.kbsky.auth.AuthProvider
import work.socialhub.kbsky.auth.BearerTokenAuthProvider

private val Context.dataStore by preferencesDataStore(name = "session")

object SessionKeys {
    val accessJwt = stringPreferencesKey("access_jwt")
    val refreshJwt = stringPreferencesKey("refresh_jwt")
    val did = stringPreferencesKey("did")
}

class SessionManager(private val context: Context) {

    suspend fun saveSession(accessJwt: String, refreshJwt: String, did: String) {
        context.dataStore.edit { prefs ->
            prefs[SessionKeys.accessJwt] = accessJwt
            prefs[SessionKeys.refreshJwt] = refreshJwt
            prefs[SessionKeys.did] = did
        }
    }

    suspend fun hasSession(): Boolean {
        val prefs = context.dataStore.data.first()
        val access = prefs[SessionKeys.accessJwt]
        val refresh = prefs[SessionKeys.refreshJwt]
        val did = prefs[SessionKeys.did]
        return (access != null && refresh != null && did != null)
    }

    suspend fun getAuth(): AuthProvider? {
        val prefs = context.dataStore.data.first()
        val access = prefs[SessionKeys.accessJwt]
        val refresh = prefs[SessionKeys.refreshJwt]
        val did = prefs[SessionKeys.did]
        return if (access != null && refresh != null && did != null) {
            BearerTokenAuthProvider(access, refresh)
        } else null
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
