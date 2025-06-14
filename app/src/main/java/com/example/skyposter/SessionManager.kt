package com.example.skyposter

import android.content.Context
import android.util.Log
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
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL

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

        println("access: $access, refresh: $refresh")
        if (access != null && refresh != null && did != null) {
            val auth: AuthProvider = BearerTokenAuthProvider(access, refresh)

            try {
                // getTimelineにtryし、expiredならrefreshする
                BlueskyFactory
                    .instance(BSKY_SOCIAL.uri)
                    .feed()
                    .getTimeline(FeedGetTimelineRequest(auth))
                return auth
            } catch (e: ATProtocolException) {
                println(e)
                if (e.message?.contains("ExpiredToken") == true) {
                    val response = BlueskyFactory
                        .instance(BSKY_SOCIAL.uri)
                        .server()
                        .refreshSession(AuthRequest(auth))
                    saveSession(
                        response.data.accessJwt,
                        response.data.refreshJwt,
                        did
                    )
                    return auth
                } else {
                    Log.e("Session", "セッション再取得失敗: ${e.message}")
                    throw e
                }
            }
        }

        return null
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
