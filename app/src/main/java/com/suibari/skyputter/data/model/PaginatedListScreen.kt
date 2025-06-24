package com.suibari.skyputter.data.model

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.suibari.skyputter.ui.type.HasUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@Composable
fun <T: HasUri> PaginatedListScreen(
    items: List<T>,
    viewModel: PaginatedListViewModel<T>,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    onRefresh: suspend () -> Unit,
    onLoadMore: suspend () -> Unit,
    itemKey: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // フィード表示時、再読み込みする
    LaunchedEffect(Unit) {
        viewModel.loadInitialItems()
    }

    // スクロール末尾で追加読み込み（改善版）
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            val lastVisibleItemIndex = visibleItemsInfo.lastOrNull()?.index
            val totalItemsCount = layoutInfo.totalItemsCount

            // 末尾から5アイテム以内に到達したときにトリガー
            lastVisibleItemIndex != null &&
                    totalItemsCount > 0 &&
                    lastVisibleItemIndex >= totalItemsCount - 5
        }
            .collect { shouldLoadMore ->
                if (shouldLoadMore && !isLoadingMore && !isRefreshing) {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            onLoadMore()
                        } catch (e: Exception) {
                            // エラーハンドリング
                            e.printStackTrace()
                        }
                    }
                }
            }
    }

    // URI指定で強制スクロール
    LaunchedEffect(viewModel.targetUri, viewModel.isRefreshing) {
        val uri = viewModel.targetUri
        if (uri != null) {
            // isRefreshingがfalse（ロード完了）になるのを待つ
            snapshotFlow { viewModel.isRefreshing }
                .collect { refreshing ->
                    if (!refreshing) {
                        val index = viewModel.items.indexOfFirst { it.uri == uri }
                        if (index != -1) {
                            listState.scrollToItem(index)
                        } else {
                            listState.scrollToItem(0)
                        }
                        viewModel.targetUri = null
                        this.cancel() // collect終了
                    }
                }
        }
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = {
            if (!isRefreshing && !isLoadingMore) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        onRefresh()
                    } catch (e: Exception) {
                        // エラーハンドリング
                        e.printStackTrace()
                    }
                }
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