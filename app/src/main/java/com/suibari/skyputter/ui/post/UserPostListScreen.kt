package com.suibari.skyputter.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.*
import com.suibari.skyputter.data.model.PaginatedListScreen
import com.suibari.skyputter.ui.main.AttachedEmbed
import com.suibari.skyputter.ui.main.MainViewModel
import com.suibari.skyputter.ui.type.DisplayFeed
import kotlinx.coroutines.launch
import work.socialhub.kbsky.BlueskyTypes
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileView
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPostListScreen(
    viewModel: UserPostViewModel,
    mainViewModel: MainViewModel,
    myDid: String,
    onBack: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var postToDelete by remember { mutableStateOf<DisplayFeed?>(null) }

    PaginatedListScreen(
        title = "Your Posts",
        items = viewModel.items,
        viewModel = viewModel,
        mainViewModel = mainViewModel,
        isRefreshing = viewModel.isRefreshing,
        isLoadingMore = viewModel.isLoadingMore,
        onBack = { onBack() },
        onRefresh = { viewModel.loadInitialItems() },
        onLoadMore = { viewModel.loadMoreItems() },
        itemKey = { it.uri!! },
        itemContent = { feed, onReply, onQuote ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it == EndToStart) {
                        postToDelete = feed
                    }
                    false // 自動で消えないようにする
                },
                positionalThreshold = { it * 0.5f }, // 感度を減らす
            )

            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
                backgroundContent = {
                    when (dismissState.dismissDirection) {
                        EndToStart -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        lerp(
                                            Color.LightGray,
                                            Color.Red,
                                            dismissState.progress
                                        )
                                    )
                                    .wrapContentSize(Alignment.CenterEnd)
                                    .padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White
                                )
                            }
                        }
                        else -> {}
                    }
                },
                modifier = Modifier.padding(vertical = 4.dp) // 少しスペース
            ) {
                val viewer = viewModel.viewerStatus[feed.uri]
                val isLiked = viewer?.like != null
                val isReposted = viewer?.repost != null

                PostItem(
                    feed = feed,
                    myDid = myDid,
                    isLiked = isLiked,
                    isReposted = isReposted,
                    onReply = onReply,
                    onLike = { ref -> coroutineScope.launch { viewModel.toggleLike(ref) } },
                    onRepost = { ref -> coroutineScope.launch { viewModel.toggleRepost(ref) } },
                    onQuote = { onQuote(it) },
                )
            }
        }
    )

    if (postToDelete != null) {
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text("投稿削除") },
            text = { Text("この投稿を削除してもよろしいですか？") },
            confirmButton = {
                TextButton(onClick = {
                    postToDelete?.let { feed ->
                        viewModel.deletePost(feed) { success ->
                            if (success) {
                                (viewModel.items as? MutableList<DisplayFeed>)?.remove(feed)
                            }
                        }
                    }
                    postToDelete = null
                }) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDelete = null }) {
                    Text("キャンセル")
                }
            }
        )
    }
}
