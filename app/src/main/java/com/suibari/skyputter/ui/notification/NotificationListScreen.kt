package com.suibari.skyputter.ui.notification

import com.suibari.skyputter.ui.main.MainViewModel
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.suibari.skyputter.data.model.PaginatedListScreen
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

    val onReply = { parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost ->
        mainViewModel.setReplyContext(parentRef, rootRef, parentPost)
        onNavigateToMain()
    }

    PaginatedListScreen(
        items = notifications,
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
