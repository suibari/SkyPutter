package com.suibari.skyputter.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.notification.NotificationListNotificationsRequest
import androidx.core.content.edit
import com.suibari.skyputter.data.model.BskyPostActionRepository
import com.suibari.skyputter.ui.type.DisplayNotification
import com.suibari.skyputter.util.BskyUtil
import com.suibari.skyputter.util.SessionManager
import work.socialhub.kbsky.api.entity.com.atproto.repo.RepoGetRecordRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

class NotificationRepository (
    val context: Context
): BskyPostActionRepository() {
    private val prefs: SharedPreferences = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
    private var lastSeenNotifIndexedAt: String? = prefs.getString(KEY_LAST_SEEN, null)
    private var lastPolledNotifIndexedAt: String? = prefs.getString(KEY_LAST_POLLED, null) // ポーリングで最後に取得した通知の時刻
    private var latestNotifIndexedAt: String? = null
    private val recordCache = mutableMapOf<String, FeedPost>()

    // 通知済みCIDを永続化
    private val notifiedCids: MutableSet<String> = prefs.getStringSet(KEY_NOTIFIED_CIDS, emptySet())?.toMutableSet() ?: mutableSetOf()

    companion object {
        private const val KEY_LAST_SEEN = "last_seen_notif_indexed_at"
        private const val KEY_LAST_POLLED = "last_polled_notif_indexed_at"
        private const val KEY_NOTIFIED_CIDS = "notified_cids"
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
            val isNew = lastSeenNotifIndexedAt?.let { notif.indexedAt > it } ?: true
            val parentPostRecord = notif.record.asFeedPost?.reply?.parent
                ?: notif.record.asFeedRepost?.subject
                ?: notif.record.asFeedLike?.subject
            val rootPostRecord = notif.record.asFeedPost?.reply?.root
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

        latestNotifIndexedAt = notifs.firstOrNull()?.indexedAt

        return Pair(result, newCursor)
    }

    /**
     * バックグラウンド通知用：未通知の新しい通知のみを取得
     */
    suspend fun fetchNewNotificationsForPolling(): List<DisplayNotification> {
        val allNewNotifs = mutableListOf<DisplayNotification>()
        var cursor: String? = null
        var hasMore = true

        // lastPolledNotifIndexedAt以降の通知をすべて取得
        while (hasMore) {
            val (notifs, newCursor) = fetchNotifications(50, cursor)

            // lastPolledNotifIndexedAt以降の通知のみをフィルタ
            val newNotifs = if (lastPolledNotifIndexedAt != null) {
                notifs.filter { it.raw.indexedAt > lastPolledNotifIndexedAt!! }
            } else {
                notifs
            }

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
    }

    fun markAllAsRead() {
        latestNotifIndexedAt?.let {
            lastSeenNotifIndexedAt = it
            prefs.edit {
                putString(KEY_LAST_SEEN, it)
            }
        }
    }

    private suspend fun getRecord(refRecord: RepoStrongRef?): FeedPost? {
        return try {
            refRecord?.let { ref ->
                val uri = ref.uri
                recordCache[uri] ?: run {
                    val (repo, collection, rkey) = BskyUtil.parseAtUri(uri)
                        ?: return@let null
                    val record = SessionManager.runWithAuthRetry { auth ->
                        BlueskyFactory
                            .instance(BSKY_SOCIAL.uri)
                            .repo()
                            .getRecord(
                                RepoGetRecordRequest(repo, collection, rkey)
                            )
                    }
                    val feedPost = record.data.value.asFeedPost
                    feedPost?.also { recordCache[uri] = it }
                }
            }
        } catch (e: Exception) {
            Log.w("getRecord", "Record not found", e)
            return null
        }
    }
}