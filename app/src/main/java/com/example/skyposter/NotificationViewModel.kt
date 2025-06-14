import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import work.socialhub.kbsky.model.app.bsky.notification.NotificationListNotificationsNotification

class NotificationViewModel(
    private val repo: NotificationRepository // Context → Repository に修正
) : ViewModel() {

    private val _notifications = mutableStateListOf<NotificationListNotificationsNotification>()
    val notifications: List<NotificationListNotificationsNotification> = _notifications

    init {
        viewModelScope.launch {
            while (true) {
                val new = repo.fetchNewNotifications()
                if (new.isNotEmpty()) {
                    _notifications.addAll(0, new)
                    sendDeviceNotification(new)
                }
                delay(60_000) // 1分おき
            }
        }
    }

    private fun sendDeviceNotification(notifs: List<NotificationListNotificationsNotification>) {
        // 通知の送信はここで NotificationManager を使って実装
    }

    suspend fun fetchNow() {
        repo.fetchNewNotifications()
    }

    fun markAllAsRead() {
        _notifications.clear() // シンプルに全部既読にする処理（UI上）
    }
}
