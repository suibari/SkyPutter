package com.example.skyposter.ui.likesback

import com.example.skyposter.ui.main.MainViewModel
import androidx.compose.runtime.*
import com.example.skyposter.ui.post.PostListScreen
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    val refreshing = remember { mutableStateOf(false) }

    val onReply = { parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost ->
        mainViewModel.setReplyContext(
            parentRef = parentRef,
            rootRef = rootRef,
            parentPost = parentPost
        )
        onNavigateToMain()
    }
    val onLike: (RepoStrongRef) -> Unit = { record ->
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.likePost(record)
        }
    }
    val onRepost: (RepoStrongRef) -> Unit = { record ->
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.repostPost(record)
        }
    }

    SwipeRefresh(state = rememberSwipeRefreshState(refreshing.value), onRefresh = {
        refreshing.value = true
        // 強制更新ロジック
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.fetchItems(10)
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
