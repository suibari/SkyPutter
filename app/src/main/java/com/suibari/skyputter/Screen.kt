package com.suibari.skyputter

// Screen.kt
sealed class Screen(val route: String) {
    object Loading : Screen("loading")
    object Login : Screen("login")
    object Main : Screen("main")
    object NotificationList : Screen("notification_list")
    object UserPost : Screen("user_post")
    object Draft: Screen("draft")
    object Settings: Screen("settings")
    object About: Screen("about")
    object Calendar: Screen("calendar")
}
