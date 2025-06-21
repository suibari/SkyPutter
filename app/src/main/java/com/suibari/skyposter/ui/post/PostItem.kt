package com.suibari.skyposter.ui.post

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suibari.skyposter.data.model.DisplayActions
import com.suibari.skyposter.data.model.DisplayContent
import com.suibari.skyposter.data.model.DisplayHeader
import com.suibari.skyposter.data.model.DisplayImage
import com.suibari.skyposter.data.model.DisplayParentPost
import com.suibari.skyposter.ui.type.DisplayFeed
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

@Composable
fun PostItem(
    feed: DisplayFeed,
    myDid: String,
    isLiked: Boolean,
    isReposted: Boolean,
    onReply: ((parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost) -> Unit)?,
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

    Row (modifier = Modifier.padding(start = 8.dp)) {
        // ヘッダー
        DisplayHeader(
            avatarUrl = feed.raw.post.author?.avatar,
        )

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
                onReply = onReply,
                onLike = onLike,
                onRepost = onRepost
            )

            // 親ポスト欄
            val quoted = feed.raw.post.embed?.asRecord?.record?.asRecord?.value?.asFeedPost
            DisplayParentPost(quoted?.text)
        }
    }
}
