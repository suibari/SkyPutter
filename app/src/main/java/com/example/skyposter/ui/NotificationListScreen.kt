package com.example.skyposter.ui

import DisplayNotification
import NotificationViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
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
import com.example.skyposter.BskyUtil
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.com.atproto.repo.RepoGetRecordRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

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
    val reason = when {
        notif.record.asFeedPost != null -> "reply"
        notif.record.asFeedRepost != null -> "repost"
        notif.record.asFeedLike != null -> "like"
        else -> "unknown"
    }
    val post = notif.record.asFeedPost?.text
    val date = notif.indexedAt
    val rootPost = notification.rootPost

    Row(modifier = Modifier.padding(8.dp)) {
        // アバター
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(notif.author.avatar)
                .crossfade(true)
                .build(),
            contentDescription = "avatar",
            modifier = Modifier
                .size(48.dp)
        )

        // 通知アイコン
        when (reason) {
            "reply" -> Icon(Icons.Default.Share, contentDescription = "リプライ")
            "repost" -> Icon(Icons.Default.Refresh, contentDescription = "リポスト")
            "like" -> Icon(Icons.Default.FavoriteBorder, contentDescription = "リポスト")
            "unknown" -> Icon(Icons.Default.Notifications, contentDescription = "不明")
        }

        // 新規マーク
        if (notification.isNew) {
            Text("●", color = Color.Red)
        }

        // 通知内容、元ポスト
        Column(modifier = Modifier.padding(start = 16.dp)) {
            if (post != null) {
                Text(post, style = MaterialTheme.typography.bodyMedium)
            }
            Text(text = date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            if (rootPost != null) {
                Text(
                    text = rootPost.text ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
