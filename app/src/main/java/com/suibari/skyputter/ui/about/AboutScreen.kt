package com.suibari.skyputter.ui.about

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.suibari.skyputter.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SkyPutterについて") },
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // キャッチコピー
            Text(
                text = "あなたの発信、\n" + "もっと自由に。",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // アプリロゴ
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painterResource(id = R.drawable.logo_skyputter), contentDescription = "App Logo")
            }

            // 説明文
            Text(
                text = "SkyPutterは、BlueskyのサードパーティAndroidアプリです。\n" + "インプットを選択集中しつつ、アウトプットが捗る、そんなコンセプトで開発しました。",
                style = MaterialTheme.typography.bodyLarge
            )

            // 説明文2
            Text(
                text = "他の人の情報や、いいねリポストの数などを可能かなぎり排除していますが、もらったリプライやいいねの通知はすべて受け取れ、それに返信などのリアクションもできるようにつくっています。",
                style = MaterialTheme.typography.bodyLarge
            )

            // 情報リンク（共通スタイル）
            InfoLink("開発者: すいばり", "https://bsky.app/profile/suibari.com", context)
            InfoLink("マニュアル: GitHub", "https://github.com/suibari/SkyPutter", context)
            InfoLink("開発支援 (OFUSE)", "https://ofuse.me/suibari", context)

            Spacer(Modifier.height(8.dp))

            // プライバシーポリシー
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("プライバシーポリシー", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "SkyPutterは、ユーザーの個人情報を収集・保存しません。\n" + "BlueskyのAPIを通じて投稿・通知を取得しますが、データは端末内でのみ処理されます。"
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoLink(label: String, url: String, context: android.content.Context) {
    Text(
        text = label,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                context.startActivity(intent)
            }
    )
}
