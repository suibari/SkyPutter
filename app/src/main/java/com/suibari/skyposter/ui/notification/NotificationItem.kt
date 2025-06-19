package com.suibari.skyposter.ui.notification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suibari.skyposter.data.repository.DisplayFeed
import com.suibari.skyposter.data.repository.DisplayNotification
import com.suibari.skyposter.ui.post.DisplayActions
import com.suibari.skyposter.ui.post.DisplayContent
import com.suibari.skyposter.ui.post.DisplayHeader
import com.suibari.skyposter.ui.post.DisplayParentPost
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

//@Composable
//fun NotificationItem(
//    notification: DisplayNotification,
//    onReply: (parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost) -> Unit,
//    onLike: (parentRecord: RepoStrongRef) -> Unit,
//    onRepost: (parentRecord: RepoStrongRef) -> Unit,
//) {
//    val notif = notification.raw
//    val reason = when {
//        notif.record.asFeedPost != null -> "reply"
//        notif.record.asFeedRepost != null -> "repost"
//        notif.record.asFeedLike != null -> "like"
//        else -> "unknown"
//    }
//    val post = notif.record.asFeedPost?.text
//    val date = notif.indexedAt
//    val parentPost = notification.parentPost
//    val subjectRef = RepoStrongRef(notification.raw.uri, notification.raw.cid)
//    val subjectRecord = notification.raw.record.asFeedPost
//
//    val likeColor = if (notification.isLiked) Color.Red else Color.Black
//    val repostColor = if (notification.isReposted) Color.Green else Color.Black
//
//    Row(modifier = Modifier.padding(8.dp)) {
//        // アバター
//        AsyncImage(
//            model = ImageRequest.Builder(LocalContext.current)
//                .data(notif.author.avatar)
//                .crossfade(true)
//                .build(),
//            contentDescription = "avatar",
//            modifier = Modifier
//                .size(48.dp)
//        )
//
//        // 通知アイコン
//        when (reason) {
//            "reply" -> Icon(Icons.Default.Share, contentDescription = "リプライ")
//            "repost" -> Icon(Icons.Default.Refresh, contentDescription = "リポスト")
//            "like" -> Icon(Icons.Default.FavoriteBorder, contentDescription = "リポスト")
//            "unknown" -> Icon(Icons.Default.Notifications, contentDescription = "不明")
//        }
//
//        // 新規マーク
//        if (notification.isNew) {
//            Text("●", color = Color.Red)
//        }
//
//        Column(modifier = Modifier.padding(start = 16.dp)) {
//            // 名前
//            Text(text = notification.raw.author.displayName ?: "", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
//
//            // リプライ内容
//            if (post != null) {
//                Text(post, style = MaterialTheme.typography.bodyMedium)
//            }
//            Text(text = date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
//
//            // アクションボタン: リプライだった場合にのみ表示
//            if (
//                notification.parentPostRecord != null &&
//                notification.rootPostRecord != null &&
//                subjectRecord != null
//            ) {
//
//                Row(modifier = Modifier.padding(top = 8.dp)) {
//                    Icon (
//                        Icons.Default.Share,
//                        contentDescription = "リプライ",
//                        modifier = Modifier
//                            .padding(end = 8.dp)
//                            .clickable {
//                                onReply(subjectRef, notification.rootPostRecord, subjectRecord)
//                            }
//                    )
//                    Icon (
//                        Icons.Default.FavoriteBorder,
//                        contentDescription = "いいね",
//                        tint = likeColor,
//                        modifier = Modifier
//                            .padding(end = 8.dp)
//                            .clickable {
//                                onLike(subjectRef)
//                            }
//                    )
//                    Icon (
//                        Icons.Default.Refresh,
//                        contentDescription = "リポスト",
//                        tint = repostColor,
//                        modifier = Modifier
//                            .clickable {
//                                onRepost(subjectRef)
//                            }
//                    )
//                }
//            }
//
//            // 通知元ポスト
//            if (parentPost != null) {
//                Text(
//                    text = parentPost.text ?: "",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = Color.Gray,
//                    modifier = Modifier.padding(top = 8.dp)
//                )
//            }
//        }
//    }
//}


@Composable
fun NotificationItem(
    notification: DisplayNotification,
    onReply: (parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost) -> Unit,
    onLike: (parentRecord: RepoStrongRef) -> Unit,
    onRepost: (parentRecord: RepoStrongRef) -> Unit,
) {
    val record = notification.raw.record

    val subjectRef = RepoStrongRef(notification.raw.uri, notification.raw.cid)
    val rootRef = notification.rootPostRecord ?: subjectRef

    val images = notification.raw.record.asFeedPost?.embed?.asImages?.images

    Row (modifier = Modifier.padding(start = 8.dp)) {
        // 自分ポスト欄
        DisplayHeader(
            avatarUrl = notification.raw.author.avatar,
            showNewMark = notification.isNew,
            reason = notification.raw.reason
        )

        Column {
            DisplayContent(
                text = record.asFeedPost?.text,
                authorName = notification.raw.author.displayName,
                images = null,
                date = notification.raw.indexedAt
            )

            DisplayActions(
                isMyPost = false,
                isLiked = notification.isLiked,
                isReposted = notification.isReposted,
                subjectRef = subjectRef,
                rootRef = rootRef,
                feed = notification.raw.record.asFeedPost,
                onReply = onReply,
                onLike = onLike,
                onRepost = onRepost
            )

            // 親ポスト欄
            val parentPost = notification.parentPost
            DisplayParentPost(parentPost?.text)
        }
    }
}
