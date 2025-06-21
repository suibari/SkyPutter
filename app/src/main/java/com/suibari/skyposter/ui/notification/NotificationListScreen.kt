package com.suibari.skyposter.ui.notification

import com.suibari.skyposter.ui.main.MainViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

@Composable
fun NotificationListScreen(
    viewModel: NotificationViewModel,
    mainViewModel: MainViewModel,
    onNavigateToMain: () -> Unit
) {
    val notifications = viewModel.items
    val refreshing = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val onReply = { parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost ->
        mainViewModel.setReplyContext(parentRef, rootRef, parentPost)
        onNavigateToMain()
    }

    val onLike: (RepoStrongRef) -> Unit = { ref ->
        coroutineScope.launch {
            viewModel.toggleLike(ref)
        }
    }

    val onRepost: (RepoStrongRef) -> Unit = { ref ->
        coroutineScope.launch {
            viewModel.toggleRepost(ref)
        }
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(refreshing.value),
        onRefresh = {
            coroutineScope.launch {
                refreshing.value = true
                viewModel.fetchNow()
                refreshing.value = false
            }
        }
    ) {
        LazyColumn {
            itemsIndexed(notifications) { index, notif ->
                val viewer = viewModel.viewerStatus[notif.uri]
                val isLiked = viewer?.like != null
                val isReposted = viewer?.repost != null

                NotificationItem(
                    notification = notif,
                    isLiked = isLiked,
                    isReposted = isReposted,
                    onReply = onReply,
                    onLike = onLike,
                    onRepost = onRepost
                )

                // 最後のアイテムが表示されたら追加読み込み
                if (index == notifications.lastIndex) {
                    LaunchedEffect(index) {
                        viewModel.loadMoreItems()
                    }
                }
            }
        }
    }
}
