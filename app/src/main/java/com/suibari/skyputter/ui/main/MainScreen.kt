package com.suibari.skyputter.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.room.util.TableInfo
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suibari.skyputter.SkyPutterApp
import com.suibari.skyputter.data.db.SuggestionEntity
import com.suibari.skyputter.data.settings.NotificationSettings
import com.suibari.skyputter.ui.notification.NotificationViewModel
import com.suibari.skyputter.ui.theme.spacePadding
import com.suibari.skyputter.util.DraftViewModel
import com.suibari.skyputter.util.SessionManager
import com.suibari.skyputter.util.Util
import com.suibari.skyputter.util.Util.formatDeviceLocaleDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import work.socialhub.kbsky.BlueskyTypes
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileView
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewDetailed
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

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
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onDraftTextCleared: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var postText = viewModel.postText
    val uiState by viewModel.uiState
    val profile by viewModel.profile
    val embeds = viewModel.embeds

    val urlRegex = remember { Regex("""https?://\S+""") }
    var lastFetchedUrl by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("Permission", "通知許可: $isGranted")
    }

    val showDialog = remember { mutableStateOf(false) }
    var dialogAlreadyShown by remember { mutableStateOf(true) }

    // プロフィール取得エラー時、強制ログアウト
    LaunchedEffect(Unit) {
        viewModel.requireLogout
            .filter { it } // trueの時のみ
            .collect {
                onLogout()
            }
    }

    // ユーザーに通知許可を求める
    LaunchedEffect(Unit) {
        NotificationSettings.getNotificationDialogShown(context).collect { alreadyShown ->
            dialogAlreadyShown = alreadyShown

            // 初めての表示であれば、通知権限未許可か確認
            if (!alreadyShown && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !context.hasNotificationPermission()) {
                showDialog.value = true
            }
        }
    }

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            confirmButton = {
                TextButton(onClick = {
                    showDialog.value = false
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

                    // ダイアログを表示済みに記録
                    coroutineScope.launch {
                        NotificationSettings.setNotificationDialogShown(context, true)
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog.value = false
                    coroutineScope.launch {
                        NotificationSettings.setNotificationDialogShown(context, true)
                    }
                }) {
                    Text("あとで")
                }
            },
            title = { Text("通知の許可について") },
            text = {
                Text("SkyPutterは、あなたへのリプライやいいねをすぐにお知らせするために通知を使用します。通知を受け取るには、許可をお願いします。")
            }
        )
    }

    // デバイス通知からの遷移イベント監視
    LaunchedEffect(Unit) {
        viewModel.navigateToNotification.collect {
            onOpenNotification()
        }
    }

    // 下書きから選択されたテキストを設定
    LaunchedEffect(initialText) {
        if (initialText.isNotEmpty()) {
            viewModel.postText = initialText
            onDraftTextCleared() // テキスト設定後にクリア
        }
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
    // MainScreenで画像選択処理を最適化
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            // コルーチンでバックグラウンド処理
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        viewModel.clearEmbed() // メインスレッドで実行
                    }

                    uris.take(4).forEach { uri ->
                        try {
                            val contentType = context.contentResolver.getType(uri)
                            val filename = Util.getFileName(context, uri) ?: "media.dat"

                            when {
                                contentType?.startsWith("image/") == true -> {
                                    // 画像の場合は軽量処理
                                    val (blob, actualContentType, aspectRatio) =
                                        Util.getImageFromUri(context, uri)

                                    withContext(Dispatchers.Main) {
                                        viewModel.addEmbed(
                                            AttachedEmbed(
                                                type = BlueskyTypes.EmbedImages,
                                                filename = filename,
                                                uriString = uri.toString(),
                                                blob = blob,
                                                contentType = actualContentType,
                                                aspectRatio = aspectRatio,
                                            )
                                        )
                                    }
                                }

                                contentType?.startsWith("video/") == true -> {
                                    // 動画の場合はメタデータのみ取得、実際の処理は投稿時
                                    val aspectRatio = Util.getVideoAspectRatio(context, uri)

                                    withContext(Dispatchers.Main) {
                                        viewModel.addEmbed(
                                            AttachedEmbed(
                                                type = BlueskyTypes.EmbedVideo,
                                                filename = filename,
                                                uriString = uri.toString(), // URIのみ保持
                                                blob = null, // 投稿時に処理
                                                contentType = contentType,
                                                aspectRatio = aspectRatio,
                                            )
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                viewModel.showError("ファイル読み込みエラー: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("SnackBarHost")
            )
        },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    ProfileMenu(
                        profile = profile,
                        onOpenUserPost = onOpenUserPost,
                        onOpenAbout = onOpenAbout,
                        onOpenSettings = onOpenSettings,
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
            BottomAppBar(
                modifier = Modifier.imePadding() // キーボードの上まで押し上げる
            ) {
                IconButton(
                    onClick = { imageLauncher.launch(arrayOf("image/*", "video/*")) },
                    enabled = !uiState.isPosting
                ) {
                    Icon(Icons.Default.Add, contentDescription = "画像添付")
                }

                Spacer(modifier = Modifier.weight(1f))

                // 下書きボタンを追加
                DraftButton(
                    postText = viewModel.postText,
                    viewModel = draftViewModel,
                    onOpenDraft = onOpenDraft,
                    onDraftSaved = {
                        // 下書き保存成功時、テキストクリア
                        viewModel.postText = ""
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                PostButton(
                    isPosting = uiState.isPosting,
                    enabled = postText.isNotBlank() && !uiState.isPosting,
                    onClick = {
                        coroutineScope.launch {
                            viewModel.post(context, postText, embeds) {
                                // 投稿成功時の処理（コルーチン内なのでOK）
                                viewModel.postText = ""
                                viewModel.clearEmbed()
                                viewModel.clearReplyContext()
                                lastFetchedUrl = null
                                viewModel.clearSuggestions()
                            }
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
                postText = viewModel.postText,
                onPostTextChange = { it ->
                    viewModel.postText = it

                    // サジェスト検索呼び出し
                    viewModel.searchSuggestionsDebounced(it)

                    // URL検出とOG画像取得
                    val match = urlRegex.find(it)
                    val foundUrl = match?.value?.trim()

                    // テキスト全消去
                    if (it.isBlank()) {
                        // URL履歴をリセット
                        lastFetchedUrl = null

                        // サジェストリセット
                        viewModel.clearSuggestions()

                        return@MainContent
                    }

                    // URLが見つかるかつ初回のみ実行
                    if (lastFetchedUrl == null && foundUrl != null) {
                        viewModel.clearEmbed()
                        viewModel.fetchOgImage(foundUrl)
                        lastFetchedUrl = foundUrl
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
    onOpenAbout: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .size(48.dp)
            .clip(CircleShape) // 丸く切り抜いてから...
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onOpenUserPost() },   // 通常タップ
                    onLongPress = { expanded = true } // 長押しでメニュー表示
                )
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(profile?.avatar)
                .crossfade(true)
                .build(),
            contentDescription = "avatar",
            contentScale = ContentScale.Crop, // クロップする必要あり
            modifier = Modifier
                .fillMaxSize()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("About") },
                onClick = {
                    onOpenAbout()
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("設定") },
                onClick = {
                    onOpenSettings()
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
    val suggestionList by viewModel.suggestions.collectAsState()

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f)
        ) {
            TextField(
                value = postText,
                onValueChange = onPostTextChange,
                label = { Text("今なにしてる？") },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .testTag("PostInput"),
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

        SuggestionList(
            suggestions = suggestionList,
            modifier = Modifier.weight(1f),
            onSuggestionClick = { null },
        )

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
            isFetchingOgImage = viewModel.uiState.value.isFetchingOgImage,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onClear = { viewModel.clearEmbed(it) }
        )
    }
}

@Composable
fun SuggestionList(
    suggestions: List<SuggestionEntity>,
    modifier: Modifier,
    onSuggestionClick: (String) -> Unit
) {
    if (suggestions.isEmpty()) return

    val suggestion = remember(suggestions) { suggestions.random() }

    Row(
        modifier = modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Lightbulb,
            contentDescription = "サジェストアイコン",
            modifier = Modifier.padding(end = 8.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    TextButton(
                        onClick = { onSuggestionClick(suggestion.text) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        shape = RectangleShape
                    ) {
                        Text(
                            text = suggestion.text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text(
                        text = formatDeviceLocaleDate(suggestion.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplyContextCard(
    parentPost: FeedPost?,
    parentAuthor: ActorDefsProfileView?,
    modifier: Modifier,
    onClear: () -> Unit
) {
    Row(
        modifier = modifier
            .padding(top = 8.dp)
            .fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Reply,
            contentDescription = "返信アイコン",
            modifier = Modifier
                .padding(end = 8.dp)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(parentAuthor?.avatar)
                        .crossfade(true)
                        .build(),
                    contentDescription = "avatar",
                    modifier = Modifier.size(32.dp)
                )

                Spacer(Modifier.spacePadding)

                Text(
                    text = parentPost?.text ?: "",
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
}

// AttachedImageCardの修正
@Composable
private fun AttachedImageCard(
    embeds: List<AttachedEmbed>?,
    isFetchingOgImage: Boolean,
    modifier: Modifier,
    onClear: (AttachedEmbed) -> Unit
) {
    // OG画像取得中の場合はローディングを表示
    if (isFetchingOgImage && embeds.isNullOrEmpty()) {
        Card(
            modifier = modifier
                .padding(top = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "リンクカード生成中...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    // embedsが空の場合は何も表示しない
    if (embeds.isNullOrEmpty()) return

    Row(
        modifier = modifier
            .padding(top = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AttachFile,
            contentDescription = "添付アイコン",
            modifier = Modifier
                .padding(end = 8.dp)
                .align(Alignment.CenterVertically),
            tint = MaterialTheme.colorScheme.primary
        )

        embeds.take(4).forEach { embed ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        // imageUrlもtitleもある場合：左に画像、右にtitle/description
                        embed.type == BlueskyTypes.EmbedExternal && embed.uri != null -> {
                            Row(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(embed.uri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "OG Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(8.dp)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = embed.title!!,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    embed.description?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        // imageUrlのみ（添付画像）：画像を全面に表示
                        embed.type == BlueskyTypes.EmbedImages -> {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(embed.uri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Attached Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // titleのみ（OG画像のないサイト）：titleを全面に表示
                        embed.type == BlueskyTypes.EmbedExternal -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = embed.title!!,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                embed.description?.let {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        // 引用
                        embed.type == BlueskyTypes.EmbedRecord -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                embed.post?.let {
                                    Text(
                                        text = it.text ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        // 動画: TODO
                        embed.type == BlueskyTypes.EmbedVideo -> {
                            Text("動画")
                        }
                    }
                }

                // 削除ボタン
                IconButton(
                    onClick = { onClear(embed) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "削除",
                        tint = Color.White,
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .padding(2.dp)
                    )
                }
            }
        }
    }
}

/**
 * デバイス通知許可
 */
fun Context.hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    } else {
        true // Android 12以下は不要
    }
}