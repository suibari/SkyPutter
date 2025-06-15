import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skyposter.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

class NotificationViewModel(
    private val repo: NotificationRepository
) : ViewModel() {
    private val _notifications = mutableStateListOf<DisplayNotification>()
    val notifications: List<DisplayNotification> = _notifications
    private var cursor: String? = null
    private var isLoading = false

    fun startPolling() {
        viewModelScope.launch {
            while (true) {
                val (newNotifs, newCursor) = repo.fetchNotifications(15)
                if (newNotifs.isNotEmpty()) {
                    _notifications.addAll(0, newNotifs)
                    sendDeviceNotification(newNotifs)
                    cursor = newCursor
                }
                delay(60_000)
            }
        }
    }

    fun loadInitialNotifications() {
        viewModelScope.launch {
            isLoading = true
            val (newNotifs, newCursor) = repo.fetchNotifications(10)
            _notifications.clear()
            _notifications.addAll(newNotifs)
            cursor = newCursor
            println("cursor: $cursor")
            isLoading = false
        }
    }

    fun loadMoreNotifications() {
        if (isLoading || cursor == null) return
        viewModelScope.launch {
            isLoading = true
            val (newNotifs, newCursor) = repo.fetchNotifications(10, cursor)
            _notifications.addAll(newNotifs)
            cursor = newCursor
            isLoading = false
        }
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
        _notifications.clear()
        _notifications.addAll(newNotifs)
        cursor = newCursor
    }

    suspend fun likePost(record: RepoStrongRef) {
        repo.likePost(record)
    }

    suspend fun repostPost(record: RepoStrongRef) {
        repo.repostPost(record)
    }
}
