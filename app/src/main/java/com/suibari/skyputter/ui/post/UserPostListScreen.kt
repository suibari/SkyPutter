package com.suibari.skyputter.ui.post

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
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
            var rawOffsetX by remember { mutableStateOf(0f) }
            val animatedOffsetX by animateFloatAsState(
                targetValue = rawOffsetX,
                label = "swipeOffset"
            )

            Box(modifier = Modifier.fillMaxWidth().clipToBounds()) {
                var itemHeightPx by remember { mutableStateOf(0) }

                // Background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let {
                            if (itemHeightPx > 0) {
                                it.height(with(LocalDensity.current) { itemHeightPx.toDp() })
                            } else {
                                it.height(80.dp)
                            }
                        }
                        .background(Color.Red),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp).padding(end = 16.dp)
                    )
                }

                // Foreground post item
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                        .onGloballyPositioned { coordinates ->
                            itemHeightPx = coordinates.size.height
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (rawOffsetX < -200f) {
                                        postToDelete = feed
                                    }
                                    rawOffsetX = 0f
                                }
                            ) { change, dragAmount ->
                                rawOffsetX = (rawOffsetX + dragAmount).coerceIn(-600f, 0f)
                                change.consume()
                            }
                        }
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
