package com.suibari.skyposter

// Screen.kt
sealed class Screen(val route: String) {
    object Loading : Screen("loading")
    object Login : Screen("login")
    object Main : Screen("main")
    object NotificationList : Screen("notification_list")
    object UserPost : Screen("user_post")
    object LikesBack : Screen("likes_back")
}
