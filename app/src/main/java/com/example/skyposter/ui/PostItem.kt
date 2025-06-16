package com.example.skyposter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsFeedViewPost
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

@Composable
fun PostItem(
    feed: FeedDefsFeedViewPost,
    myDid: String,
    onReply: ((parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost) -> Unit)?,
    onLike: ((parentRecord: RepoStrongRef) -> Unit)?,
    onRepost: ((parentRecord: RepoStrongRef) -> Unit)?,
) {
    val record = feed.post.record?.asFeedPost!!
    val isMyPost = feed.post.author?.did == myDid

    if (isMyPost) {
        // 自分のポストにはアクションボタンを非表示
        Row(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(feed.post.author?.avatar)
                    .crossfade(true)
                    .build(),
                contentDescription = "avatar",
                modifier = Modifier
                    .size(48.dp)
            )

            Column(modifier = Modifier.padding(start = 16.dp)) {
                Row {
                    Text(
                        text = record.text ?: "",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    text = feed.post.indexedAt ?: "unknown",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    } else {
        // 自分以外のポストにはアクションボタンを表示
        val subjectRef = RepoStrongRef(feed.post.uri!!, feed.post.cid!!)
        val rootRef: RepoStrongRef
        if (feed.reply?.root?.uri != null && feed.reply?.root?.cid != null) {
            rootRef = RepoStrongRef(feed.reply?.root?.uri!!, feed.reply?.root?.cid!!)
        } else {
            rootRef = subjectRef
        }

        Row(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(feed.post.author?.avatar)
                    .crossfade(true)
                    .build(),
                contentDescription = "avatar",
                modifier = Modifier
                    .size(48.dp)
            )

            Column(modifier = Modifier.padding(start = 16.dp)) {
                Row {
                    Text(
                        text = record.text ?: "",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    text = feed.post.indexedAt ?: "unknown",
                    style = MaterialTheme.typography.bodySmall
                )

                Row(modifier = Modifier.padding(top = 8.dp)) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "リプライ",
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable {
                                if (onReply != null) {
                                    onReply(subjectRef, rootRef, record)
                                }
                            }
                    )
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = "いいね",
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable {
                                if (onLike != null) {
                                    onLike(subjectRef)
                                }
                            }
                    )
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "リポスト",
                        modifier = Modifier
                            .clickable {
                                if (onRepost != null) {
                                    onRepost(subjectRef)
                                }
                            }
                    )

                }
            }
        }
    }
}
