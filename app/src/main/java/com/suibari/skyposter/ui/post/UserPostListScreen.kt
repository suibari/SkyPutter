package com.suibari.skyposter.ui.post

import androidx.compose.runtime.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.suibari.skyposter.data.model.PaginatedListScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun UserPostListScreen(
    viewModel: UserPostViewModel,
    myDid: String,
) {
    val feeds = viewModel.items

    PaginatedListScreen(
        items = feeds,
        isRefreshing = false,
        isLoadingMore = viewModel.isLoadingMore,
        onRefresh = {
            viewModel.loadInitialItems(25)
        },
        onLoadMore = {
            viewModel.loadMoreItems()
        },
        itemKey = { it.uri!! },
        itemContent = { feed ->
            PostItem(
                feed = feed,
                myDid = myDid,
                isLiked = false,
                isReposted = false,
                onReply = null,
                onLike = null,
                onRepost = null,
            )
        }
    )
}
