package com.suibari.skyputter

import android.content.Context
import com.suibari.skyputter.data.repository.MainRepository
import com.suibari.skyputter.ui.main.AttachedEmbed
import com.suibari.skyputter.ui.main.MainViewModel
import com.suibari.skyputter.ui.notification.NotificationViewModel
import com.suibari.skyputter.ui.post.UserPostViewModel

class SuccessMainViewModel(
    repo: MainRepository,
    userPostViewModel: UserPostViewModel,
    notificationViewModel: NotificationViewModel
) : MainViewModel(repo, userPostViewModel, notificationViewModel) {
    override fun post(
        context: Context,
        text: String,
        embeds: List<AttachedEmbed>?,
        onSuccess: () -> Unit
    ) {
        onSuccess()
    }
}

class ErrorMainViewModel(
    repo: MainRepository,
    userPostViewModel: UserPostViewModel,
    notificationViewModel: NotificationViewModel
) : MainViewModel(repo, userPostViewModel, notificationViewModel) {
    override fun post(
        context: Context,
        text: String,
        embeds: List<AttachedEmbed>?,
        onSuccess: () -> Unit
    ) {
        uiState.value = uiState.value.copy(errorMessage = "投稿に失敗しました")
    }

    // テスト用エラーメッセージ注入メソッド
    fun emitErrorForTest(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }
}