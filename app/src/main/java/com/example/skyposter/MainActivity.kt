package com.example.skyposter

import LoadingScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.skyposter.ui.theme.SkyPosterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SkyPosterTheme {
                val context = LocalContext.current
                val sessionManager = remember { SessionManager(context) }

                var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }

                // 起動時セッション確認
                LaunchedEffect(Unit) {
                    isLoggedIn = sessionManager.hasSession()
                }

                when (isLoggedIn) {
                    true -> MainScreen()
                    false -> LoginScreen ( onLoginSuccess = {isLoggedIn = true} )
                    null -> LoadingScreen()
                }
            }
        }
    }
}