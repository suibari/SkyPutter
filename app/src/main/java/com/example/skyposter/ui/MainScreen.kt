package com.example.skyposter.ui

import MainViewModel
import NotificationViewModel
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.skyposter.LikesBackViewModel
import com.example.skyposter.SessionManager
import com.example.skyposter.UserPostViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.skyposter.BskyUtil
import work.socialhub.kbsky.model.app.bsky.embed.EmbedDefsAspectRatio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    application: SkyPosterApp,
    viewModel: MainViewModel,
    notificationViewModel: NotificationViewModel,
    userPostViewModel: UserPostViewModel,
    likesBackViewModel: LikesBackViewModel,
    onLogout: () -> Unit,
    onOpenNotification: () -> Unit,
    onOpenUserPost: () -> Unit,
    onOpenLikesBack: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var postText by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // 画像表示用ランチャー
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    // メイン画面バックグラウンド処理
    LaunchedEffect(Unit) {
        userPostViewModel.loadInitialItemsIfNeeded()
        notificationViewModel.loadInitialItemsIfNeeded()
        notificationViewModel.startPolling()
        likesBackViewModel.loadInitialItemsIfNeeded()
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
                    // 画像をByteArrayに変換
                    var blob: ByteArray? = null
                    var filename: String? = null
                    var contentType: String? = null
                    var aspectRatio: EmbedDefsAspectRatio? = null

                    if (imageUri != null) {
                        // blob
                        blob = BskyUtil.uriToByteArray(context, imageUri!!)
                        // filename
                        filename = BskyUtil.getFileName(context, imageUri!!)
                        // contentType
                        contentType = context.contentResolver.getType(imageUri!!)
                        // aspectRatio
                        aspectRatio = BskyUtil.getAspectRatioObject(context, imageUri!!)
                    }

                    viewModel.post(postText, blob, filename, contentType, aspectRatio)

                    // ポスト後は初期化
                    postText = ""
                    imageUri = null
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
                    onValueChange = { postText = it },
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
            imageUri?.let { uri ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(uri)
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