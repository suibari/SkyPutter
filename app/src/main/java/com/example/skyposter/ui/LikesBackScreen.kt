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
import com.example.skyposter.LikesBackViewModel
import com.example.skyposter.UserPostViewModel
import kotlinx.coroutines.launch
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsPostView
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost

@Composable
fun LikesBackScreen(
    viewModel: LikesBackViewModel
) {
    val posts = viewModel.items

    PostListScreen(
        posts = posts,
        onLoadMore = { viewModel.loadMoreItems() }
    )
}
