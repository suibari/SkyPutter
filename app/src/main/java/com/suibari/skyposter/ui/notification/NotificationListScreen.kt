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

    val onReply = { parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost ->
        mainViewModel.setReplyContext(
            parentRef = parentRef,
            rootRef = rootRef,
            parentPost = parentPost
        )
        onNavigateToMain()
    }
    val onLike: (RepoStrongRef) -> Unit = { record ->
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.likePost(record)
        }
    }
    val onRepost: (RepoStrongRef) -> Unit = { record ->
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.repostPost(record)
        }
    }

    SwipeRefresh(state = rememberSwipeRefreshState(refreshing.value), onRefresh = {
        refreshing.value = true
        // 強制更新ロジック
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.fetchNow()
            refreshing.value = false
        }
    }) {
        LazyColumn {
            itemsIndexed(notifications) { index, notif ->
                NotificationItem(notif, onReply, onLike, onRepost)

                // 最後のアイテムが表示されたときに追加読み込み
                if (index == notifications.lastIndex) {
                    LaunchedEffect(index) {
                        viewModel.loadMoreItems()
                    }
                }
            }
        }
    }
}
