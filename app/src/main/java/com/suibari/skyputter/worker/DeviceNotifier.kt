package com.suibari.skyputter.worker

import android.app.NotificationManager
import android.content.Context
import android.app.NotificationChannel
import android.os.Build
import androidx.core.app.NotificationCompat
import android.app.PendingIntent
import android.content.Intent
import com.suibari.skyputter.R
import com.suibari.skyputter.ui.type.DisplayNotification
import com.suibari.skyputter.MainActivity

class DeviceNotifier(private val context: Context) {
    private val channelId = "skyputter_notif_channel"
    private val channelName = "通知"
    private val groupKey = "com.suibari.skyputter.NOTIFICATION_GROUP"
    private val summaryId = 0

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setShowBadge(true)
                description = "Notifications from Bluesky"
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun notify(notifs: List<DisplayNotification>) {
        if (notifs.isEmpty()) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 新しい通知のみをフィルタリング
        val newNotifs = notifs.filter { it.isNew }
        if (newNotifs.isEmpty()) return

        // アプリを開くPendingIntent
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 既存の通知数を取得（概算）
        val totalActiveNotifications = getTotalActiveNotifications(notificationManager)
        val willHaveMultipleNotifications = totalActiveNotifications > 0 || newNotifs.size > 1

        // 常にグループ化を前提とする
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android 7.0以上：常にグループ化
            newNotifs.forEachIndexed { index, notif ->
                val notificationId = generateNotificationId(notif)
                createIndividualNotification(notif, notificationId, pendingIntent, notificationManager, useGroup = true)
            }

            // サマリー通知を更新（全ての通知情報を含む）
            createSummaryNotification(notifs, pendingIntent, notificationManager, useGroup = true)
        } else {
            // Android 7.0未満：複数ある場合はサマリーのみ、1件のみの場合は個別
            if (willHaveMultipleNotifications) {
                createSummaryNotification(notifs, pendingIntent, notificationManager, useGroup = false)
            } else {
                createIndividualNotification(newNotifs[0], 1, pendingIntent, notificationManager, useGroup = false)
            }
        }
    }

    private fun getTotalActiveNotifications(notificationManager: NotificationManager): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // アクティブな通知数を取得（サマリー通知を除く）
            notificationManager.activeNotifications.count {
                it.id != summaryId && it.groupKey == groupKey
            }
        } else {
            // Android 6.0未満では取得できないため、0と仮定
            0
        }
    }

    private fun generateNotificationId(notif: DisplayNotification): Int {
        // 通知の一意性を保つためのID生成
        // URIやcidを使用してハッシュ値を生成
        return notif.raw.uri.hashCode()
    }

    private fun createIndividualNotification(
        notif: DisplayNotification,
        notificationId: Int,
        pendingIntent: PendingIntent,
        notificationManager: NotificationManager,
        useGroup: Boolean = true
    ) {
        val displayName = notif.raw.author.displayName ?: notif.raw.author.handle

        val (title, text) = when (notif.raw.reason) {
            "like" -> {
                val originalPost = getOriginalPostText(notif)
                Pair("$displayName liked your post", originalPost)
            }
            "repost" -> {
                val originalPost = getOriginalPostText(notif)
                Pair("$displayName reposted your post", originalPost)
            }
            "follow" -> {
                Pair("$displayName followed you", "")
            }
            "reply" -> {
                val replyText = notif.raw.record.asFeedPost?.text ?: ""
                Pair("$displayName replied to your post", replyText)
            }
            "mention" -> {
                val mentionText = notif.raw.record.asFeedPost?.text ?: ""
                Pair("$displayName mentioned you", mentionText)
            }
            "quote" -> {
                val quoteText = notif.raw.record.asFeedPost?.text ?: ""
                Pair("$displayName quoted your post", quoteText)
            }
            else -> {
                Pair("$displayName", "New notification")
            }
        }

        // URI をIntentに埋め込む
        val detailIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("postUri", notif.raw.uri) // 通知選択時にMainにURIを渡すため
        }

        val detailPendingIntent = PendingIntent.getActivity(
            context, notificationId, detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(detailPendingIntent)
            .setWhen(System.currentTimeMillis()) // タイムスタンプを設定
            .setShowWhen(true)
            .setStyle(if (text.isNotEmpty()) NotificationCompat.BigTextStyle().bigText(text) else null)

        if (useGroup) {
            builder.setGroup(groupKey)
        }

        notificationManager.notify(notificationId, builder.build())
    }

    private fun getOriginalPostText(notif: DisplayNotification): String {
        return notif.parentPost?.text ?: ""
    }

    private fun createSummaryNotification(
        allNotifs: List<DisplayNotification>,
        pendingIntent: PendingIntent,
        notificationManager: NotificationManager,
        useGroup: Boolean = true
    ) {
        // 新しい通知の総数を使用
        val newCount = allNotifs.count { it.isNew }
        val count = maxOf(newCount, 1) // 最低1件として扱う

        val title = if (count == 1) {
            "New notification"
        } else {
            "$count new notifications"
        }

        // InboxStyleで最新の通知を表示
        val inboxStyle = NotificationCompat.InboxStyle()

        // 昇順ソートで正しい順に並ぶはず: 降順ソートだと1枠で2件以上通知が来ると逆順になる
        val recentNotifs = allNotifs
            .sortedBy { it.raw.indexedAt }
            .takeLast(5)

        recentNotifs.forEach { notif ->
            val displayName = notif.raw.author.displayName ?: notif.raw.author.handle
            val reasonText = when (notif.raw.reason) {
                "like" -> "liked your post"
                "repost" -> "reposted your post"
                "follow" -> "followed you"
                "reply" -> "replied to your post"
                "mention" -> "mentioned you"
                "quote" -> "quoted your post"
                else -> "notification"
            }
            val line = "$displayName $reasonText"
            inboxStyle.addLine(line)
        }

        if (count > 5) {
            inboxStyle.setSummaryText("and ${count - 5} more")
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText("You have $count new notifications")
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setStyle(inboxStyle)
            .setNumber(count)

        if (useGroup) {
            builder.setGroup(groupKey)
                .setGroupSummary(true)
        }

        notificationManager.notify(summaryId, builder.build())
    }

    fun clearNotifications() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }
}