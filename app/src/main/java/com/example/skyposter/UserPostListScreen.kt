package com.example.skyposter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.launch
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetAuthorFeedRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsPostView
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost

@Composable
fun UserPostListScreen(sessionManager: SessionManager) {
    val coroutineScope = rememberCoroutineScope()
    var posts by remember { mutableStateOf<List<FeedDefsPostView>>(emptyList()) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val auth = sessionManager.getAuth() ?: return@launch
            val did = sessionManager.getSession().did ?: return@launch

            val response = BlueskyFactory
                .instance(BSKY_SOCIAL.uri)
                .feed()
                .getAuthorFeed(
                    FeedGetAuthorFeedRequest(auth).also {
                        it.actor = did
                        it.limit = 100
                    }
                )

            posts = response.data.feed.map { item -> item.post }
        }
    }

    LazyColumn {
        items(posts) { post ->
            PostItem(post)
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