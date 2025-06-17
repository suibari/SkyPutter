package com.example.skyposter.ui

import MainViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.skyposter.LikesBackViewModel
import com.example.skyposter.UserPostViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewBasic
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsPostView
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
