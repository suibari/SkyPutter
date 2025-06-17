package com.example.skyposter

import NotificationViewModel
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class AttachedEmbed(
    var title: String,
    var uriString: String? = null,
    var blob: ByteArray? = null,
    var contentType: String? = null,
    var aspectRatio: EmbedDefsAspectRatio? = null
) {
    val uri: Uri?
        get() = uriString?.toUri()

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

class MainViewModel(
    userPostViewModel: UserPostViewModel,
    notificationViewModel: NotificationViewModel,
    likesBackViewModel: LikesBackViewModel,
) : ViewModel() {
    private var _profile = mutableStateOf<ActorDefsProfileViewDetailed?>(null)
    var parentPostRecord by mutableStateOf<RepoStrongRef?>(null)
    var parentPost by mutableStateOf<FeedPost?>(null)
    private var rootPostRecord by mutableStateOf<RepoStrongRef?>(null)

    private val _embed = mutableStateOf<AttachedEmbed?>(null)
    val embed: MutableState<AttachedEmbed?> = _embed
    private var isFetchingOgImage = false

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

            userPostViewModel.loadInitialItemsIfNeeded()
            notificationViewModel.loadInitialItemsIfNeeded()
            notificationViewModel.startPolling()
            likesBackViewModel.loadInitialItemsIfNeeded()
        }
    }

    fun getProfile(): ActorDefsProfileViewDetailed? {
        return _profile.value
    }

    fun post(postText: String, embed: AttachedEmbed?) {
        viewModelScope.launch {
            // 画像添付準備
            var images: EmbedImages? = null
            if (embed?.blob != null && embed.contentType != null) {
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
                val doc = withContext(Dispatchers.IO) {
                    org.jsoup.Jsoup.connect(url).get()
                }
                val ogImageRaw = doc.select("meta[property=og:image]").attr("content")
                val ogTitle = doc.select("meta[property=og:title]").attr("content")
                val ogImage = URL(URL(url), ogImageRaw).toString()

                val result = getByteArrayFromUrl(ogImage)

                if (result != null) {
                    val (imageData, contentType) = result
                    val ext = extensionFromContentType(contentType)

                    _embed.value = AttachedEmbed(
                        title = "ogp.$ext",
                        uriString = ogImage,
                        blob = imageData,
                        contentType = contentType,
                        aspectRatio = null,
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isFetchingOgImage = false
            }
        }
    }

    private fun extensionFromContentType(contentType: String?): String {
        if (contentType != null) {
            return when {
                contentType.contains("jpeg") -> "jpg"
                contentType.contains("png") -> "png"
                contentType.contains("webp") -> "webp"
                contentType.contains("gif") -> "gif"
                else -> "bin" // fallback
            }
        }
        return "png"
    }

    private suspend fun getByteArrayFromUrl(url: String): Pair<ByteArray?, String?>? {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }
            if (response.isSuccessful) {
                val blob = response.body?.bytes()
                val contentType = response.body?.contentType()?.toString() ?: "image/jpeg"

                return Pair(blob, contentType)
            } else {
                return null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }
}
