package com.example.skyposter.ui.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewModelScope
import com.example.skyposter.data.repository.DisplayNotification
import com.example.skyposter.data.repository.NotificationRepository
import com.example.skyposter.data.model.PaginatedListViewModel
import com.example.skyposter.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

class NotificationViewModel constructor(
    private val repo: NotificationRepository
) : PaginatedListViewModel<DisplayNotification>() {

    private var isPolling = false
    private val notifiedCids = mutableSetOf<String>() // ← 通知済みcidを記録

    override suspend fun fetchItems(
        limit: Int,
        cursor: String?
    ): Pair<List<DisplayNotification>, String?> {
        return repo.fetchNotifications(limit, cursor)
    }

    fun startPolling() {
        if (isPolling) return // すでに開始済みなら無視
        isPolling = true

        viewModelScope.launch {
            while (isPolling) {
                try {
                    val (notifs, newCursor) = repo.fetchNotifications(15)
                    val newNotifs = notifs.filter { it.isNew && it.raw.cid !in notifiedCids }
                    if (newNotifs.isNotEmpty()) {
                        _items.addAll(0, newNotifs)
                        sendDeviceNotification(newNotifs)

                        // 通知済みCIDを記録
                        notifiedCids.addAll(newNotifs.map { it.raw.cid })
                        if (notifiedCids.size > 1000) {
                            // メモリ消費防止のため古いIDを削除
                            val toRemove = notifiedCids.take(notifiedCids.size - 1000)
                            notifiedCids.removeAll(toRemove)
                        }
                    }
                    delay(60_000)
                } catch (e: Exception) {
                    // ネットワークなどの例外をキャッチしてロギング等
                    e.printStackTrace()
                    delay(60_000) // 次のリトライまで待機
                }
            }
        }
    }

    fun stopPolling() {
        isPolling = false
    }

    private fun sendDeviceNotification(notifs: List<DisplayNotification>) {
        val context = repo.context
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 通知チャンネル
        val channelId = "sky_notif_channel"
        val channelName = "通知"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setShowBadge(true) // バッジ表示を許可
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 通知内容（例：最新の通知のみ表示）
        val latest = notifs.first()
        if (latest.isNew) {
            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle("SkyPoster")
                .setContentText("${latest.raw.author.handle} から ${latest.raw.reason}")
                .setSmallIcon(R.drawable.ic_notification) // あなたのアプリアイコン
                .setAutoCancel(true)
                .setNumber(notifs.size)
                .build()

            notificationManager.notify(1, notification)
        }
    }

    suspend fun fetchNow() {
        repo.markAllAsRead()
        val (newNotifs, newCursor) = repo.fetchNotifications(15)
        _items.clear()
        _items.addAll(newNotifs)
        cursor = newCursor
    }

    suspend fun likePost(record: RepoStrongRef) {
        repo.likePost(record)

        val index = _items.indexOfFirst { it.raw.uri == record.uri }
        if (index != -1) {
            val item = _items[index]
            _items[index] = item.copy(isLiked = !(item.isLiked ?: false))
        }
    }

    suspend fun repostPost(record: RepoStrongRef) {
        repo.repostPost(record)

        val index = _items.indexOfFirst { it.raw.uri == record.uri }
        if (index != -1) {
            val item = _items[index]
            _items[index] = item.copy(isReposted = !(item.isReposted ?: false))
        }
    }
}
