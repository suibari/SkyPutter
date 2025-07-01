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
import com.suibari.skyputter.data.model.DisplayExternal
import com.suibari.skyputter.data.model.DisplayHeader
import com.suibari.skyputter.data.model.DisplayImage
import com.suibari.skyputter.data.model.DisplayParentPost
import com.suibari.skyputter.ui.theme.itemPadding
import com.suibari.skyputter.ui.theme.spacePadding
import com.suibari.skyputter.ui.type.DisplayFeed
import com.suibari.skyputter.util.BskyUtil
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
    val (repo, collection, rkey) = BskyUtil.parseAtUri(feed.uri.toString())!!

    val subjectRef = RepoStrongRef(feed.raw.post.uri!!, feed.raw.post.cid!!)
    val rootRef = feed.raw.reply?.root?.let {
        RepoStrongRef(it.uri!!, it.cid!!)
    } ?: subjectRef

    val embed = feed.raw.post.embed
    val recordWithMedia = embed?.asRecordWithMedia

    val quotePost = recordWithMedia?.record?.record?.asRecord?.value?.asFeedPost
        ?: embed?.asRecord?.record?.asRecord?.value?.asFeedPost

    val quoteAuthor = recordWithMedia?.record?.record?.asRecord?.author
        ?: embed?.asRecord?.record?.asRecord?.author

    val images: List<DisplayImage>? = (
            recordWithMedia?.media?.asImages?.images
                ?: embed?.asImages?.images
            )?.map {
            DisplayImage(
                urlThumb = it.thumb!!,
                urlFullsize = it.fullsize!!,
                alt = it.alt,
            )
        }

    val video = recordWithMedia?.media?.asVideo
        ?: embed?.asVideo

    val external = recordWithMedia?.media?.asExternal?.external
        ?: embed?.asExternal?.external

    Box(
        Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
    ) {
        Box(Modifier.itemPadding) {
            Row {
                // ヘッダー
                DisplayHeader(
                    avatarUrl = feed.raw.post.author?.avatar,
                )

                Spacer(Modifier.spacePadding)

                // メインコンテンツ
                Column {
                    DisplayContent(
                        text = record.text,
                        authorName = feed.raw.post.author?.displayName,
                        images = images,
                        video = video,
                        date = record.createdAt,
                    )

                    DisplayActions(
                        isMyPost = false,
                        isLiked = isLiked,
                        isReposted = isReposted,
                        subjectRef = subjectRef,
                        rootRef = rootRef,
                        feed = record,
                        author = ActorDefsProfileView(), // 自分のポストのみ表示ならこれで暫定OK
                        onReply = onReply,
                        onLike = onLike,
                        onRepost = onRepost,
                        onQuote = onQuote,
                    )

                    // 外部リンクカード
                    if (external != null) {
                        DisplayExternal(
                            authorDid = repo,
                            title = external.title,
                            thumb = external.thumb,
                            uri = external.uri,
                        )
                    }

                    // 返信（親ポスト）
                    DisplayParentPost(
                        authorName = feed.raw.reply?.parent?.author?.displayName,
                        record = feed.raw.reply?.parent?.record?.asFeedPost,
                    )

                    // 引用（record or recordWithMediaのrecord）
                    DisplayParentPost(
                        authorName = quoteAuthor?.displayName,
                        record = quotePost,
                    )
                }
            }
        }
    }
}

