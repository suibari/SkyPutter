package com.example.skyposter.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.skyposter.UserPostViewModel
import kotlinx.coroutines.launch
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsPostView
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost

@Composable
fun UserPostListScreen(
    viewModel: UserPostViewModel
) {
    val posts = viewModel.items

    LazyColumn {
        itemsIndexed(posts) { index, post ->
            PostItem(post)

            // 最後のアイテムが表示されたときに追加読み込み
            if (index == posts.lastIndex) {
                LaunchedEffect(index) {
                    viewModel.loadMoreItems()
                }
            }
        }
    }
}

@Composable
fun PostItem(post: FeedDefsPostView) {
    val record = post.record as FeedPost

    Row(modifier = Modifier.padding(8.dp)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(post.author?.avatar)
                .crossfade(true)
                .build(),
            contentDescription = "avatar",
            modifier = Modifier
                .size(48.dp)
        )

        Column(modifier = Modifier.padding(start = 16.dp)) {
            Row {
                Text(
                    text = record.text ?: "",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Text(
                text = post.indexedAt ?: "unknown",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}