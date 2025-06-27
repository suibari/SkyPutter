package com.suibari.skyputter.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.notification.NotificationListNotificationsRequest
import androidx.core.content.edit
import com.suibari.skyputter.data.model.BskyPostActionRepository
import com.suibari.skyputter.ui.type.DisplayNotification
import com.suibari.skyputter.util.SessionManager
import work.socialhub.kbsky.api.entity.app.bsky.notification.NotificationUpdateSeenRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import java.time.Instant

class NotificationRepository (
    val context: Context
): BskyPostActionRepository() {
    private val prefs: SharedPreferences = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
    private var lastSeenNotifIndexedAt: String? = prefs.getString(KEY_LAST_SEEN, null)
    private var lastPolledNotifIndexedAt: String? = prefs.getString(KEY_LAST_POLLED, null)
    private var latestNotifIndexedAt: String? = null

    // 通知済みCIDを永続化
    private val notifiedCids: MutableSet<String> = prefs.getStringSet(KEY_NOTIFIED_CIDS, emptySet())?.toMutableSet() ?: mutableSetOf()

    // アプリの初回起動時刻を記録
    private val appFirstLaunchTime: String by lazy {
        val saved = prefs.getString(KEY_FIRST_LAUNCH, null)
        if (saved == null) {
            val currentTime = Instant.now().toString()
            prefs.edit {
                putString(KEY_FIRST_LAUNCH, currentTime)
            }
            currentTime
        } else {
            saved
        }
    }

    companion object {
        private const val KEY_LAST_SEEN = "last_seen_notif_indexed_at"
        private const val KEY_LAST_POLLED = "last_polled_notif_indexed_at"
        private const val KEY_NOTIFIED_CIDS = "notified_cids"
        private const val KEY_FIRST_LAUNCH = "app_first_launch_time"
        private const val MAX_NOTIFIED_CIDS = 1000
    }

    suspend fun fetchNotifications(limit: Int, cursor: String? = null): Pair<List<DisplayNotification>, String?> {
        Log.i("NotificationRepository", "fetching notification, cursor: $cursor")

        val response = SessionManager.runWithAuthRetry { auth ->
            BlueskyFactory
                .instance(BSKY_SOCIAL.uri)
                .notification()
                .listNotifications(
                    NotificationListNotificationsRequest(auth).also {
                        it.limit = limit
                        it.cursor = cursor
                    }
                )
        }

        val notifs = response.data.notifications
        val newCursor = response.data.cursor

        val replyNotifs = notifs.filter { it.reason == "reply" }
        val replyUris = replyNotifs.mapNotNull { it.uri }
        val viewerStatusMap = fetchViewerStatusMap(replyUris)

        val result = notifs.map { notif ->
            val isNew = !notif.isRead
            // 親ポストを 返信 > 引用 > リポスト > いいね の順番で判定
            val parentPostRecord = notif.record.asFeedPost?.reply?.parent
                ?: notif.record.asFeedPost?.embed?.asRecord?.record
                ?: notif.record.asFeedRepost?.subject
                ?: notif.record.asFeedLike?.subject
            val rootPostRecord = notif.record.asFeedPost?.reply?.root

            // 共通クラスのgetRecordメソッドを使用
            val parentPost: FeedPost? = getRecord(parentPostRecord)

            val viewer = viewerStatusMap[notif.uri]

            DisplayNotification(
                raw = notif,
                isNew = isNew,
                parentPost = parentPost,
                parentPostRecord = parentPostRecord,
                rootPostRecord = rootPostRecord,
                likeUri = viewer?.like,
                repostUri = viewer?.repost,
            )
        }

        // updateSeen 用に最新の indexedAt を記録（isRead == false の中で最大のもの）
        latestNotifIndexedAt = notifs
            .filter { !it.isRead }
            .maxByOrNull { it.indexedAt }?.indexedAt

        return Pair(result, newCursor)
    }

    /**
     * バックグラウンド通知用：未通知の新しい通知のみを取得
     * 初回起動時刻以降の通知のみを対象にする
     */
    suspend fun fetchNewNotificationsForPolling(): List<DisplayNotification> {
        val allNewNotifs = mutableListOf<DisplayNotification>()
        var cursor: String? = null
        var hasMore = true

        // 初回起動時刻を基準時刻として使用
        val baselineTime = lastPolledNotifIndexedAt ?: appFirstLaunchTime

        Log.d("NotificationRepository", "Fetching notifications since: $baselineTime")

        // baselineTime以降の通知をすべて取得
        while (hasMore) {
            val (notifs, newCursor) = fetchNotifications(50, cursor)

            // baselineTime以降の通知のみをフィルタ
            val newNotifs = notifs.filter { it.raw.indexedAt > baselineTime }

            if (newNotifs.isEmpty()) {
                hasMore = false
            } else {
                allNewNotifs.addAll(newNotifs)
                cursor = newCursor

                // 古い通知に到達したら終了
                if (newNotifs.size < notifs.size) {
                    hasMore = false
                }
            }

            // 無限ループ防止
            if (cursor == null) {
                hasMore = false
            }
        }

        // 未通知のもののみをフィルタ
        val unnotifiedNotifs = allNewNotifs.filter { it.raw.cid !in notifiedCids }

        Log.d("NotificationRepository", "Found ${allNewNotifs.size} new notifications, ${unnotifiedNotifs.size} unnotified")

        // 最新の通知時刻を更新
        if (allNewNotifs.isNotEmpty()) {
            lastPolledNotifIndexedAt = allNewNotifs.maxByOrNull { it.raw.indexedAt }?.raw?.indexedAt
            prefs.edit {
                putString(KEY_LAST_POLLED, lastPolledNotifIndexedAt)
            }
        }

        return unnotifiedNotifs
    }

    /**
     * 通知済みとしてマーク
     */
    fun markAsNotified(notifs: List<DisplayNotification>) {
        notifs.forEach { notif ->
            notifiedCids.add(notif.raw.cid)
        }

        // サイズ制限
        if (notifiedCids.size > MAX_NOTIFIED_CIDS) {
            val excess = notifiedCids.size - MAX_NOTIFIED_CIDS
            val toRemove = notifiedCids.take(excess)
            notifiedCids.removeAll(toRemove.toSet())
        }

        // 永続化
        prefs.edit {
            putStringSet(KEY_NOTIFIED_CIDS, notifiedCids)
        }

        Log.d("NotificationRepository", "Marked ${notifs.size} notifications as notified")
    }

    suspend fun markAllAsRead() {
        latestNotifIndexedAt?.let {
            lastSeenNotifIndexedAt = it
            prefs.edit {
                putString(KEY_LAST_SEEN, it)
            }
        }
    }

    suspend fun updateSeenToLatest() {
        latestNotifIndexedAt?.let { indexedAt ->
            val seenAt = Instant.parse(indexedAt).plusSeconds(1).toString()

            SessionManager.runWithAuthRetry { auth ->
                BlueskyFactory
                    .instance(BSKY_SOCIAL.uri)
                    .notification()
                    .updateSeen(
                        NotificationUpdateSeenRequest(auth).also {
                            it.seenAt = seenAt
                        }
                    )
            }
        }
    }

    /**
     * レコードキャッシュをクリア（必要に応じて使用）
     */
    fun clearCache() {
        clearRecordCache()
    }

    /**
     * 初回起動時刻をリセット（デバッグ用）
     */
    fun resetFirstLaunchTime() {
        prefs.edit {
            remove(KEY_FIRST_LAUNCH)
            remove(KEY_LAST_POLLED)
        }
    }

    /**
     * 通知設定の状態を取得（デバッグ用）
     */
    fun getNotificationStatus(): Map<String, String?> {
        return mapOf(
            "firstLaunchTime" to appFirstLaunchTime,
            "lastPolledTime" to lastPolledNotifIndexedAt,
            "lastSeenTime" to lastSeenNotifIndexedAt,
            "notifiedCount" to notifiedCids.size.toString(),
            "recordCacheSize" to getRecordCacheSize().toString()
        )
    }
}