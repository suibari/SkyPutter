package com.suibari.skyputter.ui.post

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
import com.suibari.skyputter.data.model.DisplayHeader
import com.suibari.skyputter.data.model.DisplayImage
import com.suibari.skyputter.data.model.DisplayParentPost
import com.suibari.skyputter.ui.theme.itemPadding
import com.suibari.skyputter.ui.theme.spacePadding
import com.suibari.skyputter.ui.type.DisplayFeed
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileView
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

@Composable
fun PostItem(
    feed: DisplayFeed,
    myDid: String,
    isLiked: Boolean,
    isReposted: Boolean,
    onReply: ((parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost, parentAuthor: ActorDefsProfileView) -> Unit)?,
    onLike: ((RepoStrongRef) -> Unit)?,
    onRepost: ((RepoStrongRef) -> Unit)?,
) {
    val record = feed.raw.post.record?.asFeedPost!!
    val isMyPost = feed.raw.post.author?.did == myDid

    val subjectRef = RepoStrongRef(feed.raw.post.uri!!, feed.raw.post.cid!!)
    val rootRef = feed.raw.reply?.root?.let {
        RepoStrongRef(it.uri!!, it.cid!!)
    } ?: subjectRef

    val images: List<DisplayImage>? = feed.raw.post.embed?.asImages?.images?.map {
        DisplayImage(
            urlThumb = it.thumb!!,
            urlFullsize = it.fullsize!!,
            alt = it.alt,
        )
    }

    Box (Modifier.itemPadding) {
        Row {
            // ヘッダー
            DisplayHeader(
                avatarUrl = feed.raw.post.author?.avatar,
            )

            Spacer (Modifier.spacePadding)

            // メインコンテンツ
            Column {
                DisplayContent (
                    text = record.text,
                    authorName = feed.raw.post.author?.displayName,
                    images = images,
                    date = record.createdAt,
                )

                DisplayActions(
                    isMyPost = isMyPost,
                    isLiked = isLiked,
                    isReposted = isReposted,
                    subjectRef = subjectRef,
                    rootRef = rootRef,
                    feed = record,
                    author = ActorDefsProfileView(),
                    onReply = onReply,
                    onLike = onLike,
                    onRepost = onRepost
                )

                // 親ポスト欄
                val replied = feed.raw.reply?.parent?.record?.asFeedPost
                DisplayParentPost(replied?.text)
                val quoted = feed.raw.post.embed?.asRecord?.record?.asRecord?.value?.asFeedPost
                DisplayParentPost(quoted?.text)
            }
        }
    }
}
