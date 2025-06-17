package com.example.skyposter.ui.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.skyposter.data.repository.DisplayFeed
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

@Composable
fun PostItem(
    feed: DisplayFeed,
    myDid: String,
    onReply: ((parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost) -> Unit)?,
    onLike: ((parentRecord: RepoStrongRef) -> Unit)?,
    onRepost: ((parentRecord: RepoStrongRef) -> Unit)?,
) {
    val record = feed.raw.post.record?.asFeedPost!!
    val isMyPost = feed.raw.post.author?.did == myDid
    val selectedImage = remember { mutableStateOf<String?>(null) }

    Row(modifier = Modifier.padding(8.dp)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(feed.raw.post.author?.avatar)
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
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = feed.raw.post.indexedAt ?: "unknown",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            // --- 画像表示 ---
            val embed = feed.raw.post.embed
            if (embed?.asImages != null) {
                val images = embed.asImages?.images
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    images?.forEach { image ->
                        AsyncImage(
                            model = image.thumb,
                            contentDescription = image.alt,
                            modifier = Modifier
                                .size(100.dp)
                                .padding(end = 4.dp)
                                .clickable { selectedImage.value = image.fullsize }
                        )
                    }
                }

                selectedImage.value?.let { imageUrl ->
                    ZoomableImageDialog(
                        imageUrl = imageUrl,
                        onDismiss = { selectedImage.value = null }
                    )
                }
            }

            // --- 引用ポスト表示 ---
            if (embed?.asRecord != null) {
                val quoted = embed.asRecord?.record?.asRecord
                if (quoted != null) {
                    Column(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = quoted.value?.asFeedPost?.text ?: "(引用ポスト)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // 自分以外のポストにはアクションボタンを表示
            if (!isMyPost) {
                val subjectRef = RepoStrongRef(feed.raw.post.uri!!, feed.raw.post.cid!!)
                val rootRef: RepoStrongRef
                if (feed.raw.reply?.root?.uri != null && feed.raw.reply?.root?.cid != null) {
                    rootRef = RepoStrongRef(feed.raw.reply?.root?.uri!!, feed.raw.reply?.root?.cid!!)
                } else {
                    rootRef = subjectRef
                }
                val likeColor = if (feed.isLiked == true) Color.Red else Color.Black
                val repostColor = if (feed.isReposted == true) Color.Green else Color.Black

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
                        tint = likeColor,
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
                        tint = repostColor,
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

@Composable
fun PostImages(images: List<String>) {
    val selectedImage = remember { mutableStateOf<String?>(null) }

    Column {
        Row(modifier = Modifier.padding(top = 8.dp)) {
            images.forEach { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .padding(end = 4.dp)
                        .clickable { selectedImage.value = imageUrl }
                )
            }
        }

        selectedImage.value?.let { imageUrl ->
            ZoomableImageDialog(
                imageUrl = imageUrl,
                onDismiss = { selectedImage.value = null }
            )
        }
    }
}

@Composable
fun ZoomableImageDialog(imageUrl: String, onDismiss: () -> Unit) {
    val scale = remember { mutableStateOf(1f) }
    val zoomState = rememberTransformableState { zoomChange, _, _ ->
        scale.value *= zoomChange
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .graphicsLayer(
                        scaleX = scale.value.coerceIn(1f, 5f),
                        scaleY = scale.value.coerceIn(1f, 5f)
                    )
                    .transformable(state = zoomState)
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

