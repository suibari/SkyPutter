package com.example.skyposter

import MainViewModel
import NotificationRepository
import NotificationViewModel
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.*
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.skyposter.ui.*
import com.example.skyposter.ui.theme.SkyPosterTheme
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as SkyPosterApp

        setContent {
            SkyPosterTheme {
                val context = LocalContext.current
                val navController = rememberNavController()
                val sessionManager = app.sessionManager
                val mainViewModel = remember { MainViewModel(sessionManager) }
                val notificationViewModel = remember {
                    NotificationViewModel(NotificationRepository(sessionManager, context))
                }

                val coroutineScope = rememberCoroutineScope()
                var isCheckedSession by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    if (sessionManager.hasSession()) {
                        scheduleNotificationWorker(context)

                        // 通知プリロード
//                        coroutineScope.launch {
//                            notificationViewModel.fetchNow()
//                        }

                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Loading.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Loading.route) { inclusive = true }
                        }
                    }
                    isCheckedSession = true
                }

                NavHost(
                    navController = navController,
                    startDestination = Screen.Loading.route
                ) {
                    composable(Screen.Loading.route) {
                        LoadingScreen()
                    }
                    composable(Screen.Login.route) {
                        LoginScreen(
                            application = app,
                            onLoginSuccess = {
                                coroutineScope.launch {
                                    notificationViewModel.fetchNow()
                                }
                                navController.navigate(Screen.Main.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Screen.Main.route) {
                        MainScreen(
                            application = app,
                            viewModel = mainViewModel,
                            onLogout = {
                                coroutineScope.launch {
                                    sessionManager.clearSession()
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Main.route) { inclusive = true }
                                    }
                                }
                            },
                            onOpenNotification = {
                                navController.navigate(Screen.NotificationList.route)
                            },
                            onOpenUserPost = {
                                navController.navigate(Screen.UserPost.route)
                            }
                        )
                    }
                    composable(Screen.NotificationList.route) {
                        NotificationListScreen(
                            viewModel = notificationViewModel,
                            mainViewModel = mainViewModel,
                            onNavigateToMain = {
                                navController.navigate("main")
                            }
                        )
                    }
                }
            }
        }
    }
}

fun scheduleNotificationWorker(context: Context) {
    val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
        15, TimeUnit.MINUTES
    ).build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "notification_worker",
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
}
