package com.suibari.skyputter.ui.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suibari.skyputter.SkyPutterApp
import com.suibari.skyputter.util.SessionManager
import com.suibari.skyputter.util.Util
import kotlinx.coroutines.launch
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewDetailed
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    application: SkyPutterApp,
    viewModel: MainViewModel,
    onLogout: () -> Unit,
    onOpenNotification: () -> Unit,
    onOpenUserPost: () -> Unit,
    onOpenLikesBack: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var postText by remember { mutableStateOf("") }
    val uiState by viewModel.uiState
    val profile by viewModel.profile
    val embed by viewModel.embed

    val urlRegex = remember { Regex("""https?://\S+""") }

    // 初期化処理
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    // エラーメッセージ表示用のSnackbarHost
    val snackbarHostState = remember { SnackbarHostState() }

    // エラーメッセージが変更されたときにSnackbarを表示
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    // 画像選択用ランチャー
    val imageLauncher = rememberLauncherForActivityResult(
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    ProfileMenu(
                        profile = profile,
                        onOpenUserPost = onOpenUserPost,
                        onLogout = {
                            coroutineScope.launch {
                                SessionManager.clearSession()
                                onLogout()
                            }
                        }
                    )
                },
                actions = {
                    IconButton(onClick = onOpenLikesBack) {
                        Icon(Icons.Default.Favorite, contentDescription = "LikesBack")
                    }
                    IconButton(onClick = onOpenNotification) {
                        Icon(Icons.Default.Notifications, contentDescription = "通知")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                IconButton(
                    onClick = { imageLauncher.launch("image/*") },
                    enabled = !uiState.isPosting
                ) {
                    Icon(Icons.Default.Add, contentDescription = "画像添付")
                }

                Spacer(modifier = Modifier.weight(1f))

                PostButton(
                    isPosting = uiState.isPosting,
                    enabled = postText.isNotBlank() && !uiState.isPosting,
                    onClick = {
                        viewModel.post(postText, embed) {
                            // 投稿成功時の処理
                            postText = ""
                            viewModel.clearEmbed()
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // メインコンテンツ
            MainContent(
                modifier = Modifier.fillMaxSize(),
                postText = postText,
                onPostTextChange = { newText ->
                    postText = newText
                    // URL検出とOG画像取得
                    val foundUrl = urlRegex.find(newText)?.value
                    if (foundUrl != null) {
                        viewModel.fetchOgImage(foundUrl)
                    }
                },
                viewModel = viewModel,
                embed = embed
            )

            // ローディング表示
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    Card(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("初期化中...")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMenu(
    profile: ActorDefsProfileViewDetailed?,
    onOpenUserPost: () -> Unit,
    onLogout: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(profile?.avatar)
                .crossfade(true)
                .build(),
            contentDescription = "avatar",
            modifier = Modifier.size(48.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("プロフィール") },
                onClick = {
                    onOpenUserPost()
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("ログアウト") },
                onClick = {
                    onLogout()
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun PostButton(
    isPosting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled
    ) {
        if (isPosting) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("投稿中...")
            }
        } else {
            Text("ポスト")
        }
    }
}

@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    postText: String,
    onPostTextChange: (String) -> Unit,
    viewModel: MainViewModel,
    embed: AttachedEmbed?
) {
    Column(
        modifier = modifier
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
                onValueChange = onPostTextChange,
                label = { Text("今なにしてる？") },
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                enabled = !viewModel.uiState.value.isPosting
            )
        }

        // 返信先表示
        ReplyContextCard(
            parentPost = viewModel.parentPost,
            isVisible = viewModel.parentPostRecord != null,
            onClear = { viewModel.clearReplyContext() }
        )

        // 添付画像表示
        AttachedImageCard(
            embed = embed,
            onClear = { viewModel.clearEmbed() }
        )
    }
}

@Composable
private fun ReplyContextCard(
    parentPost: FeedPost?,
    isVisible: Boolean,
    onClear: () -> Unit
) {
    if (isVisible) {
        Card(modifier = Modifier.padding(top = 8.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row {
                    Text(
                        text = "返信先",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "閉じる")
                    }
                }
                Text(
                    text = parentPost?.text ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun AttachedImageCard(
    embed: AttachedEmbed?,
    onClear: () -> Unit
) {
    embed?.imageUri?.let { imageUri ->
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = embed.title ?: embed.filename ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onClear,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "添付画像を削除"
                )
            }
        }
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