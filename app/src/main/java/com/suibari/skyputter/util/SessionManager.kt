package com.suibari.skyputter.util

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
import work.socialhub.kbsky.api.entity.share.AuthRequest
import work.socialhub.kbsky.auth.AuthProvider
import work.socialhub.kbsky.auth.BearerTokenAuthProvider
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL

// セッション関連の例外クラス
sealed class SessionException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotInitialized : SessionException("SessionManager is not initialized. Call initialize(context) first.")
    class NoSession : SessionException("No valid session found")
    class RefreshTokenMissing : SessionException("Refresh token is missing")
    class RefreshFailed(message: String, cause: Throwable? = null) : SessionException("Failed to refresh session: $message", cause)
    class InvalidSession(message: String) : SessionException("Invalid session data: $message")
}

object SessionKeys {
    val host = stringPreferencesKey("host")
    val accessJwt = stringPreferencesKey("access_jwt")
    val refreshJwt = stringPreferencesKey("refresh_jwt")
    val did = stringPreferencesKey("did")
}

data class Session(
    val accessJwt: String?,
    val refreshJwt: String?,
    val did: String?,
    val host: String?,
) {
    fun isValid(): Boolean {
        return !accessJwt.isNullOrBlank() &&
                !refreshJwt.isNullOrBlank() &&
                !did.isNullOrBlank() &&
                !host.isNullOrBlank()
    }
}

object SessionManager {
    private lateinit var appContext: Context
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun ensureInitialized() {
        if (!::appContext.isInitialized) {
            throw SessionException.NotInitialized()
        }
    }

    suspend fun saveSession(accessJwt: String, refreshJwt: String, did: String, host: String) {
        ensureInitialized()

        // 入力値の検証
        if (accessJwt.isBlank()) throw SessionException.InvalidSession("Access JWT is blank")
        if (refreshJwt.isBlank()) throw SessionException.InvalidSession("Refresh JWT is blank")
        if (did.isBlank()) throw SessionException.InvalidSession("DID is blank")
        if (host.isBlank()) throw SessionException.InvalidSession("Host is blank")

        try {
            appContext.dataStore.edit { prefs ->
                prefs[SessionKeys.accessJwt] = accessJwt
                prefs[SessionKeys.refreshJwt] = refreshJwt
                prefs[SessionKeys.did] = did
                prefs[SessionKeys.host] = host
            }
        } catch (e: Exception) {
            throw SessionException.InvalidSession("Failed to save session: ${e.message}")
        }
    }

    suspend fun getSession(): Session {
        ensureInitialized()

        return try {
            val prefs = appContext.dataStore.data.first()
            Session(
                accessJwt = prefs[SessionKeys.accessJwt],
                refreshJwt = prefs[SessionKeys.refreshJwt],
                did = prefs[SessionKeys.did],
                host = prefs[SessionKeys.host]
            )
        } catch (e: Exception) {
            throw SessionException.InvalidSession("Failed to retrieve session: ${e.message}")
        }
    }

    suspend fun hasValidSession(): Boolean {
        return try {
            ensureInitialized()
            val session = getSession()
            session.isValid()
        } catch (e: SessionException) {
            false
        }
    }

    suspend fun getAuth(): AuthProvider {
        val session = getSession()

        val accessJwt = session.accessJwt
        val refreshJwt = session.refreshJwt
        val did = session.did

        if (accessJwt.isNullOrBlank() || refreshJwt.isNullOrBlank() || did.isNullOrBlank()) {
            throw SessionException.NoSession()
        }

        return BearerTokenAuthProvider(accessJwt, refreshJwt)
    }

    suspend fun clearSession() {
        ensureInitialized()

        try {
            appContext.dataStore.edit { it.clear() }
        } catch (e: Exception) {
            Log.w("SessionManager", "Failed to clear session", e)
            // セッションクリアの失敗は致命的でないため、警告ログのみ
        }
    }

    private suspend fun refreshSession(): AuthProvider {
        val session = getSession()
        val refreshJwt = session.refreshJwt
        val host = session.host ?: BSKY_SOCIAL.uri

        if (refreshJwt.isNullOrBlank()) {
            throw SessionException.RefreshTokenMissing()
        }

        try {
            // refreshトークンを使ってセッションをリフレッシュ
            val authRefresh = BearerTokenAuthProvider(refreshJwt)
            val response = BlueskyFactory
                .instance(host)
                .server()
                .refreshSession(AuthRequest(authRefresh))

            val responseData = response.data
            val accessNew = responseData.accessJwt
            val refreshNew = responseData.refreshJwt
            val did = responseData.did

            // レスポンスデータの検証
            if (accessNew.isNullOrBlank() || refreshNew.isNullOrBlank() || did.isNullOrBlank()) {
                throw SessionException.RefreshFailed("Invalid response data from refresh endpoint")
            }

            // 新しいセッションを保存
            saveSession(accessNew, refreshNew, did, host)
            Log.i("SessionManager", "Session refreshed successfully")

            return BearerTokenAuthProvider(accessNew, refreshNew)

        } catch (e: ATProtocolException) {
            throw SessionException.RefreshFailed("AT Protocol error: ${e.message}", e)
        } catch (e: SessionException) {
            throw e // SessionExceptionはそのまま再投げ
        } catch (e: Exception) {
            throw SessionException.RefreshFailed("Unexpected error: ${e.message}", e)
        }
    }

    suspend fun <T> runWithAuthRetry(block: suspend (AuthProvider) -> T): T {
        val auth = getAuth()

        try {
            return block(auth)
        } catch (e: ATProtocolException) {
            // トークンの有効期限切れの場合のみリフレッシュを試行
            if (isTokenExpiredError(e)) {
                Log.i("SessionManager", "Access token expired, attempting refresh...")

                try {
                    val newAuth = refreshSession()
                    return block(newAuth)
                } catch (refreshError: SessionException) {
                    Log.e("SessionManager", "Session refresh failed", refreshError)
                    throw refreshError
                }
            } else {
                // その他のATProtocolExceptionはそのまま投げる
                throw e
            }
        }
    }

    private fun isTokenExpiredError(e: ATProtocolException): Boolean {
        val message = e.message?.lowercase()
        return message?.contains("expired") == true ||
                message?.contains("invalid") == true ||
                message?.contains("unauthorized") == true
    }
}