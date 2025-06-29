package com.suibari.skyputter.ui.notification

import com.suibari.skyputter.ui.main.MainViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.rememberLazyListState
import com.suibari.skyputter.data.model.PaginatedListScreen
import com.suibari.skyputter.ui.main.AttachedEmbed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import work.socialhub.kbsky.BlueskyTypes
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileView
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

@Composable
fun NotificationListScreen(
    viewModel: NotificationViewModel,
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    val notifications = viewModel.items
    val coroutineScope = rememberCoroutineScope()

    val listState = rememberLazyListState()

    LaunchedEffect(viewModel.targetUri) {
        val uri = viewModel.targetUri
        if (uri != null) {
            val index = viewModel.items.indexOfFirst { it.raw.uri == uri }
            if (index != -1) {
                listState.scrollToItem(index)
            } else {
                // 見つからない場合は上に戻す（任意）
                listState.scrollToItem(0)
            }
            viewModel.targetUri = null
        }
    }

    PaginatedListScreen(
        title = "Notification",
        items = notifications,
        viewModel = viewModel,
        mainViewModel = mainViewModel,
        isRefreshing = viewModel.isRefreshing,
        isLoadingMore = viewModel.isLoadingMore,
        onBack = { onBack() },
        onRefresh = {
            viewModel.markAllAsReadAndReload()
        },
        onLoadMore = {
            viewModel.loadMoreItems()
        },
        itemKey = { it.uri!! },
        itemContent = { notif, onReply, onQuote ->
            val viewer = viewModel.viewerStatus[notif.uri]
            val isLiked = viewer?.like != null
            val isReposted = viewer?.repost != null

            NotificationItem(
                notification = notif,
                isLiked = isLiked,
                isReposted = isReposted,
                onReply = onReply,
                onLike = { ref -> coroutineScope.launch { viewModel.toggleLike(ref) } },
                onRepost = { ref -> coroutineScope.launch { viewModel.toggleRepost(ref) } },
                onQuote = { onQuote(it) },
            )
        }
    )
}
