package com.suibari.skyputter.ui.notification

import com.suibari.skyputter.ui.main.MainViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.rememberLazyListState
import com.suibari.skyputter.data.model.PaginatedListScreen
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileView
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

@Composable
fun NotificationListScreen(
    viewModel: NotificationViewModel,
    mainViewModel: MainViewModel,
    onNavigateToMain: () -> Unit
) {
    val notifications = viewModel.items
    val coroutineScope = rememberCoroutineScope()

    val onReply = { parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost, parentAuthor: ActorDefsProfileView ->
        mainViewModel.setReplyContext(parentRef, rootRef, parentPost, parentAuthor)
        onNavigateToMain()
    }

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
        items = notifications,
        viewModel = viewModel,
        isRefreshing = false,
        isLoadingMore = viewModel.isLoadingMore,
        onRefresh = {
            viewModel.fetchNow()
        },
        onLoadMore = {
            viewModel.loadMoreItems()
        },
        itemKey = { it.uri!! },
        itemContent = { notif ->
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
            )
        }
    )
}
