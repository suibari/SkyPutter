package com.suibari.skyputter.data.model

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
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
import com.suibari.skyputter.ui.theme.itemPadding
import com.suibari.skyputter.ui.theme.screenPadding
import com.suibari.skyputter.ui.theme.spacePadding
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileView
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
    Row {
        val context = LocalContext.current
        val imageRequest = remember(avatarUrl) {
            ImageRequest.Builder(context)
                .data(avatarUrl)
                .crossfade(true)
                .build()
        }

        AsyncImage(
            model = imageRequest,
            contentDescription = "avatar",
            modifier = Modifier.size(48.dp)
        )

        Spacer (Modifier.spacePadding)

        Column {
            if (reason != null) {
                when (reason) {
                    "reply" -> Icon(
                        Icons.Default.Share,
                        contentDescription = "リプライ",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                    "repost" -> Icon(
                        Icons.Default.Refresh,
                        contentDescription = "リポスト",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                    "like" -> Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = "リポスト",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                    "follow" -> Icon(
                        Icons.Default.Face,
                        contentDescription = "フォロー",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                    "unknown" -> Icon(
                        Icons.Default.Notifications,
                        contentDescription = "不明",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }

        Spacer (Modifier.spacePadding)

        Column {
            if (showNewMark) {
                Text(" ●", color = Color.Red)
            }
        }
    }
}

@Composable
fun DisplayContent(text: String?, authorName: String?, images: List<DisplayImage>?, date: String?) {
    val selectedImage = remember { mutableStateOf<String?>(null) }

    if (!authorName.isNullOrBlank()) {
        Text(
            text = authorName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (!text.isNullOrBlank()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
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
        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
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
    author: ActorDefsProfileView,
    onReply: ((RepoStrongRef, RepoStrongRef, FeedPost, ActorDefsProfileView) -> Unit)?,
    onLike: ((RepoStrongRef) -> Unit)?,
    onRepost: ((RepoStrongRef) -> Unit)?
) {
    if (!isMyPost) {
        val likeColor = if (isLiked) Color.Red else MaterialTheme.colorScheme.onBackground
        val repostColor = if (isReposted) Color.Green else MaterialTheme.colorScheme.onBackground

        Row {
            if (feed?.asFeedPost != null) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "リプライ",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable {
                            onReply?.invoke(subjectRef, rootRef, feed, author)
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
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
