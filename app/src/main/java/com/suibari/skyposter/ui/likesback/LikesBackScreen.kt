package com.suibari.skyposter.ui.likesback

import com.suibari.skyposter.ui.main.MainViewModel
import androidx.compose.runtime.*
import com.suibari.skyposter.data.model.PaginatedListScreen
import com.suibari.skyposter.ui.post.PostItem
import kotlinx.coroutines.launch
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

@Composable
fun LikesBackScreen(
    viewModel: LikesBackViewModel,
    mainViewModel: MainViewModel,
    myDid: String,
    onNavigateToMain: () -> Unit,
) {
    val feeds = viewModel.items
    val coroutineScope = rememberCoroutineScope()

    val onReply = { parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost ->
        mainViewModel.setReplyContext(parentRef, rootRef, parentPost)
        onNavigateToMain()
    }

    PaginatedListScreen(
        items = feeds,
        isRefreshing = false,
        onRefresh = {
            viewModel.loadInitialItems(25)
        },
        onLoadMore = {
            viewModel.loadMoreItems()
        },
        itemKey = { it.uri!! },
        itemContent = { feed ->
            val viewer = viewModel.viewerStatus[feed.uri]
            val isLiked = viewer?.like != null
            val isReposted = viewer?.repost != null

            PostItem(
                feed = feed,
                myDid = myDid,
                isLiked = isLiked,
                isReposted = isReposted,
                onReply = onReply,
                onLike = { ref -> coroutineScope.launch { viewModel.toggleLike(ref) } },
                onRepost = { ref -> coroutineScope.launch { viewModel.toggleRepost(ref) } },
            )
        }
    )
}
