package com.suibari.skyposter.worker

import android.app.NotificationManager
import android.content.Context
import android.app.NotificationChannel
import android.os.Build
import androidx.core.app.NotificationCompat
import com.suibari.skyposter.R
import com.suibari.skyposter.ui.type.DisplayNotification

class DeviceNotifier(private val context: Context) {
    private val channelId = "sky_notif_channel"
    private val channelName = "通知"

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setShowBadge(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun notify(notifs: List<DisplayNotification>) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val latest = notifs.first()
        if (latest.isNew) {
            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle("SkyPoster")
                .setContentText("${latest.raw.author.handle} から ${latest.raw.reason}")
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setNumber(notifs.size)
                .build()

            notificationManager.notify(1, notification)
        }
    }
}
