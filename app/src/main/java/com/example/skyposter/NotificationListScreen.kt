import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import work.socialhub.kbsky.model.app.bsky.notification.NotificationListNotificationsNotification

@Composable
fun NotificationListScreen(viewModel: NotificationViewModel) {
    val notifications = viewModel.notifications
    val refreshing = remember { mutableStateOf(false) }

    SwipeRefresh(state = rememberSwipeRefreshState(refreshing.value), onRefresh = {
        refreshing.value = true
        // 強制更新ロジック
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.fetchNow()
            refreshing.value = false
        }
    }) {
        LazyColumn {
            items(notifications) { notif ->
                NotificationItem(notif)
            }
        }
    }
}

@Composable
fun NotificationItem(notification: DisplayNotification) {
    val notif = notification.raw
    Row(modifier = Modifier.padding(8.dp)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(notif.author.avatar)
                .crossfade(true)
                .build(),
            contentDescription = "avatar",
            modifier = Modifier
                .size(48.dp)
        )

        Column(modifier = Modifier.padding(start = 16.dp)) {
            Row {
                Text(
                    text = notif.reason ?: "通知",
                    style = MaterialTheme.typography.bodyLarge
                )
                if (notification.isNew) {
                    Text("●", color = Color.Red)
                }
            }
            Text(
                text = notif.author.handle ?: "unknown",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}