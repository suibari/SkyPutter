package com.suibari.skyputter.data.repository

import android.content.Context

object NotificationRepoProvider {
    private var instance: NotificationRepository? = null

    fun getInstance(context: Context): NotificationRepository {
        return instance ?: NotificationRepository(context.applicationContext).also {
            instance = it
        }
    }
}
