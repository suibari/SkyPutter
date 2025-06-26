package com.suibari.skyputter.data.model

import android.content.Context
import android.view.ViewGroup
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
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suibari.skyputter.ui.theme.spacePadding
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileView
import work.socialhub.kbsky.model.app.bsky.embed.EmbedVideoView
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef
import java.time.Instant
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

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
fun DisplayContent(text: String?, authorName: String?, images: List<DisplayImage>?, video: EmbedVideoView?, date: String?) {
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

    if (video != null) {
        DisplayVideo(video.playlist)
    }

    if (!date.isNullOrBlank()) {
        Text(
            text = formatRelativeTime(date),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

fun formatRelativeTime(dateString: String): String {
    return try {
        val dateTime = Instant.parse(dateString)
        val now = Instant.now()
        val duration = Duration.between(dateTime, now)

        when {
            duration.seconds < 60 -> "今"
            duration.toMinutes() < 60 -> "${duration.toMinutes()}分前"
            duration.toHours() < 24 -> "${duration.toHours()}時間前"
            else -> "${duration.toDays()}日前"
        }
    } catch (e: Exception) {
        // パースできなかった場合は元の文字列を返す
        dateString
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
fun DisplayVideo(
    videoUrl: String,
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        // 明示的に HLS 対応の MediaSourceFactory を指定
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                setMediaItem(
                    MediaItem.Builder()
                        .setUri(videoUrl)
                        .setMimeType(MimeTypes.APPLICATION_M3U8)
                        .build()
                )
                prepare()
            }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier
            .size(200.dp)
    )
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
    onRepost: ((RepoStrongRef) -> Unit)?,
    onQuote: ((RepoStrongRef) -> Unit)?,
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
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable {
                            onRepost?.invoke(
                                subjectRef,
                            )
                        }
                )
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = "引用",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.clickable {
                        onQuote?.invoke(
                            subjectRef,
                        )
                    }
                )
            }
        }
    }
}


@Composable
fun DisplayParentPost(authorName: String?, record: FeedPost?) {
    if (record != null) {
        Column(
            modifier = Modifier
                .padding(top = 8.dp)
        ) {
            Text(
                text = authorName ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            Text(
                text = record.text ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
fun VideoPlayer(
    context: Context,
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = modifier
    )
}
