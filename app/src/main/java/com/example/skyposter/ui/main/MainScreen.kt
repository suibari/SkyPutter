package com.example.skyposter.ui.main

import com.example.skyposter.ui.notification.NotificationViewModel
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.skyposter.SkyPosterApp
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.skyposter.ui.likesback.LikesBackViewModel
import com.example.skyposter.util.SessionManager
import com.example.skyposter.ui.post.UserPostViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import com.example.skyposter.util.BskyUtil
import com.example.skyposter.util.Util
import work.socialhub.kbsky.model.app.bsky.embed.EmbedDefsAspectRatio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    application: SkyPosterApp,
    viewModel: MainViewModel,
    onLogout: () -> Unit,
    onOpenNotification: () -> Unit,
    onOpenUserPost: () -> Unit,
    onOpenLikesBack: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var postText by remember { mutableStateOf("") }

    val urlRegex = Regex("""https?://\S+""")

    // 初期化処理
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    // 画像表示用ランチャー
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val (blob, contentType, aspectRatio) = Util.getByteArrayFromUri(context, uri)
            val filename = Util.getFileName(context, uri) ?: "image.jpg"

            viewModel.setEmbed(
                AttachedEmbed(
                    filename = filename,
                    imageUriString = uri.toString(),
                    blob = blob,
                    contentType = contentType,
                    aspectRatio = aspectRatio
                )
            )
        }
    }

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
                                        SessionManager.clearSession()
                                        onLogout()
                                    }
                                }
                            )
                        }
                    }
                },
                actions = {
                    // LikesBack
                    IconButton(onClick = onOpenLikesBack) {
                        Icon(Icons.Default.Favorite, contentDescription = "LikesBack")
                    }

                    // 右上通知アイコン
                    IconButton(onClick = onOpenNotification) {
                        Icon(Icons.Default.Notifications, contentDescription = "通知")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                // 左下画像添付
                IconButton(onClick = {
                    launcher.launch("image/*")
                }) {
                    Icon(Icons.Default.Add, contentDescription = "画像添付")
                }

                Spacer(modifier = Modifier.weight(1f))

                // 右下ポストボタン
                Button(onClick = {
                    viewModel.post(postText, viewModel.embed.value)

                    // ポスト後は初期化
                    postText = ""
                    viewModel.clearEmbed()
                }) {
                    Text("ポスト")
                }
            }
        }
    ) { innerPadding ->
        Column (
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(bottom = if (viewModel.parentPostRecord != null) 120.dp else 80.dp)
                    .fillMaxWidth()
            ) {

                TextField(
                    value = postText,
                    onValueChange = { newText ->
                        postText = newText

                        val foundUrl = urlRegex.find(newText)?.value
                        if (foundUrl != null) {
                            viewModel.fetchOgImage(foundUrl)
                        }
                    },
                    label = { Text("今なにしてる？") },
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                )
            }

            // 返信ポスト確認
            if (viewModel.parentPostRecord != null) {
                Card(
                    modifier = Modifier
                        .padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row {
                            Text(
                                text = "返信先",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearReplyContext() }) {
                                Icon(Icons.Default.Close, contentDescription = "閉じる")
                            }
                        }
                        Text(
                            text = viewModel.parentPost?.text ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            // 添付画像確認
            viewModel.embed.value?.imageUri?.let { imageUri ->
                Row {
                    Text(
                        text = viewModel.embed.value?.title ?: viewModel.embed.value?.filename ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                    )
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}
