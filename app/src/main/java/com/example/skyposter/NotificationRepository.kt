import android.app.Notification
import android.content.Context
import android.content.SharedPreferences
import com.example.skyposter.SessionManager
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.notification.NotificationListNotificationsRequest
import work.socialhub.kbsky.domain.Service
import work.socialhub.kbsky.model.app.bsky.notification.NotificationListNotificationsNotification
import androidx.core.content.edit

data class DisplayNotification(
    val raw: NotificationListNotificationsNotification,
    val isNew: Boolean
)

class NotificationRepository(
    private val sessionManager: SessionManager,
    val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("notifications", Context.MODE_PRIVATE)

    private var lastSeenNotifIndexedAt: String? =
        prefs.getString(KEY_LAST_SEEN, null)

    companion object {
        private const val KEY_LAST_SEEN = "last_seen_notif_indexed_at"
    }

    suspend fun fetchNewNotifications(): List<DisplayNotification> {
        println("[INFO] fetching new notifications...")
        val auth = sessionManager.getAuth() ?: return emptyList()
        val response = BlueskyFactory
            .instance(Service.BSKY_SOCIAL.uri)
            .notification()
            .listNotifications(
                NotificationListNotificationsRequest(auth)
            )
        val notifs = response.data.notifications

        // 未読判定
        val result = notifs.map { notif ->
            val isNew = if (lastSeenNotifIndexedAt == null) {
                true
            } else {
                notif.indexedAt > lastSeenNotifIndexedAt!!
            }
            DisplayNotification(notif, isNew)
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
}
