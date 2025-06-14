package com.example.skyposter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.skyposter.SkyPosterApp
import kotlinx.coroutines.*
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.domain.Service
import work.socialhub.kbsky.api.entity.com.atproto.server.ServerCreateSessionRequest

@Composable
fun LoginScreen(
    application: SkyPosterApp,
    onLoginSuccess: () -> Unit
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val sessionManager = application.sessionManager

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("SkyPoster ログイン", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = { Text("Handle または Email") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("パスワード") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isLoading = true
                errorMessage = null
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Blueskyログイン
                        val response = BlueskyFactory
                            .instance(Service.BSKY_SOCIAL.uri)
                            .server()
                            .createSession(ServerCreateSessionRequest().also {
                                it.identifier = identifier
                                it.password = password
                            })
                        val accessJwt = response.data.accessJwt
                        val refreshJwt = response.data.refreshJwt

                        // セッション保存
                        sessionManager.saveSession(accessJwt, refreshJwt, identifier)

                        withContext(Dispatchers.Main) {
                            onLoginSuccess()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            errorMessage = "ログイン失敗：${e.message}"
                            isLoading = false
                        }
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "ログイン中…" else "ログイン")
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}