package com.suibari.skyputter.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suibari.skyputter.data.settings.NotificationSettings
import com.suibari.skyputter.data.settings.SuggestionSettings
import com.suibari.skyputter.ui.main.MainViewModel
import com.suibari.skyputter.util.DebugLogUtil
import kotlinx.coroutines.launch
import com.suibari.skyputter.ui.settings.SuggestionProgressState.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    myDid: String,
    viewModel: SettingsViewModel,
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 通知設定状態
    val isNotificationPollingEnabled by NotificationSettings
        .getNotificationPollingEnabled(context)
        .collectAsState(initial = true)

    // サジェスト機能状態
    val isSuggestionEnabled by SuggestionSettings
        .getSuggestionEnabled(context)
        .collectAsState(initial = false)

    // ダイアログ表示制御
    var showSuggestionDialog by remember { mutableStateOf(false) }

    // 過去ポスト収集ローディングサークル
    val progress by viewModel.suggestionProgress.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            // 通知設定
            Text("通知設定", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("フォアグラウンド自動取得")
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = isNotificationPollingEnabled,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            NotificationSettings.setNotificationPollingEnabled(context, enabled)
                        }
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            // サジェスト設定
            Text("サジェスト", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("過去ポストからのサジェスト表示")
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = isSuggestionEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            showSuggestionDialog = true
                        } else {
                            coroutineScope.launch {
                                SuggestionSettings.setSuggestionEnabled(context, false)
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            // デバッグ
            Text("デバッグ", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Button(onClick = {
                coroutineScope.launch {
                    val logFile = DebugLogUtil.writeDebugLogToFile(context, mainViewModel)
                    DebugLogUtil.shareLogFile(context, logFile)
                }
            }) {
                Text("デバッグログを共有")
            }
        }
    }

    // ダイアログ：Wi-Fi推奨
    if (showSuggestionDialog) {
        AlertDialog(
            onDismissRequest = { showSuggestionDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showSuggestionDialog = false
                    coroutineScope.launch {
                        SuggestionSettings.setSuggestionEnabled(context, true)

                        // 収集処理
                        viewModel.initializeSuggestion(myDid)
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSuggestionDialog = false
                }) {
                    Text("キャンセル")
                }
            },
            title = { Text("Wi-Fi環境推奨") },
            text = {
                Text("初回のデータ収集には通信量がかかります。Wi-Fi環境下での実行を推奨します。")
            }
        )
    }

    // ローディング中のオーバーレイ
    if (progress != Idle) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = when (progress) {
                        CollectingPosts -> "ポスト収集中...\n(10秒ほどかかります)"
                        AnalyzingPosts -> "ポスト解析中...\n(30秒ほどかかります)"
                        SavingSuggestions -> "デバイスに保存中..."
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}