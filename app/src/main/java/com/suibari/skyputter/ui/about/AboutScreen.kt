package com.suibari.skyputter.ui.about

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("SkyPutterについて") })
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                text = "SkyPutterは、発信をサポートするためのBlueskyクライアントです。",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
