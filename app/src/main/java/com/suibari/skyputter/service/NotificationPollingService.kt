package com.suibari.skyputter.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.suibari.skyputter.R
import com.suibari.skyputter.data.repository.NotificationRepository
import com.suibari.skyputter.worker.DeviceNotifier
import kotlinx.coroutines.*

class NotificationPollingService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private lateinit var repo: NotificationRepository
    private lateinit var deviceNotifier: DeviceNotifier

    companion object {
        private const val TAG = "NotificationPollingService"
        const val CHANNEL_ID = "notification_polling_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "START_POLLING"
        const val ACTION_STOP = "STOP_POLLING"

        fun startService(context: Context) {
            Log.d(TAG, "startService called")
            val intent = Intent(context, NotificationPollingService::class.java)
            intent.action = ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Starting foreground service (Android O+)")
                context.startForegroundService(intent)
            } else {
                Log.d(TAG, "Starting regular service (Android < O)")
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            Log.d(TAG, "stopService called")
            val intent = Intent(context, NotificationPollingService::class.java)
            intent.action = ACTION_STOP
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate called")
        try {
            repo = NotificationRepository(this)
            deviceNotifier = DeviceNotifier(this)
            createNotificationChannel()
            Log.d(TAG, "onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "Starting polling service")
                try {
                    startForegroundService()
                    startPolling()
                    Log.d(TAG, "Polling service started successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting polling service", e)
                }
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping polling service")
                stopPolling()
                stopSelf()
            }
            null -> {
                Log.w(TAG, "Service restarted with null intent - restarting polling")
                try {
                    startForegroundService()
                    startPolling()
                } catch (e: Exception) {
                    Log.e(TAG, "Error restarting polling service", e)
                }
            }
        }
        Log.d(TAG, "onStartCommand returning START_STICKY")
        return START_STICKY // サービスが強制終了されても再起動
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind called")
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Creating notification channel")
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
            Log.d(TAG, "Notification channel created")
        } else {
            Log.d(TAG, "Skipping notification channel creation (Android < O)")
        }
    }

    private fun startForegroundService() {
        Log.d(TAG, "Starting foreground service")
        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SkyPutter")
                .setContentText("通知をチェック中...")
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Foreground service started with notification")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }
    }

    private fun startPolling() {
        Log.d(TAG, "startPolling called")
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            Log.d(TAG, "Polling coroutine started")
            var pollCount = 0
            while (isActive) {
                try {
                    pollCount++
                    Log.d(TAG, "Polling iteration #$pollCount - checking for new notifications...")

                    val startTime = System.currentTimeMillis()
                    val newNotifs = repo.fetchNewNotificationsForPolling()
                    val fetchTime = System.currentTimeMillis() - startTime

                    Log.d(TAG, "Found ${newNotifs.size} new notifications (fetch took ${fetchTime}ms)")

                    if (newNotifs.isNotEmpty()) {
                        Log.d(TAG, "Processing ${newNotifs.size} new notifications...")
                        val notifyStartTime = System.currentTimeMillis()
                        deviceNotifier.notify(newNotifs)
                        val notifyTime = System.currentTimeMillis() - notifyStartTime

                        repo.markAsNotified(newNotifs)
                        Log.d(TAG, "Notifications sent and marked as notified (notify took ${notifyTime}ms)")
                    } else {
                        Log.d(TAG, "No new notifications found")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during polling iteration #$pollCount", e)
                    e.printStackTrace()
                }

                Log.d(TAG, "Waiting 60 seconds before next poll...")
                delay(60_000) // 1分待機
            }
            Log.d(TAG, "Polling coroutine ended")
        }
        Log.d(TAG, "Polling job created and started")
    }

    private fun stopPolling() {
        Log.d(TAG, "stopPolling called")
        pollingJob?.cancel()
        pollingJob = null
        Log.d(TAG, "Polling job cancelled")
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed and scope cancelled")
    }
}