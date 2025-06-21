package com.suibari.skyposter.ui.likesback

import com.suibari.skyposter.ui.main.MainViewModel
import androidx.compose.runtime.*
import com.suibari.skyposter.ui.post.PostListScreen
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import work.socialhub.kbsky.api.entity.share.RKeyRequest
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
    val refreshing = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val onReply = { parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost ->
        mainViewModel.setReplyContext(parentRef, rootRef, parentPost)
        onNavigateToMain()
    }

    val onLike: (RepoStrongRef) -> Unit = { ref ->
        coroutineScope.launch {
            viewModel.toggleLike(ref)
        }
    }

    val onRepost: (RepoStrongRef) -> Unit = { ref ->
        coroutineScope.launch {
            viewModel.toggleRepost(ref)
        }
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(refreshing.value),
        onRefresh = {
            coroutineScope.launch {
                refreshing.value = true
                viewModel.loadInitialItems(25)
                refreshing.value = false
            }
        }
    ) {
        PostListScreen(
            feeds = feeds,
            myDid = myDid,
            viewerStatus = viewModel.viewerStatus,
            onLoadMore = { viewModel.loadMoreItems() },
            onReply = onReply,
            onLike = onLike,
            onRepost = onRepost,
        )
    }
}
