import android.app.Notification
import com.example.skyposter.SessionManager
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.notification.NotificationListNotificationsRequest
import work.socialhub.kbsky.domain.Service
import work.socialhub.kbsky.model.app.bsky.notification.NotificationListNotificationsNotification

class NotificationRepository(private val sessionManager: SessionManager) {
    private var lastSeenNotifId: String? = null

    suspend fun fetchNewNotifications(): List<NotificationListNotificationsNotification> {
        println("[INFO] fetching new notifications...")
        val auth = sessionManager.getAuth() ?: return emptyList()
        val response = BlueskyFactory
            .instance(Service.BSKY_SOCIAL.uri)
            .notification()
            .listNotifications(
                NotificationListNotificationsRequest(auth)
            )

        val notifs = response.data.notifications

        return notifs
    }
}
