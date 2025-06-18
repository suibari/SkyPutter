package com.suibari.skyposter

import com.suibari.skyposter.ui.notification.NotificationViewModel
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
import com.suibari.skyposter.data.model.GenericViewModelFactory
import com.suibari.skyposter.data.model.SessionViewModel
import com.suibari.skyposter.data.repository.LikesBackRepository
import com.suibari.skyposter.data.repository.MainRepository
import com.suibari.skyposter.data.repository.NotificationRepository
import com.suibari.skyposter.data.repository.UserPostRepository
import com.suibari.skyposter.ui.likesback.LikesBackScreen
import com.suibari.skyposter.ui.likesback.LikesBackViewModel
import com.suibari.skyposter.ui.loading.LoadingScreen
import com.suibari.skyposter.ui.login.LoginScreen
import com.suibari.skyposter.ui.main.MainScreen
import com.suibari.skyposter.ui.main.MainViewModel
import com.suibari.skyposter.ui.notification.NotificationListScreen
import com.suibari.skyposter.ui.post.UserPostListScreen
import com.suibari.skyposter.ui.post.UserPostViewModel
import com.suibari.skyposter.ui.theme.SkyPosterTheme
import com.suibari.skyposter.util.SessionManager
import com.suibari.skyposter.worker.NotificationWorker
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

                // session management
                val sessionViewModel: SessionViewModel = viewModel()
                val hasSession = sessionViewModel.hasSession
                val myDid = sessionViewModel.myDid

                // notification factory
                val notificationRepo = NotificationRepository(context)
                val factoryNotification = GenericViewModelFactory { NotificationViewModel(notificationRepo) }
                val notificationViewModel: NotificationViewModel = viewModel(factory = factoryNotification)

                // profile factory
                val userPostRepo = UserPostRepository()
                val factoryUserPost = GenericViewModelFactory { UserPostViewModel(userPostRepo) }
                val userPostViewModel: UserPostViewModel = viewModel(factory = factoryUserPost)

                // likesback factory
                val likesbackRepo = LikesBackRepository()
                val factoryLikesBack = GenericViewModelFactory { LikesBackViewModel(likesbackRepo) }
                val likesBackViewModel: LikesBackViewModel = viewModel(factory = factoryLikesBack)

                // main factory
                val mainRepo = MainRepository()
                val factoryMain = GenericViewModelFactory { MainViewModel(
                    repo = mainRepo,
                    userPostViewModel = userPostViewModel,
                    notificationViewModel = notificationViewModel,
                    likesBackViewModel = likesBackViewModel,
                ) }
                val mainViewModel: MainViewModel = viewModel(factory = factoryMain)

                val coroutineScope = rememberCoroutineScope()

                NavHost(
                    navController = navController,
                    startDestination = when (hasSession) {
                        null -> Screen.Loading.route
                        true -> Screen.Main.route
                        false -> Screen.Login.route
                    }
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
                        if (myDid != null) {
                            MainScreen(
                                application = app,
                                viewModel = mainViewModel,
                                onLogout = {
                                    coroutineScope.launch {
                                        SessionManager.clearSession()
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
                            myDid = myDid!!,
                        )
                    }
                    composable(Screen.LikesBack.route) {
                        LikesBackScreen(
                            viewModel = likesBackViewModel,
                            mainViewModel = mainViewModel,
                            myDid = myDid!!,
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
