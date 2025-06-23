package com.suibari.skyputter.ui.post

import androidx.compose.runtime.*
import com.suibari.skyputter.data.model.PaginatedListScreen

@Composable
fun UserPostListScreen(
    viewModel: UserPostViewModel,
    myDid: String,
) {
    val feeds = viewModel.items

    PaginatedListScreen(
        items = feeds,
        viewModel = viewModel,
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
