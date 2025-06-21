package com.suibari.skyposter.data.model

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

@Composable
fun <T> PaginatedListScreen(
    items: List<T>,
    isRefreshing: Boolean,
    onRefresh: suspend () -> Unit,
    onLoadMore: suspend () -> Unit,
    itemKey: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
) {
    val refreshing = remember { mutableStateOf(isRefreshing) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // スクロール末尾で追加読み込み
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex == items.lastIndex) {
                    coroutineScope.launch {
                        onLoadMore()
                    }
                }
            }
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(refreshing.value),
        onRefresh = {
            coroutineScope.launch {
                refreshing.value = true
                onRefresh()
                refreshing.value = false
            }
        }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(8.dp)
        ) {
            items(items = items, key = itemKey) { item ->
                itemContent(item)
            }
        }
    }
}
