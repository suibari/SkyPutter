package com.suibari.skyposter.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.notification.NotificationListNotificationsRequest
import androidx.core.content.edit
import com.suibari.skyposter.data.model.BskyPostActionRepository
import com.suibari.skyposter.ui.type.DisplayNotification
import com.suibari.skyposter.util.BskyUtil
import com.suibari.skyposter.util.SessionManager
import work.socialhub.kbsky.api.entity.com.atproto.repo.RepoGetRecordRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

class NotificationRepository (
    val context: Context
): BskyPostActionRepository() {
    private val prefs: SharedPreferences = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
    private var lastSeenNotifIndexedAt: String? = prefs.getString(KEY_LAST_SEEN, null) // 見た通知の最新時刻
    private var latestNotifIndexedAt: String? = null // 来た通知の最新時刻
    private val recordCache = mutableMapOf<String, FeedPost>()

    companion object {
        private const val KEY_LAST_SEEN = "last_seen_notif_indexed_at"
    }

    suspend fun fetchNotifications(limit: Int, cursor: String? = null): Pair<List<DisplayNotification>, String?> {
        Log.i("com.example.skyposter.NotificationRepository", "fetching notification, cursor: $cursor")
        // 通知取得
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

        // --- 1. reply 通知のみ URI 抽出してまとめて取得 ---
        val replyNotifs = notifs.filter { it.reason == "reply" }
        val replyUris = replyNotifs.mapNotNull { it.uri }
        val viewerStatusMap = fetchViewerStatusMap(replyUris)  // BskyPostActionRepository の共通関数

        // --- 2. DisplayNotification にマッピング ---
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
