package com.suibari.skyputter.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.suibari.skyputter.data.settings.NotificationSettings
import com.suibari.skyputter.ui.main.MainViewModel
import com.suibari.skyputter.util.DebugLogUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // DataStoreから直接購読
    val isNotificationPollingEnabled by NotificationSettings
        .getNotificationPollingEnabled(context)
        .collectAsState(initial = true)

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
            // 通知関連
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

            // デバッグ関連
            Text("デバッグ", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Button(onClick = {
                coroutineScope.launch {
                    // ログ出力して共有Intent起動
                    val logFile = DebugLogUtil.writeDebugLogToFile(context, mainViewModel)
                    DebugLogUtil.shareLogFile(context, logFile)
                }
            }) {
                Text("デバッグログを共有")
            }
        }
    }
}