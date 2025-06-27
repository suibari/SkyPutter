package com.suibari.skyputter.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore by preferencesDataStore(name = "settings")

object NotificationSettings {
    private val NOTIFICATION_POLLING_ENABLED_KEY = booleanPreferencesKey("notification_enabled")

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
