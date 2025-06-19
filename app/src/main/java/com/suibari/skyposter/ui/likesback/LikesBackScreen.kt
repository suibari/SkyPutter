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

data class RefWithLikedOrReposted (
    val ref: RepoStrongRef,
    val isExec: Boolean,
)

@Composable
fun LikesBackScreen(
    viewModel: LikesBackViewModel,
    mainViewModel: MainViewModel,
    myDid: String,
    onNavigateToMain: () -> Unit,
) {
    val feeds = viewModel.items
    val refreshing = remember { mutableStateOf(false) }

    val onReply = { parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost ->
        mainViewModel.setReplyContext(
            parentRef = parentRef,
            rootRef = rootRef,
            parentPost = parentPost
        )
        onNavigateToMain()
    }
    val onLike: (RefWithLikedOrReposted) -> Unit = {
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.toggleLike(
                ref = it.ref,
                isLiked = it.isExec,
            )
        }
    }
    val onRepost: (RefWithLikedOrReposted) -> Unit = {
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.toggleRepost(
                ref = it.ref,
                isReposted = it.isExec,
            )
        }
    }

    SwipeRefresh(state = rememberSwipeRefreshState(refreshing.value), onRefresh = {
        refreshing.value = true
        // 強制更新ロジック
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.loadInitialItems(10)
            refreshing.value = false
        }
    }) {
        PostListScreen(
            feeds = feeds,
            myDid = myDid,
            onLoadMore = { viewModel.loadMoreItems() },
            onReply,
            onLike,
            onRepost,
        )
    }
}
