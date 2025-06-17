package com.example.skyposter

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
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

data class AttachedEmbed(
    var title: String,
    var uri: Uri,
    var blob: ByteArray? = null,
    var contentType: String? = null,
    var aspectRatio: EmbedDefsAspectRatio? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttachedEmbed

        if (blob != null) {
            if (other.blob == null) return false
            if (!blob.contentEquals(other.blob)) return false
        } else if (other.blob != null) return false

        return true
    }

    override fun hashCode(): Int {
        return blob?.contentHashCode() ?: 0
    }
}

class MainViewModel() : ViewModel() {
    private var _profile = mutableStateOf<ActorDefsProfileViewDetailed?>(null)
    var parentPostRecord by mutableStateOf<RepoStrongRef?>(null)
    var parentPost by mutableStateOf<FeedPost?>(null)
    private var rootPostRecord by mutableStateOf<RepoStrongRef?>(null)

    init {
        viewModelScope.launch {
            val response = SessionManager.runWithAuthRetry { auth ->
                BlueskyFactory
                    .instance(BSKY_SOCIAL.uri)
                    .actor()
                    .getProfile(ActorGetProfileRequest(auth).also {
                        it.actor = auth.did
                    })
            }
            _profile.value = response.data
        }
    }

    fun getProfile(): ActorDefsProfileViewDetailed? {
        return _profile.value
    }

    fun post(postText: String, embed: AttachedEmbed?) {
        viewModelScope.launch {
            // 画像添付準備
            var images: EmbedImages? = null
            if (embed?.blob != null && embed.contentType != null && embed.aspectRatio != null) {
                SessionManager.runWithAuthRetry { auth ->
                    val response = BlueskyFactory
                        .instance(BSKY_SOCIAL.uri)
                        .repo()
                        .uploadBlob(RepoUploadBlobRequest(
                            auth = auth,
                            bytes = embed.blob!!,
                            name = embed.title,
                            contentType = embed.contentType!!
                            ))

                    val blobRef = response.data.blob

                    images = EmbedImages().also { it ->
                        it.images = listOf(EmbedImagesImage().also {
                            it.image = blobRef
                            it.alt = "image from SkyPoster"
                            it.aspectRatio = embed.aspectRatio
                        })
                    }
                }
            }

            // ポスト処理
            if (parentPostRecord != null) {
                // リプライ投稿
                val reply = FeedPostReplyRef().also {
                    it.root = rootPostRecord
                    it.parent = parentPostRecord
                }
                SessionManager.runWithAuthRetry { auth ->
                    BlueskyFactory
                        .instance(BSKY_SOCIAL.uri)
                        .feed()
                        .post(FeedPostRequest(auth).also {
                            it.text = postText
                            it.reply = reply
                            it.embed = images
                        })
                }
            } else {
                // 通常投稿
                SessionManager.runWithAuthRetry { auth ->
                    BlueskyFactory
                        .instance(BSKY_SOCIAL.uri)
                        .feed()
                        .post(FeedPostRequest(auth).also {
                            it.text = postText
                            it.embed = images
                        })
                }
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
}