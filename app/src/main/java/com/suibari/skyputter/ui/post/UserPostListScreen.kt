package com.suibari.skyputter.ui.post

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.suibari.skyputter.data.model.PaginatedListScreen
import com.suibari.skyputter.ui.type.DisplayFeed
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPostListScreen(
    viewModel: UserPostViewModel,
    myDid: String,
) {
    var postToDelete by remember { mutableStateOf<DisplayFeed?>(null) }

    PaginatedListScreen(
        title = "Your Posts",
        items = viewModel.items,
        viewModel = viewModel,
        isRefreshing = viewModel.isRefreshing,
        isLoadingMore = viewModel.isLoadingMore,
        onRefresh = { viewModel.loadInitialItems() },
        onLoadMore = { viewModel.loadMoreItems() },
        itemKey = { it.uri!! },
        itemContent = { feed ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it == EndToStart) {
                        postToDelete = feed
                    }
                    false // 自動で消えないようにする
                }
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
                                    .background(lerp(Color.LightGray, Color.Red, dismissState.progress))
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
                PostItem(
                    feed = feed,
                    myDid = myDid,
                    isLiked = false,
                    isReposted = false,
                    onReply = null,
                    onLike = null,
                    onRepost = null,
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
