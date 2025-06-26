package com.suibari.skyputter.data.model

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suibari.skyputter.ui.main.AttachedEmbed
import com.suibari.skyputter.ui.main.MainViewModel
import com.suibari.skyputter.ui.type.HasUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
    ) -> Unit)
{
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val onReply = { parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost, parentAuthor: ActorDefsProfileView ->
        mainViewModel.setReplyContext(parentRef, rootRef, parentPost, parentAuthor)
        onBack()
    }

    val onQuote = { ref: RepoStrongRef ->
        coroutineScope.launch {
            val post = viewModel.getRecord(ref)
            mainViewModel.addEmbed(
                AttachedEmbed(
                    type = BlueskyTypes.EmbedRecord,
                    ref = ref,
                    post = post,
                )
            )
            onBack()
        }
        Unit
    }

    // フィード表示時、再読み込みする: 通知画面を直接呼び出したとき対策
    LaunchedEffect(Unit) {
        viewModel.loadInitialItems()
    }

    // スクロール末尾で読み込み
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val totalItems = layoutInfo.totalItemsCount
            lastVisibleIndex != null &&
                    totalItems > 0 &&
                    lastVisibleIndex >= totalItems - 5
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && !isLoadingMore && !isRefreshing) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        onLoadMore()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
            modifier = Modifier
                .padding(paddingValues)
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
            }
        }
    }
}