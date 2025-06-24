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
import com.suibari.skyputter.ui.type.DisplayFeed
import kotlin.math.roundToInt

@Composable
fun UserPostListScreen(
    viewModel: UserPostViewModel,
    myDid: String,
) {
    val feeds = viewModel.items.toMutableStateList()
    var postToDelete by remember { mutableStateOf<DisplayFeed?>(null) }

    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        LazyColumn {
            items(feeds, key = { it.uri!! }) { feed ->
                var rawOffsetX by remember { mutableStateOf(0f) }

                // アニメーションで滑らかに戻す
                val animatedOffsetX by animateFloatAsState(
                    targetValue = rawOffsetX,
                    label = "swipeOffset"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()  // はみ出し防止
                ) {
                    var itemHeightPx by remember { mutableStateOf(0) }

                    // 🔴 背景
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
                            modifier = Modifier
                                .size(32.dp)
                                .padding(end = 16.dp)
                        )
                    }

                    // 📄 前面
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
        }
    }

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
                                feeds.remove(feed)
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

@Composable
fun Int.toDp(): Dp {
    return with(LocalDensity.current) { this@toDp.toDp() }
}
