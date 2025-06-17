package com.example.skyposter.ui.post

import androidx.compose.runtime.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun UserPostListScreen(
    viewModel: UserPostViewModel,
    myDid: String,
) {
    val feeds = viewModel.items
    val refreshing = remember { mutableStateOf(false) }

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
        )
    }
}
