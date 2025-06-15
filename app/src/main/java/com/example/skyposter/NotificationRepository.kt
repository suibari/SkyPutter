import android.content.Context
import android.content.SharedPreferences
import com.example.skyposter.SessionManager
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.notification.NotificationListNotificationsRequest
import work.socialhub.kbsky.model.app.bsky.notification.NotificationListNotificationsNotification
import androidx.core.content.edit
import com.example.skyposter.BskyUtil
import work.socialhub.kbsky.api.entity.com.atproto.repo.RepoGetRecordRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

data class DisplayNotification(
    val raw: NotificationListNotificationsNotification,
    val isNew: Boolean,
    val rootPost: FeedPost? = null
)

class NotificationRepository(
    private val sessionManager: SessionManager,
    val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
    private var lastSeenNotifIndexedAt: String? =
        prefs.getString(KEY_LAST_SEEN, null)
    private val recordCache = mutableMapOf<String, FeedPost>()

    companion object {
        private const val KEY_LAST_SEEN = "last_seen_notif_indexed_at"
    }

    suspend fun fetchNewNotifications(): List<DisplayNotification> {
        println("[INFO] fetching new notifications...")
        val auth = sessionManager.getAuth() ?: return emptyList()
        val response = BlueskyFactory
            .instance(BSKY_SOCIAL.uri)
            .notification()
            .listNotifications(
                NotificationListNotificationsRequest(auth)
            )
        val notifs = response.data.notifications

        // 未読判定
        val result = notifs.map { notif ->
            val isNew = lastSeenNotifIndexedAt?.let { notif.indexedAt > it } ?: true

            val rootPostRecord = notif.record.asFeedPost?.reply?.parent
                ?: notif.record.asFeedRepost?.subject
                ?: notif.record.asFeedLike?.subject

            val rootPost: FeedPost? = try {
                rootPostRecord?.let { ref ->
                    val uri = ref.uri
                    recordCache[uri] ?: run {
                        val (repo, collection, rkey) = BskyUtil.parseAtUri(uri)
                            ?: return@let null
                        val record = BlueskyFactory
                            .instance(BSKY_SOCIAL.uri)
                            .repo()
                            .getRecord(
                                RepoGetRecordRequest(repo, collection, rkey)
                            )
                        val feedPost = record.data.value.asFeedPost
                        feedPost?.also { recordCache[uri] = it }
                    }
                }
            } catch (e: Exception) {
                throw e
            }

            DisplayNotification(
                raw = notif,
                isNew = isNew,
                rootPost = rootPost
            )
        }

        // 最新既読をセット
        if (notifs.isNotEmpty()) {
            val newest = notifs.first().indexedAt
            lastSeenNotifIndexedAt = newest
            prefs.edit { putString(KEY_LAST_SEEN, newest) }
        }

        return result
    }

    fun markAllAsRead() {
        // 一括既読処理用: 現在の最新時刻を保存
        lastSeenNotifIndexedAt?.let {
            prefs.edit { putString(KEY_LAST_SEEN, it) }
        }
    }

    private fun getRecord(record: RepoStrongRef): FeedPost? {
        val uriSplit = BskyUtil.parseAtUri(record.uri)

        val response = BlueskyFactory
            .instance(BSKY_SOCIAL.uri)
            .repo()
            .getRecord(
                RepoGetRecordRequest(
                    repo = uriSplit?.first ?: "",
                    collection = uriSplit?.second ?: "",
                    rkey = uriSplit?.third ?: ""
                )
            )
        return response.data.value.asFeedPost
    }
}
