package com.example.skyposter

import NotificationListScreen
import NotificationRepository
import NotificationViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedPostRequest
import work.socialhub.kbsky.domain.Service

@Composable
fun MainScreen(
    application: SkyPosterApp,
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("ポスト", "通知")
    val sessionManager = application.sessionManager
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var postText by remember { mutableStateOf("") }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(label) },
                        icon = {} // アイコンを追加したい場合はここに
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> {
                    // ポスト画面
                    TextField(
                        value = postText,
                        onValueChange = { postText = it },
                        label = { Text("今何してる？") },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )

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
                            postText = ""
                        }
                    }) {
                        Text("ポストする")
                    }
                }

                1 -> {
                    // 通知画面
                    NotificationListScreen(viewModel = remember { NotificationViewModel(NotificationRepository(sessionManager, context)) })
                }
            }
        }
    }
}
