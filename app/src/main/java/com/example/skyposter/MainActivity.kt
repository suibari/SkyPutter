package com.example.skyposter

import LoadingScreen
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.skyposter.ui.theme.SkyPosterTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SkyPosterTheme {
                val context = LocalContext.current
                val app = context.applicationContext as SkyPosterApp
                val sessionManager = app.sessionManager

                var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }

                // 起動時セッション確認
                LaunchedEffect(Unit) {
                    isLoggedIn = sessionManager.hasSession()
                }

                if (isLoggedIn == true) {
                    scheduleNotificationWorker(context)
                }

                when (isLoggedIn) {
                    true -> MainScreen(app)
                    false -> LoginScreen (app, onLoginSuccess = {isLoggedIn = true} )
                    null -> LoadingScreen()
                }
            }
        }
    }
}

fun scheduleNotificationWorker(context: Context) {
    val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
        15, TimeUnit.MINUTES // Android制限：最小15分間隔
    ).build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "notification_worker",
        ExistingPeriodicWorkPolicy.KEEP, // すでに登録されていれば上書きしない
        workRequest
    )
}