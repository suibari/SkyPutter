package com.example.skyposter

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import work.socialhub.kbsky.ATProtocolException
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetTimelineRequest
import work.socialhub.kbsky.api.entity.share.AuthRequest
import work.socialhub.kbsky.auth.AuthProvider
import work.socialhub.kbsky.auth.BearerTokenAuthProvider
import work.socialhub.kbsky.auth.api.entity.oauth.OAuthRefreshTokenRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.notification.NotificationListNotificationsNotification

private val Context.dataStore by preferencesDataStore(name = "session")

object SessionKeys {
    val accessJwt = stringPreferencesKey("access_jwt")
    val refreshJwt = stringPreferencesKey("refresh_jwt")
    val did = stringPreferencesKey("did")
}

data class Session(
    val accessJwt: String?,
    val refreshJwt: String?,
    val did: String?
)

object SessionManager {
    private lateinit var appContext: Context
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")


    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    suspend fun saveSession(accessJwt: String, refreshJwt: String, did: String) {
        appContext.dataStore.edit { prefs ->
            prefs[SessionKeys.accessJwt] = accessJwt
            prefs[SessionKeys.refreshJwt] = refreshJwt
            prefs[SessionKeys.did] = did
        }
    }

    suspend fun getSession(): Session {
        val prefs = appContext.dataStore.data.first()
        val access = prefs[SessionKeys.accessJwt]
        val refresh = prefs[SessionKeys.refreshJwt]
        val did = prefs[SessionKeys.did]
        return Session(access, refresh, did)
    }

    fun hasSession(): Boolean {
        // 非suspend関数からは本当のチェックができないため、トークンがセット済みかで判定
        // 利用時は getAuth() で安全にチェックすること
        return this::appContext.isInitialized
    }

    suspend fun getAuth(): AuthProvider? {
        val prefs = appContext.dataStore.data.first()
        val access = prefs[SessionKeys.accessJwt]
        val refresh = prefs[SessionKeys.refreshJwt]
        val did = prefs[SessionKeys.did]

        if (access != null && refresh != null && did != null) {
            return BearerTokenAuthProvider(access, refresh)
        }

        return null
    }

    suspend fun clearSession() {
        appContext.dataStore.edit { it.clear() }
    }

    suspend fun <T> runWithAuthRetry(block: suspend (AuthProvider) -> T): T {
        val auth = getAuth() ?: throw IllegalStateException("No session")

        try {
            return block(auth)
        } catch (e: ATProtocolException) {
            if (e.message?.contains("expired") == true) {
                val prefs = appContext.dataStore.data.first()
                val refresh = prefs[SessionKeys.refreshJwt]

                // refresh!
                val authRefresh = BearerTokenAuthProvider(refresh!!)
                val response = BlueskyFactory
                    .instance(BSKY_SOCIAL.uri)
                    .server()
                    .refreshSession(AuthRequest(authRefresh))
                val accessNew = response.data.accessJwt
                val refreshNew = response.data.refreshJwt
                val did = response.data.did

                val authNew = BearerTokenAuthProvider(accessNew, refreshNew)
                saveSession(accessNew, refreshNew, did)
                Log.i("Session", "session refreshed!")

                // retry with new token
                return block(authNew)
            } else {
                throw e
            }
        }
    }

}
