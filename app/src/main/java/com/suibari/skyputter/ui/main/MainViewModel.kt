package com.suibari.skyputter.ui.main

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suibari.skyputter.data.db.SuggestionDatabase
import com.suibari.skyputter.data.db.SuggestionEntity
import com.suibari.skyputter.data.repository.MainRepository
import com.suibari.skyputter.data.repository.PostResult
import com.suibari.skyputter.data.repository.ProfileResult
import com.suibari.skyputter.data.repository.OgImageResult
import com.suibari.skyputter.data.repository.SuggestionBuilder
import com.suibari.skyputter.data.settings.NotificationSettings
import com.suibari.skyputter.ui.notification.NotificationViewModel
import com.suibari.skyputter.ui.post.UserPostViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import work.socialhub.kbsky.BlueskyTypes
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

open class MainViewModel(
    application: Application,
    private val repo: MainRepository,
    val userPostViewModel: UserPostViewModel,
    val notificationViewModel: NotificationViewModel,
) : AndroidViewModel(application) {

    protected var _uiState = mutableStateOf(UiState())
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
    val navigateToNotification = MutableSharedFlow<Unit>()

    // postText保持用
    var postText by mutableStateOf("")

    // セッション不正（ログアウトが必要）を通知するフロー
    private val _requireLogout = MutableStateFlow(false)
    val requireLogout = _requireLogout.asStateFlow()

    // サジェスト
    private val suggestionDao = SuggestionDatabase.getInstance(application).suggestionDao()
    private var searchJob: Job? = null
    private val _suggestions = MutableStateFlow<List<SuggestionEntity>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private var initializing = false
    fun initialize(context: Context) {
        // 初期化中の初期化を含め、1度だけ初期化
        if (_uiState.value.isInitialized || initializing) return

        initializing = true
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
                        Log.e("MainViewModel", "getProfile failed, requiring logout", result.exception)
                        _uiState.value = _uiState.value.copy(
                            errorMessage = result.message,
                            isLoading = false
                        )
                        _requireLogout.emit(true)
                        return@launch
                    }
                }

                // バックグラウンド処理開始: 設定ON時のみ
                val isNotificationPollingEnabled = NotificationSettings
                    .getNotificationPollingEnabled(context)
                    .firstOrNull() ?: true // 初回起動時はnullなのでtrueにフォールバック
                if (isNotificationPollingEnabled) {
                    notificationViewModel.startBackgroundPolling()
                }
                // 設定画面での変更を監視
                notificationViewModel.startSettingsWatcher(context)

                // 子ViewModelの初期化
                Log.d("MainViewModel", "loading child view models")
                userPostViewModel.loadInitialItemsIfNeeded()
                notificationViewModel.loadInitialItemsIfNeeded()

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
                _requireLogout.emit(true)
            } finally {
                initializing = false
            }
        }
    }

    open fun post(context: Context, postText: String, embeds: List<AttachedEmbed>?, onSuccess: () -> Unit = {}) {
        if (_uiState.value.isPosting) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPosting = true, errorMessage = null)

            val replyRef = createReplyRef()

            when (val result = repo.postText(context, postText, embeds, replyRef)) {
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

    fun showError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    fun addEmbed(newEmbed: AttachedEmbed) {
        // 新しいEmbedのタイプ
        val newType = newEmbed.type

        // 共存を許す唯一の組み合わせ: newType == Record または Recordと共存できる他の1つ
        val record = _embeds.find { it.type == BlueskyTypes.EmbedRecord }

        val mediaTypes = listOf(
            BlueskyTypes.EmbedImages,
            BlueskyTypes.EmbedVideo,
            BlueskyTypes.EmbedExternal
        )

        val isMedia = newType in mediaTypes
        val isRecord = newType == BlueskyTypes.EmbedRecord

        // ---- Step 1: 許可されない組み合わせを排除 ----
        if (isMedia) {
            // すでに存在する他のMediaがあればすべて除去（Record以外）
            _embeds.removeAll { it.type in mediaTypes }
        }

        if (isRecord) {
            // Recordは単体 or 既存Mediaと共存OKなので除去不要
            _embeds.removeAll { it.type == BlueskyTypes.EmbedRecord }
        }

        // ---- Step 2: EmbedImagesは複数許可、それ以外は1つのみ ----
        if (newType == BlueskyTypes.EmbedImages) {
            _embeds.add(newEmbed)
            return
        }

        // 同じタイプがあれば置換
        val existingIndex = _embeds.indexOfFirst { it.type == newType }
        if (existingIndex >= 0) {
            _embeds[existingIndex] = newEmbed
        } else {
            _embeds.add(newEmbed)
        }
    }

    // 特定の種類のEmbedを取得するヘルパーメソッド
    fun getEmbedByType(type: String): AttachedEmbed? {
        return _embeds.find { it.type == type }
    }

    // 種類別のクリア機能
    fun clearEmbedByType(type: String) {
        _embeds.removeAll { it.type == type }
    }

    // 指定クリア機能
    fun removeEmbed(embed: AttachedEmbed) {
        _embeds.remove(embed)
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

    /**
     * 使用禁止
     */
    fun onNavigatedToNotification() {
        viewModelScope.launch {
            navigateToNotification.emit(Unit)
        }
    }

    // 下位互換性のために残す
    @Deprecated("Use profile property instead", ReplaceWith("profile.value"))
    fun getProfile(): ActorDefsProfileViewDetailed? {
        return _profile.value
    }

    /**
     * mutable変数のログ出力
     */
    fun getDebugLogSnapshot(): String {
        return buildString {
            appendLine("MainViewModel.uiState: ${uiState.value}")
            appendLine("MainViewModel.profile: ${profile.value}")
            appendLine("MainViewModel.parentPostRecord.uri: ${parentPostRecord?.uri}")
            embeds.forEachIndexed { index, embed ->
                appendLine("MainViewModel.embed.images[$index].filename ${embed.filename}")
                appendLine("MainViewModel.embed.images[$index].uriString ${embed.uriString}")
                appendLine("MainViewModel.embed.images[$index].blob (size) ${embed.blob?.size}")
                appendLine("MainViewModel.embed.images[$index].contentType ${embed.contentType}")
                appendLine("MainViewModel.embed.images[$index].aspectRatio ${embed.aspectRatio}")
            }
            appendLine("MainViewModel.postText: $postText")
        }
    }

    /**
     * 投稿テキストの入力変化に応じてサジェスト検索し、ログに出力する
     * デバウンス300ms付き
     */
    fun searchSuggestionsDebounced(input: String) {
        searchJob?.cancel()
        if (input.isBlank()) return

        searchJob = viewModelScope.launch {
            delay(300)

            try {
                // 形態素解析でトークンを取得
                val tokens = withContext(Dispatchers.IO) {
                    SuggestionBuilder.sendToMorphServerSingle(input)
                }

                if (tokens.isEmpty()) {
                    Log.d("MainViewModel", "トークンなし、検索スキップ")
                    return@launch
                }

                // ORでつないで部分一致検索用に整形
                val query = tokens.joinToString(" OR ") { "$it*" }

                val results = suggestionDao.searchByTokens(query)
                _suggestions.value = results

                Log.d("MainViewModel", "Morph検索クエリ='$query', 件数=${results.size}")
                results.forEach {
                    Log.d("MainViewModel", "サジェスト候補: ${it.text}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "サジェスト検索失敗", e)
            }
        }
    }

    fun clearSuggestions() {
        _suggestions.value = listOf<SuggestionEntity>()
    }
}