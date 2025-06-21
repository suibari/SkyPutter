package com.suibari.skyposter.ui.notification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suibari.skyposter.ui.post.DisplayActions
import com.suibari.skyposter.ui.post.DisplayContent
import com.suibari.skyposter.ui.post.DisplayHeader
import com.suibari.skyposter.ui.post.DisplayImage
import com.suibari.skyposter.ui.post.DisplayParentPost
import com.suibari.skyposter.ui.type.DisplayNotification
import com.suibari.skyposter.util.BskyUtil
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

@Composable
fun NotificationItem(
    notification: DisplayNotification,
    isLiked: Boolean,
    isReposted: Boolean,
    onReply: (parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost) -> Unit,
    onLike: (ref: RepoStrongRef) -> Unit,
    onRepost: (ref: RepoStrongRef) -> Unit,
) {
    val record = notification.raw.record

    val subjectRef = RepoStrongRef(notification.raw.uri, notification.raw.cid)
    val rootRef = notification.rootPostRecord ?: subjectRef

    val images: List<DisplayImage>? = notification.raw.record.asFeedPost?.embed?.asImages?.images?.map {
        val did = notification.raw.author.did
        val cid = it.image?.ref?.link!!
        val thumb = BskyUtil.buildCdnImageUrl(did, cid, "feed_thumbnail")
        val fullsize = BskyUtil.buildCdnImageUrl(did, cid, "feed_fullsize")
        DisplayImage(
            urlThumb = thumb,
            urlFullsize = fullsize,
            alt = it.alt,
        )
    }

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
                images = images,
                date = notification.raw.indexedAt
            )

            DisplayActions(
                isMyPost = false,
                isLiked = isLiked,
                isReposted = isReposted,
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
