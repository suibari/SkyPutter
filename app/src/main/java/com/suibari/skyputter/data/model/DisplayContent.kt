package com.suibari.skyputter.data.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Reply
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
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
import work.socialhub.kbsky.model.app.bsky.embed.EmbedDefsAspectRatio
import work.socialhub.kbsky.model.app.bsky.embed.EmbedVideoView
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef
import java.time.Instant
import java.time.Duration
import androidx.core.net.toUri
import com.suibari.skyputter.util.BskyUtil
import work.socialhub.kbsky.model.app.bsky.embed.EmbedExternal
import work.socialhub.kbsky.model.app.bsky.embed.EmbedExternalExternal
import work.socialhub.kbsky.model.app.bsky.embed.EmbedExternalView


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
                        Icons.Default.Reply,
                        contentDescription = "リプライ",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                    "repost" -> Icon(
                        Icons.Default.Repeat,
                        contentDescription = "リポスト",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                    "like" -> Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = "いいね",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                    "follow" -> Icon(
                        Icons.Default.Face,
                        contentDescription = "フォロー",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                    "quote" -> Icon(
                        Icons.Default.FormatQuote,
                        contentDescription = "引用",
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
fun DisplayContent(
    text: String?,
    authorName: String?,
    images: List<DisplayImage>?,
    video: EmbedVideoView?,
    date: String?
) {
    val context = LocalContext.current
    val selectedImage = remember { mutableStateOf<String?>(null) }

    if (!authorName.isNullOrBlank()) {
        Text(
            text = authorName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    if (!text.isNullOrBlank()) {
        LinkifiedText(text)
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
        DisplayVideo(video.playlist, video.aspectRatio)
    }

    if (!date.isNullOrBlank()) {
        Text(
            text = formatRelativeTime(date),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
fun LinkifiedText(text: String) {
    val context = LocalContext.current
    val annotatedText = remember { buildLinkAnnotatedText(text) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    BasicText(
        text = annotatedText,
        modifier = Modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.first().position
                        layoutResult?.let { layout ->
                            val offset = layout.getOffsetForPosition(position)
                            annotatedText.getLinkAnnotations(offset, offset).firstOrNull()?.let { annotation ->
                                val url = (annotation.item as? LinkAnnotation.Url)?.url
                                if (url != null) {
                                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                    context.startActivity(intent)
                                }
                            }
                        }
                    }
                }
            },
        onTextLayout = { layoutResult = it },
        style = androidx.compose.ui.text.TextStyle(
            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
            color = MaterialTheme.colorScheme.onBackground,
        )
    )
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
    images: List<DisplayImage>, // 4枚想定
    onImageClick: (String?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1行目: images[0], images[1]
        Row(modifier = Modifier.fillMaxWidth()) {
            images.getOrNull(0)?.let { image ->
                AsyncImage(
                    model = image.urlThumb,
                    contentDescription = image.alt,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(4.dp)
                        .clickable { onImageClick(image.urlFullsize) },
                    contentScale = ContentScale.Crop
                )
            }
            images.getOrNull(1)?.let { image ->
                AsyncImage(
                    model = image.urlThumb,
                    contentDescription = image.alt,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(4.dp)
                        .clickable { onImageClick(image.urlFullsize) },
                    contentScale = ContentScale.Crop
                )
            }
        }
        // 2行目: images[2], images[3]
        Row(modifier = Modifier.fillMaxWidth()) {
            images.getOrNull(2)?.let { image ->
                AsyncImage(
                    model = image.urlThumb,
                    contentDescription = image.alt,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(4.dp)
                        .clickable { onImageClick(image.urlFullsize) },
                    contentScale = ContentScale.Crop
                )
            }
            images.getOrNull(3)?.let { image ->
                AsyncImage(
                    model = image.urlThumb,
                    contentDescription = image.alt,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(4.dp)
                        .clickable { onImageClick(image.urlFullsize) },
                    contentScale = ContentScale.Crop
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
    aspectRatio: EmbedDefsAspectRatio?,
) {
    val context = LocalContext.current
    val aspectRatioFloat = if (aspectRatio != null && aspectRatio.width != 0 && aspectRatio.height != 0) {
        aspectRatio.width.toFloat() / aspectRatio.height
    } else {
        1.0f
    }

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
            .aspectRatio(aspectRatioFloat)
    )
}

@Composable
fun DisplayExternal(
    authorDid: String,
    uri: String,
    title: String,
    thumb: String? = null,
    cid: String? = null,
) {
    val context = LocalContext.current

    // サムネイルURL取得: Notification側だとURLが入っていないので
    var thumbUrl: String = if (cid != null) {
        BskyUtil.buildCdnImageUrl(
            did = authorDid,
            cid = cid,
            variant = "feed_thumbnail",
        )
    } else thumb ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, uri.toUri())
                context.startActivity(intent)
            }
            .padding(8.dp)
    ) {
        if (thumb != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(bottom = 8.dp),
                contentScale = ContentScale.Crop
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = uri,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
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
    onRepost: ((RepoStrongRef) -> Unit)?,
    onQuote: ((RepoStrongRef) -> Unit)?,
) {
    if (!isMyPost) {
        val likeColor = if (isLiked) Color.Red else MaterialTheme.colorScheme.onBackground
        val repostColor = if (isReposted) Color.Green else MaterialTheme.colorScheme.onBackground

        Row {
            if (feed?.asFeedPost != null) {
                Icon(
                    Icons.Default.Reply,
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
                    Icons.Default.Repeat,
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
                    Icons.Default.FormatQuote,
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

fun buildLinkAnnotatedText(text: String): AnnotatedString {
    val regex = "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=.%]+)".toRegex()
    val builder = AnnotatedString.Builder()
    var currentIndex = 0

    for (match in regex.findAll(text)) {
        val url = match.value
        val start = match.range.first

        if (start > currentIndex) {
            builder.append(text.substring(currentIndex, start))
        }

        builder.pushLink(LinkAnnotation.Url(url))
        builder.withStyle(SpanStyle(color = androidx.compose.ui.graphics.Color.Blue, textDecoration = TextDecoration.Underline)) {
            builder.append(url)
        }
        builder.pop()

        currentIndex = match.range.last + 1
    }

    if (currentIndex < text.length) {
        builder.append(text.substring(currentIndex))
    }

    return builder.toAnnotatedString()
}
