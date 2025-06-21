package com.suibari.skyposter.ui.post

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
import androidx.compose.material.icons.filled.Notifications
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
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

data class DisplayImage (
    val urlThumb: String,
    val urlFullsize: String,
    val alt: String?,
)

@Composable
fun DisplayHeader(
    avatarUrl: String?,
    reason: String? = null,
    showNewMark: Boolean = false
) {
    Row (modifier = Modifier.padding(8.dp)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = "avatar",
            modifier = Modifier.size(48.dp)
        )

        if (reason != null) {
            when (reason) {
                "reply" -> Icon(Icons.Default.Share, contentDescription = "リプライ")
                "repost" -> Icon(Icons.Default.Refresh, contentDescription = "リポスト")
                "like" -> Icon(Icons.Default.FavoriteBorder, contentDescription = "リポスト")
                "unknown" -> Icon(Icons.Default.Notifications, contentDescription = "不明")
            }
        }

        Column(modifier = Modifier.padding(start = 8.dp)) {
            Row {
                if (showNewMark) {
                    Text(" ●", color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun DisplayContent(text: String?, authorName: String?, images: List<DisplayImage>?, date: String?) {
    val selectedImage = remember { mutableStateOf<String?>(null) }

    if (!authorName.isNullOrBlank()) {
        Text(text = authorName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
    if (!text.isNullOrBlank()) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }

    if (!images.isNullOrEmpty()) {
        DisplayImages(images) { selectedImage.value = it }
        selectedImage.value?.let { imageUrl ->
            ZoomableImageDialog(
                imageUrl = imageUrl,
                onDismiss = { selectedImage.value = null }
            )
        }
    }

    if (!date.isNullOrBlank()) {
        Text(text = date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun DisplayImages(
    images: List<DisplayImage>?,
    onImageClick: (String?) -> Unit
) {
    if (!images.isNullOrEmpty()) {
        Row {
            images.forEach { image ->
                AsyncImage(
                    model = image.urlThumb,
                    contentDescription = image.alt,
                    modifier = Modifier
                        .size(100.dp)
                        .padding(end = 4.dp)
                        .clickable { onImageClick(image.urlFullsize) }
                )
            }
        }
    }
}

@Composable
fun ZoomableImageDialog(imageUrl: String?, onDismiss: (() -> Unit)?) {
    val scale = remember { mutableStateOf(1f) }
    val zoomState = rememberTransformableState { zoomChange, _, _ ->
        scale.value *= zoomChange
    }

    if (onDismiss != null) {
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
}

@Composable
fun DisplayActions(
    isMyPost: Boolean,
    isLiked: Boolean,
    isReposted: Boolean,
    subjectRef: RepoStrongRef,
    rootRef: RepoStrongRef,
    feed: FeedPost?,
    onReply: ((RepoStrongRef, RepoStrongRef, FeedPost) -> Unit)?,
    onLike: ((RepoStrongRef) -> Unit)?,
    onRepost: ((RepoStrongRef) -> Unit)?
) {
    if (!isMyPost) {
        val likeColor = if (isLiked) Color.Red else Color.Black
        val repostColor = if (isReposted) Color.Green else Color.Black

        Row(modifier = Modifier.padding(top = 8.dp)) {
            if (feed?.asFeedPost != null) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "リプライ",
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable {
                            onReply?.invoke(subjectRef, rootRef, feed)
                        }
                )
            }
            if (feed != null) {
                Icon(
                    Icons.Default.FavoriteBorder,
                    contentDescription = "いいね",
                    tint = likeColor,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable {
                            onLike?.invoke(
                                subjectRef,
                            )
                        }
                )
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "リポスト",
                    tint = repostColor,
                    modifier = Modifier.clickable {
                        onRepost?.invoke(
                            subjectRef,
                        )
                    }
                )
            }
        }
    }
}


@Composable
fun DisplayParentPost(text: String?) {
    if (!text.isNullOrBlank()) {
        Column(
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
