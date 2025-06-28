package com.suibari.skyputter.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore by preferencesDataStore(name = "settings")

object NotificationSettings {
    // OS側通知許可ダイアログを表示したか
    private val NOTIFICATION_PERMISSION_DIALOG_SHOWN_KEY = booleanPreferencesKey("notification_permission_dialog_shown")
    // 通知受信のポーリングを有効にするか
    private val NOTIFICATION_POLLING_ENABLED_KEY = booleanPreferencesKey("notification_enabled")

    fun getNotificationDialogShown(context: Context): Flow<Boolean> {
        return context.settingsDataStore.data.map { prefs ->
            prefs[NOTIFICATION_PERMISSION_DIALOG_SHOWN_KEY] ?: false // 初期状態は「未表示」
        }
    }

    suspend fun setNotificationDialogShown(context: Context, shown: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[NOTIFICATION_PERMISSION_DIALOG_SHOWN_KEY] = shown
        }
    }

    fun getNotificationPollingEnabled(context: Context): Flow<Boolean> {
        return context.settingsDataStore.data.map { prefs ->
            prefs[NOTIFICATION_POLLING_ENABLED_KEY] ?: true // デフォルトは ON
        }
    }

    suspend fun setNotificationPollingEnabled(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[NOTIFICATION_POLLING_ENABLED_KEY] = enabled
        }
    }
}
