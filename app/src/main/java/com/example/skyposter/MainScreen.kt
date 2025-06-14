package com.example.skyposter

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedPostRequest
import work.socialhub.kbsky.auth.BearerTokenAuthProvider
import work.socialhub.kbsky.domain.Service

@Composable
fun MainScreen() {
    Column {
        var postText by remember { mutableStateOf("") }
        val context = LocalContext.current
        val sessionManager = remember { SessionManager(context) }
        val coroutineScope = rememberCoroutineScope()

        TextField(
            value = postText,
            onValueChange = { postText = it },
            label = { Text("今何してる？") }
        )

        // ポストボタン
        Button(onClick = {
            coroutineScope.launch {
                val auth = sessionManager.getAuth() ?: return@launch

                BlueskyFactory
                    .instance(Service.BSKY_SOCIAL.uri)
                    .feed()
                    .post(
                        FeedPostRequest(auth).also {
                            it.text = postText
                        }
                    )
            }
        }) {
            Text("ポストする")
        }
    }
}