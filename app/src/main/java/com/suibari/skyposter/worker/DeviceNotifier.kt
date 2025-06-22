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

        // 新しい通知のみをフィルタリング
        val newNotifs = notifs.filter { it.isNew }

        // 各新しい通知に対して個別の通知を作成
        newNotifs.forEachIndexed { index, notif ->
            val replytext = notif.raw.record.asFeedPost?.text ?: ""
            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle(notif.raw.author.handle)
                .setContentText("${notif.raw.reason} $replytext from ${notif.raw.author.handle}")
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .build()

            // 各通知に異なるIDを使用（重複を避けるため）
            notificationManager.notify(index + 1, notification)
        }
    }
}