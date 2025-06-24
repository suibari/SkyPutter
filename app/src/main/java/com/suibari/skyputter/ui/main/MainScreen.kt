package com.suibari.skyputter.ui.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suibari.skyputter.SkyPutterApp
import com.suibari.skyputter.ui.notification.NotificationViewModel
import com.suibari.skyputter.ui.theme.spacePadding
import com.suibari.skyputter.util.DraftViewModel
import com.suibari.skyputter.util.SessionManager
import com.suibari.skyputter.util.Util
import kotlinx.coroutines.launch
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileView
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewDetailed
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    application: SkyPutterApp,
    viewModel: MainViewModel,
    notificationViewModel: NotificationViewModel,
    draftViewModel: DraftViewModel,
    initialText: String = "",
    onLogout: () -> Unit,
    onOpenNotification: () -> Unit,
    onOpenUserPost: () -> Unit,
//    onOpenLikesBack: () -> Unit,
    onOpenDraft: () -> Unit,
    onDraftTextCleared: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var postText by remember { mutableStateOf("") }
    val uiState by viewModel.uiState
    val profile by viewModel.profile
    val embeds = viewModel.embeds

    val urlRegex = remember { Regex("""https?://\S+""") }

    // デバイス通知からの遷移イベント監視
    LaunchedEffect(viewModel.navigateToNotification.value) {
        if (viewModel.navigateToNotification.value) {
            onOpenNotification() // ← navController.navigate(Screen.NotificationList.route)
            viewModel.onNavigatedToNotification()
        }
    }

    // 下書きから選択されたテキストを設定
    LaunchedEffect(initialText) {
        if (initialText.isNotEmpty()) {
            postText = initialText
            onDraftTextCleared() // テキスト設定後にクリア
        }
    }

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
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.clearEmbed() // ループの外で一度だけクリア
            uris.take(4).forEach { uri ->
                val (blob, contentType, aspectRatio) = Util.getByteArrayFromUri(context, uri)
                val filename = Util.getFileName(context, uri) ?: "image.jpg"
                viewModel.addEmbed(
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
//                    IconButton(onClick = onOpenLikesBack) {
//                        Icon(Icons.Default.Favorite, contentDescription = "LikesBack")
//                    }
                    IconButton(onClick = onOpenNotification) {
                        BadgedBox(
                            badge = {
                                if (notificationViewModel.items.any { it.isNew }) {
                                    Badge()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "通知")
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                IconButton(
                    onClick = { imageLauncher.launch(arrayOf("image/*")) },
                    enabled = !uiState.isPosting
                ) {
                    Icon(Icons.Default.Add, contentDescription = "画像添付")
                }

                Spacer(modifier = Modifier.weight(1f))

                // 下書きボタンを追加
                DraftButton(
                    postText = postText,
                    viewModel = draftViewModel,
                    onOpenDraft = onOpenDraft,
                    onDraftSaved = {
                        // 下書き保存成功時、テキストクリア
                        postText = ""
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                PostButton(
                    isPosting = uiState.isPosting,
                    enabled = postText.isNotBlank() && !uiState.isPosting,
                    onClick = {
                        viewModel.post(postText, embeds) {
                            // 投稿成功時の処理
                            postText = ""
                            viewModel.clearEmbed()
                            viewModel.clearReplyContext()
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
                        viewModel.clearEmbed()
                        viewModel.fetchOgImage(foundUrl)
                    }
                },
                viewModel = viewModel,
                embeds = embeds
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
private fun DraftButton(
    postText: String,
    viewModel: DraftViewModel,
    onOpenDraft: () -> Unit,
    onDraftSaved: () -> Unit,
) {
    val hasDrafts = remember { mutableStateOf(viewModel.hasDrafts()) }
    val hasText = postText.isNotEmpty()

    Button(
        onClick = {
            if (hasText) {
                // 下書き保存
                viewModel.saveDraft(postText)
                hasDrafts.value = true
                onDraftSaved()
            } else {
                // 下書き画面を開く
                onOpenDraft()
            }
        },
        enabled = hasText || hasDrafts.value,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = Color.Gray
        )
    ) {
        Text(
            text = if (hasText) "下書き保存" else "下書き",
//            color = if (hasText || hasDrafts.value) Color.White else Color.Gray
        )
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
        enabled = enabled,
        colors = ButtonDefaults.buttonColors (
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = Color.Gray
        )
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
    embeds: List<AttachedEmbed>?
) {
    val hasReply = viewModel.parentPostRecord != null

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (hasReply) 5f else 6f)
        ) {
            TextField(
                value = postText,
                onValueChange = onPostTextChange,
                label = { Text("今なにしてる？") },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                enabled = !viewModel.uiState.value.isPosting,
                maxLines = Int.MAX_VALUE
            )
            Text(
                text = "${postText.length}/300",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }

        // 返信先表示
        if (hasReply) {
            ReplyContextCard(
                parentPost = viewModel.parentPost,
                parentAuthor = viewModel.parentAuthor,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onClear = { viewModel.clearReplyContext() }
            )
        }

        // 添付画像表示
        AttachedImageCard(
            embeds = embeds,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onClear = { viewModel.clearEmbed(it) }
        )
    }
}

@Composable
private fun ReplyContextCard(
    parentPost: FeedPost?,
    parentAuthor: ActorDefsProfileView?,
    modifier: Modifier,
    onClear: () -> Unit
) {
    Card(
        modifier = modifier
            .padding(top = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage( // アイコン
                model = ImageRequest.Builder(LocalContext.current)
                    .data(parentAuthor?.avatar)
                    .crossfade(true)
                    .build(),
                contentDescription = "avatar",
                modifier = Modifier.size(32.dp)
            )

            Spacer (Modifier.spacePadding)

            Text(
                text = parentPost?.text ?: "",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "閉じる")
            }
        }
    }
}

@Composable
private fun AttachedImageCard(
    embeds: List<AttachedEmbed>?,
    modifier: Modifier,
    onClear: (AttachedEmbed) -> Unit
) {
    if (embeds.isNullOrEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp) // 高さを一定に
    ) {
        embeds.forEach { embed ->
            embed.imageUri?.let { imageUri ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Selected Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    IconButton(
                        onClick = { onClear(embed) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "添付画像を削除",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
