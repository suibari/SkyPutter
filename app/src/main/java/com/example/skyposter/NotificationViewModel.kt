import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skyposter.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

class NotificationViewModel(
    private val repo: NotificationRepository // Context → Repository に修正
) : ViewModel() {

    private val _notifications = mutableStateListOf<DisplayNotification>()
    val notifications: List<DisplayNotification> = _notifications

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
        repo.fetchNewNotifications()
    }

    suspend fun likePost(record: RepoStrongRef) {
        repo.likePost(record)
    }

    suspend fun repostPost(record: RepoStrongRef) {
        repo.repostPost(record)
    }
}
