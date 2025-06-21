package com.suibari.skyposter.ui.post

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.suibari.skyposter.data.model.PaginatedListViewModel
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsViewerState
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

@Composable
fun PostListScreen(
    feeds: List<DisplayFeed>,
    myDid: String,
    viewerStatus: Map<String, FeedDefsViewerState?>,
    onLoadMore: () -> Unit,
    onReply: ((parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost) -> Unit)? = null,
    onLike: ((RepoStrongRef) -> Unit)? = null,
    onRepost: ((RepoStrongRef) -> Unit)? = null
) {
    LazyColumn {
        itemsIndexed(feeds) { index, feed ->
            val viewer = viewerStatus[feed.uri]
            val isLiked = viewer?.like != null
            val isReposted = viewer?.repost != null

            PostItem(
                feed = feed,
                myDid = myDid,
                isLiked = isLiked,
                isReposted = isReposted,
                onReply = onReply,
                onLike = onLike,
                onRepost = onRepost
            )

            if (index == feeds.lastIndex) {
                LaunchedEffect(index) {
                    onLoadMore()
                }
            }
        }
    }
}
