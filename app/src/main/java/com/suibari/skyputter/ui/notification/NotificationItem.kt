package com.suibari.skyputter.ui.notification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suibari.skyputter.data.model.DisplayActions
import com.suibari.skyputter.data.model.DisplayContent
import com.suibari.skyputter.data.model.DisplayExternal
import com.suibari.skyputter.data.model.DisplayHeader
import com.suibari.skyputter.data.model.DisplayImage
import com.suibari.skyputter.data.model.DisplayParentPost
import com.suibari.skyputter.ui.theme.itemPadding
import com.suibari.skyputter.ui.theme.spacePadding
import com.suibari.skyputter.ui.type.DisplayNotification
import com.suibari.skyputter.util.BskyUtil
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileView
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

@Composable
fun NotificationItem(
    notification: DisplayNotification,
    isLiked: Boolean,
    isReposted: Boolean,
    onReply: (parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost, parentAuthor: ActorDefsProfileView) -> Unit,
    onLike: (ref: RepoStrongRef) -> Unit,
    onRepost: (ref: RepoStrongRef) -> Unit,
    onQuote: (ref: RepoStrongRef) -> Unit,
) {
    val record = notification.raw.record
    val did = notification.raw.author.did

    val subjectRef = RepoStrongRef(notification.raw.uri, notification.raw.cid)
    val rootRef = notification.rootPostRecord ?: subjectRef

    val images: List<DisplayImage>? = notification.raw.record.asFeedPost?.embed?.asImages?.images?.map {
        val cid = it.image?.ref?.link!!
        val thumb = BskyUtil.buildCdnImageUrl(did, cid, "feed_thumbnail")
        val fullsize = BskyUtil.buildCdnImageUrl(did, cid, "feed_fullsize")
        DisplayImage(
            urlThumb = thumb,
            urlFullsize = fullsize,
            alt = it.alt,
        )
    }

    val external = record.asFeedPost?.embed?.asExternal?.external

    Box (Modifier.itemPadding) {
        Row {
            // 自分ポスト欄
            DisplayHeader(
                avatarUrl = notification.raw.author.avatar,
                showNewMark = notification.isNew,
                reason = notification.raw.reason
            )

            Spacer (Modifier.spacePadding)

            Column {
                DisplayContent(
                    text = record.asFeedPost?.text,
                    authorName = notification.raw.author.displayName,
                    images = images,
                    video = null, // 暫定非対応: 通知で動画は表示しない
                    date = notification.raw.indexedAt,
                )

                DisplayActions(
                    isMyPost = false,
                    isLiked = isLiked,
                    isReposted = isReposted,
                    subjectRef = subjectRef,
                    rootRef = rootRef,
                    feed = notification.raw.record.asFeedPost,
                    author = notification.raw.author,
                    onReply = onReply,
                    onLike = onLike,
                    onRepost = onRepost,
                    onQuote = onQuote,
                )

                // リンクカード
                if (external != null) {
                    DisplayExternal(
                        authorDid = did,
                        title = external.title,
                        uri = external.uri,
                        cid = external.thumb?.ref?.link,
                    )
                }

                // 返信
                DisplayParentPost(
                    authorName = null, // 元のポスト主は絶対に自分のため、表示が無駄なのでnull入れておく
                    record = notification.parentPost,
                )
            }
        }
    }
}
