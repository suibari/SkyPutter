package com.example.skyposter.ui

import MainViewModel
import NotificationRepository
import NotificationViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.skyposter.SkyPosterApp
import kotlinx.coroutines.launch
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedPostRequest
import work.socialhub.kbsky.domain.Service
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewDetailed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    application: SkyPosterApp,
    viewModel: MainViewModel,
    onLogout: () -> Unit,
    onOpenNotification: () -> Unit,
    onOpenUserPost: () -> Unit,
) {
    val sessionManager = application.sessionManager
    val coroutineScope = rememberCoroutineScope()
    var postText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    // 左上ユーザーアイコン（ログアウトメニュー）
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(viewModel.getProfile()?.avatar)
                                .crossfade(true)
                                .build(),
                            contentDescription = "avatar",
                            modifier = Modifier
                                .size(48.dp)
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                text = { Text("プロフィール") },
                                onClick = {
                                    coroutineScope.launch {
                                        onOpenUserPost()
                                        expanded = false
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("ログアウト") },
                                onClick = {
                                    coroutineScope.launch {
                                        sessionManager.clearSession()
                                        onLogout()
                                    }
                                }
                            )
                        }
                    }
                },
                actions = {
                    // 右上通知アイコン
                    IconButton(onClick = onOpenNotification) {
                        Icon(Icons.Default.Notifications, contentDescription = "通知")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                // 左下画像添付（未実装）
                IconButton(onClick = {
                    // TODO: ギャラリー開く処理
                }) {
                    Icon(Icons.Default.Add, contentDescription = "画像添付")
                }

                Spacer(modifier = Modifier.weight(1f))

                // 右下ポストボタン
                Button(onClick = { viewModel.post(postText) }) {
                    Text("ポスト")
                }
            }
        }
    ) { innerPadding ->
        // 中央テキストフィールド
        TextField(
            value = postText,
            onValueChange = { postText = it },
            label = { Text("今なにしてる？") },
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth()
        )
    }
}
