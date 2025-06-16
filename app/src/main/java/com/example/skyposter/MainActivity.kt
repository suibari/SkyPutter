package com.example.skyposter

import MainViewModel
import NotificationRepository
import NotificationViewModel
import Screen
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
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

                // notification factory
                val notificationRepo = NotificationRepository(sessionManager, context)
                val factoryNotification = remember { GenericViewModelFactory { NotificationViewModel(notificationRepo) } }
                val notificationViewModel: NotificationViewModel = viewModel(factory = factoryNotification)

                // profile factory
                val userPostRepo = UserPostRepository(sessionManager, context)
                val factoryUserPost = remember { GenericViewModelFactory { UserPostViewModel(userPostRepo) } }
                val userPostViewModel: UserPostViewModel = viewModel(factory = factoryUserPost)

                // likesback factory
                val likesbackRepo = LikesBackRepository(sessionManager, context)
                val factoryLikesBack = remember { GenericViewModelFactory { LikesBackViewModel(likesbackRepo) } }
                val likesBackViewModel: LikesBackViewModel = viewModel(factory = factoryLikesBack)

                val coroutineScope = rememberCoroutineScope()
                var isCheckedSession by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    if (sessionManager.hasSession()) {
                        scheduleNotificationWorker(context)

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
                                    notificationViewModel.loadInitialItems()
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
                            notificationViewModel = notificationViewModel,
                            userPostViewModel = userPostViewModel,
                            likesBackViewModel = likesBackViewModel,
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
                            },
                            onOpenLikesBack = {
                                navController.navigate(Screen.LikesBack.route)
                            },
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
                    composable(Screen.UserPost.route) {
                        UserPostListScreen(
                            viewModel = userPostViewModel,
                        )
                    }
                    composable(Screen.LikesBack.route) {
                        LikesBackScreen(
                            viewModel = likesBackViewModel,
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
