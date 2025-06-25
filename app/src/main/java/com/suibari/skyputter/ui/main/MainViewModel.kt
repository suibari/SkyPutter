package com.suibari.skyputter.ui.main

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suibari.skyputter.data.repository.MainRepository
import com.suibari.skyputter.data.repository.PostResult
import com.suibari.skyputter.data.repository.ProfileResult
import com.suibari.skyputter.data.repository.OgImageResult
import com.suibari.skyputter.ui.likesback.LikesBackViewModel
import com.suibari.skyputter.ui.notification.NotificationViewModel
import com.suibari.skyputter.ui.post.UserPostViewModel
import kotlinx.coroutines.launch
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileView
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewDetailed
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.app.bsky.feed.FeedPostReplyRef
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

data class UiState(
    val isLoading: Boolean = false,
    val isPosting: Boolean = false,
    val isFetchingOgImage: Boolean = false,
    val errorMessage: String? = null,
    val isInitialized: Boolean = false
)

class MainViewModel(
    private val repo: MainRepository,
    val userPostViewModel: UserPostViewModel,
    val notificationViewModel: NotificationViewModel,
    val likesBackViewModel: LikesBackViewModel,
) : ViewModel() {

    private var _uiState = mutableStateOf(UiState())
    val uiState: MutableState<UiState> = _uiState

    private var _profile = mutableStateOf<ActorDefsProfileViewDetailed?>(null)
    val profile: MutableState<ActorDefsProfileViewDetailed?> = _profile

    // 返信先ポスト情報
    var parentPostRecord by mutableStateOf<RepoStrongRef?>(null)
        private set
    var parentPost by mutableStateOf<FeedPost?>(null)
        private set
    var parentAuthor by mutableStateOf<ActorDefsProfileView?>(null)
        private set
    private var rootPostRecord by mutableStateOf<RepoStrongRef?>(null)

    // 添付画像情報
    private val _embeds = mutableStateListOf<AttachedEmbed>()
    val embeds: List<AttachedEmbed> get() = _embeds

    // デバイス通知からの遷移用
    val navigateToNotification = mutableStateOf(false)

    fun initialize(context: Context) {
        if (_uiState.value.isInitialized) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                Log.d("MainViewModel", "initialize: start")

                // プロフィール取得
                when (val result = repo.getProfile()) {
                    is ProfileResult.Success -> {
                        _profile.value = result.profile
                        Log.d("MainViewModel", "profile loaded")
                    }
                    is ProfileResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = result.message,
                            isLoading = false
                        )
                        return@launch
                    }
                }

                // バックグラウンド処理開始
                notificationViewModel.startBackgroundPolling()

                // 子ViewModelの初期化
                Log.d("MainViewModel", "loading child view models")
                userPostViewModel.loadInitialItemsIfNeeded()
                notificationViewModel.loadInitialItemsIfNeeded()
                likesBackViewModel.loadInitialItemsIfNeeded()

                Log.d("MainViewModel", "initialization finished")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isInitialized = true,
                    errorMessage = null
                )

            } catch (e: Exception) {
                Log.e("MainViewModel", "initialize error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "初期化に失敗しました: ${e.message}"
                )
            }
        }
    }

    fun post(postText: String, embeds: List<AttachedEmbed>?, onSuccess: () -> Unit = {}) {
        if (_uiState.value.isPosting) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPosting = true, errorMessage = null)

            val replyRef = createReplyRef()

            when (val result = repo.postText(postText, embeds, replyRef)) {
                is PostResult.Success -> {
                    _uiState.value = _uiState.value.copy(isPosting = false)
                    onSuccess()
                }
                is PostResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isPosting = false,
                        errorMessage = result.message
                    )
                    Log.e("MainViewModel", "post error", result.exception)
                }
            }
        }
    }

    fun fetchOgImage(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFetchingOgImage = true)

            when (val result = repo.fetchOgImageEmbed(url)) {
                is OgImageResult.Success -> {
                    _embeds.add(result.embed)
                }
                is OgImageResult.Error -> {
                    Log.e("MainViewModel", "fetchOgImage error", result.exception)
                    // OG画像取得失敗は致命的ではないのでエラーメッセージは表示しない
                }
                is OgImageResult.NotFound -> {
                    // OG画像が見つからない場合は何もしない
                }
            }

            _uiState.value = _uiState.value.copy(isFetchingOgImage = false)
        }
    }

    fun setReplyContext(parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost, parentAuthor: ActorDefsProfileView) {
        this.parentPostRecord = parentRef
        this.rootPostRecord = rootRef
        this.parentPost = parentPost
        this.parentAuthor = parentAuthor
    }

    fun clearReplyContext() {
        this.parentPostRecord = null
        this.rootPostRecord = null
        this.parentPost = null
        this.parentAuthor = null
    }

    fun addEmbed(newEmbed: AttachedEmbed) {
        _embeds.add(newEmbed)
    }

    fun clearEmbed(embed: AttachedEmbed? = null) {
        if (embed != null) {
            _embeds.remove(embed)
        } else {
            _embeds.clear()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun createReplyRef(): FeedPostReplyRef? {
        return parentPostRecord?.let {
            FeedPostReplyRef().apply {
                root = rootPostRecord
                parent = parentPostRecord
            }
        }
    }

    fun onNavigatedToNotification() {
        navigateToNotification.value = false
    }

    // 下位互換性のために残す
    @Deprecated("Use profile property instead", ReplaceWith("profile.value"))
    fun getProfile(): ActorDefsProfileViewDetailed? {
        return _profile.value
    }
}