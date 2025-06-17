package com.example.skyposter

import android.app.Application
import com.example.skyposter.util.SessionManager

class SkyPosterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.initialize(this)
        scheduleNotificationWorker(this)
    }
}