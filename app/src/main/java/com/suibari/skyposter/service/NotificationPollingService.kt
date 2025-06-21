package com.suibari.skyposter.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.suibari.skyposter.R
import com.suibari.skyposter.data.repository.NotificationRepository
import com.suibari.skyposter.worker.DeviceNotifier
import kotlinx.coroutines.*

class NotificationPollingService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private lateinit var repo: NotificationRepository
    private lateinit var deviceNotifier: DeviceNotifier

    companion object {
        const val CHANNEL_ID = "notification_polling_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "START_POLLING"
        const val ACTION_STOP = "STOP_POLLING"

        fun startService(context: Context) {
            val intent = Intent(context, NotificationPollingService::class.java)
            intent.action = ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, NotificationPollingService::class.java)
            intent.action = ACTION_STOP
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repo = NotificationRepository(this)
        deviceNotifier = DeviceNotifier(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundService()
                startPolling()
            }
            ACTION_STOP -> {
                stopPolling()
                stopSelf()
            }
        }
        return START_STICKY // サービスが強制終了されても再起動
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "通知ポーリングサービス",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "バックグラウンドで新しい通知をチェックします"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SkyPoster")
            .setContentText("通知をチェック中...")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            while (isActive) {
                try {
                    val newNotifs = repo.fetchNewNotificationsForPolling()

                    if (newNotifs.isNotEmpty()) {
                        deviceNotifier.notify(newNotifs)
                        repo.markAsNotified(newNotifs)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                delay(60_000) // 1分待機
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}