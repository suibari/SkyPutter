package com.suibari.skyputter.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import work.socialhub.kbsky.model.app.bsky.embed.EmbedVideoView
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
    onQuote: ((RepoStrongRef) -> Unit)?,
) {
    val record = feed.raw.post.record?.asFeedPost!!

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

    val video: EmbedVideoView? = feed.raw.post.embed?.asVideo

    // 2段階boxとし、1つ目のboxで背景を設定、2つ目のboxでコンテンツ描画
    // これによりスワイプ削除をうまく機能させる
    Box (Modifier
        .background(MaterialTheme.colorScheme.background)
        .fillMaxSize()
    ) {
        Box (Modifier
            .itemPadding
        ) {
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
                        video = video,
                        date = record.createdAt,
                    )

                    DisplayActions(
                        isMyPost = false, // trueでアクションボタンが消える。暫定false(常に表示)
                        isLiked = isLiked,
                        isReposted = isReposted,
                        subjectRef = subjectRef,
                        rootRef = rootRef,
                        feed = record,
                        author = ActorDefsProfileView(), // 現状自分のポストのみ表示なので暫定措置
                        onReply = onReply,
                        onLike = onLike,
                        onRepost = onRepost,
                        onQuote = onQuote,
                    )

                    // 返信
                    DisplayParentPost(
                        authorName = feed.raw.reply?.parent?.author?.displayName,
                        record = feed.raw.reply?.parent?.record?.asFeedPost,
                    )
                    // 引用
                    DisplayParentPost(
                        authorName = feed.raw.post.embed?.asRecord?.record?.asRecord?.author?.displayName,
                        record = feed.raw.post.embed?.asRecord?.record?.asRecord?.value?.asFeedPost,
                    )
                }
            }
        }
    }
}
