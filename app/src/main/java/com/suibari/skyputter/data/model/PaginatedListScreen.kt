package com.suibari.skyputter.data.model

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suibari.skyputter.ui.main.AttachedEmbed
import com.suibari.skyputter.ui.main.MainViewModel
import com.suibari.skyputter.ui.type.HasUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import work.socialhub.kbsky.BlueskyTypes
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileView
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : HasUri> PaginatedListScreen(
    title: String,
    items: List<T>,
    viewModel: PaginatedListViewModel<T>,
    mainViewModel: MainViewModel,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    onBack: () -> Unit,
    onRefresh: suspend () -> Unit,
    onLoadMore: suspend () -> Unit,
    itemKey: (T) -> Any,
    itemContent: @Composable (
        item: T,
        onReply: (RepoStrongRef, RepoStrongRef, FeedPost, ActorDefsProfileView) -> Unit,
        onQuote: (RepoStrongRef) -> Unit
    ) -> Unit,
    topBarActions: @Composable (() -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val onReply = { parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost, parentAuthor: ActorDefsProfileView ->
        mainViewModel.setReplyContext(parentRef, rootRef, parentPost, parentAuthor)
        onBack()
    }

    val onQuote = { ref: RepoStrongRef ->
        coroutineScope.launch(Dispatchers.IO) {
            val post = viewModel.getRecord(ref)
            withContext(Dispatchers.Main) {
                mainViewModel.addEmbed(
                    AttachedEmbed(
                        type = BlueskyTypes.EmbedRecord,
                        ref = ref,
                        post = post,
                    )
                )
                onBack()
            }
        }
        Unit
    }

    // フィード表示時、再読み込みする: 通知画面を直接呼び出したとき対策
    LaunchedEffect(Unit) {
        viewModel.loadInitialItems()
    }

    // 高速スクロール対応の末尾検出
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = layoutInfo.totalItemsCount

            lastVisibleIndex >= totalItems - 1 && totalItems > 0 &&
                    !isLoadingMore && !isRefreshing
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    onLoadMore()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // URI指定スクロール
    LaunchedEffect(viewModel.targetUri, viewModel.isRefreshing) {
        val uri = viewModel.targetUri
        if (uri != null) {
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
                        this.cancel()
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    topBarActions?.invoke()
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch(Dispatchers.IO) {
                    onRefresh()
                }
            },
            modifier = Modifier.padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(8.dp)
            ) {
                items(items = items, key = itemKey) { item ->
                    itemContent(item, onReply, onQuote)
                }

                // LoadMore中のプログレスインジケーター
                if (isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "読み込み中...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}