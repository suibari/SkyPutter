package com.example.skyposter.ui.main

import com.example.skyposter.ui.notification.NotificationViewModel
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skyposter.data.repository.MainRepository
import com.example.skyposter.ui.likesback.LikesBackViewModel
import com.example.skyposter.util.SessionManager
import com.example.skyposter.ui.post.UserPostViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.actor.ActorGetProfileRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedPostRequest
import work.socialhub.kbsky.api.entity.com.atproto.repo.RepoUploadBlobRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewDetailed
import work.socialhub.kbsky.model.app.bsky.embed.EmbedDefsAspectRatio
import work.socialhub.kbsky.model.app.bsky.embed.EmbedImages
import work.socialhub.kbsky.model.app.bsky.embed.EmbedImagesImage
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.app.bsky.feed.FeedPostReplyRef
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef
import java.io.IOException
import java.net.URL

class MainViewModel(
    val repo: MainRepository,
    val userPostViewModel: UserPostViewModel,
    val notificationViewModel: NotificationViewModel,
    val likesBackViewModel: LikesBackViewModel,
) : ViewModel() {
    private var _profile = mutableStateOf<ActorDefsProfileViewDetailed?>(null)
    var parentPostRecord by mutableStateOf<RepoStrongRef?>(null)
    var parentPost by mutableStateOf<FeedPost?>(null)
    private var rootPostRecord by mutableStateOf<RepoStrongRef?>(null)

    private val _embed = mutableStateOf<AttachedEmbed?>(null)
    val embed: MutableState<AttachedEmbed?> = _embed
    private var isFetchingOgImage = false


    fun initialize() {
        viewModelScope.launch {
            try {
                // UIに関係するものはメインでOK
                _profile.value = repo.getProfile()
                notificationViewModel.startPolling()

                // UI側と関係の薄いロジックもできるだけIOで
                withContext(Dispatchers.IO) {
                    userPostViewModel.loadInitialItemsIfNeeded()
                    notificationViewModel.loadInitialItemsIfNeeded()
                    likesBackViewModel.loadInitialItemsIfNeeded()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getProfile(): ActorDefsProfileViewDetailed? {
        return _profile.value
    }

    fun post(postText: String, embed: AttachedEmbed?) {
        viewModelScope.launch {
            val replyRef = parentPostRecord?.let {
                FeedPostReplyRef().apply {
                    root = rootPostRecord
                    parent = parentPostRecord
                }
            }

            try {
                repo.postText(postText, embed, replyRef)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setReplyContext(parentRef: RepoStrongRef, rootRef: RepoStrongRef, parentPost: FeedPost) {
        this.parentPostRecord = parentRef
        this.rootPostRecord = rootRef
        this.parentPost = parentPost
    }

    fun clearReplyContext() {
        this.parentPostRecord = null
        this.rootPostRecord = null
        this.parentPost = null
    }

    fun setEmbed(newEmbed: AttachedEmbed?) {
        _embed.value = newEmbed
    }

    fun clearEmbed() {
        _embed.value = null
    }

    fun fetchOgImage(url: String) {
        if (isFetchingOgImage || _embed.value != null) return

        isFetchingOgImage = true
        viewModelScope.launch {
            try {
                val embedResult = repo.fetchOgImageEmbed(url)
                _embed.value = embedResult
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isFetchingOgImage = false
            }
        }
    }
}
