package com.example.skyposter.ui.post

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.skyposter.data.repository.DisplayFeed
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

@Composable
fun PostListScreen(
    feeds: List<DisplayFeed>,
    myDid: String,
    onLoadMore: () -> Unit,
    onReply: ((parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost) -> Unit)? = null,
    onLike: ((parentRecord: RepoStrongRef) -> Unit)? = null,
    onRepost: ((parentRecord: RepoStrongRef) -> Unit)? = null
) {
    LazyColumn {
        itemsIndexed(feeds) { index, feed ->
            PostItem(
                feed = feed,
                myDid = myDid,
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
