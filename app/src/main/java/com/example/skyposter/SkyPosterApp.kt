package com.example.skyposter

import android.app.Application

class SkyPosterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.initialize(this)
        scheduleNotificationWorker(this)
    }
}