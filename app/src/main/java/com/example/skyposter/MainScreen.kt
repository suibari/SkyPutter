package com.example.skyposter

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedPostRequest
import work.socialhub.kbsky.domain.Service

@Composable
fun MainScreen() {
    Column {
        var postText by remember { mutableStateOf("") }

        TextField(
            value = postText,
            onValueChange = { postText = it },
            label = { Text("今何してる？") }
        )

        Button(onClick = {
            val auth = BskyClient.getAuth()
            BlueskyFactory
                .instance(Service.BSKY_SOCIAL.uri)
                .feed()
                .post(
                    FeedPostRequest(auth).also {
                        it.text = postText
                    }
                )
        }) {
            Text("ポストする")
        }
    }
}