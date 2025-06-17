import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skyposter.BskyUtil
import com.example.skyposter.SessionManager
import kotlinx.coroutines.launch
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.actor.ActorGetProfileRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedPostRequest
import work.socialhub.kbsky.api.entity.com.atproto.repo.RepoUploadBlobRequest
import work.socialhub.kbsky.auth.AuthProvider
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewDetailed
import work.socialhub.kbsky.model.app.bsky.embed.EmbedDefsAspectRatio
import work.socialhub.kbsky.model.app.bsky.embed.EmbedImages
import work.socialhub.kbsky.model.app.bsky.embed.EmbedImagesImage
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.app.bsky.feed.FeedPostReplyRef
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef
import work.socialhub.kbsky.model.share.Blob

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

    fun post(postText: String, blob: ByteArray?, filename: String?, contentType: String?, aspectRatio: EmbedDefsAspectRatio?) {
        viewModelScope.launch {
            // 画像添付準備
            var images: EmbedImages? = null
            if (blob != null && filename != null && contentType != null && aspectRatio != null) {
                SessionManager.runWithAuthRetry { auth ->
                    val response = BlueskyFactory
                        .instance(BSKY_SOCIAL.uri)
                        .repo()
                        .uploadBlob(RepoUploadBlobRequest(
                            auth = auth,
                            bytes = blob,
                            name = filename,
                            contentType = contentType
                            ))

                    val blobRef = response.data.blob

                    images = EmbedImages().also { it ->
                        it.images = listOf(EmbedImagesImage().also {
                            it.image = blobRef
                            it.alt = "image from SkyPoster"
                            it.aspectRatio = aspectRatio
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