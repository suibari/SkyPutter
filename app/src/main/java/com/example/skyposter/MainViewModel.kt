import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skyposter.SessionManager
import kotlinx.coroutines.launch
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.actor.ActorGetProfileRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedPostRequest
import work.socialhub.kbsky.auth.AuthProvider
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewDetailed
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.app.bsky.feed.FeedPostReplyRef
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

class MainViewModel(
    sessionManager: SessionManager
) : ViewModel() {
    private var _auth: AuthProvider? = null
    private var _profile = mutableStateOf<ActorDefsProfileViewDetailed?>(null)
    var parentPostRecord by mutableStateOf<RepoStrongRef?>(null)
    var parentPost by mutableStateOf<FeedPost?>(null)
    private var rootPostRecord by mutableStateOf<RepoStrongRef?>(null)

    init {
        viewModelScope.launch {
            _auth = sessionManager.getAuth() ?: return@launch
            val response = BlueskyFactory
                .instance(BSKY_SOCIAL.uri)
                .actor()
                .getProfile(ActorGetProfileRequest(_auth!!).also {
                    it.actor = _auth!!.did
                })
            _profile.value = response.data
        }
    }

    fun getProfile(): ActorDefsProfileViewDetailed? {
        return _profile.value
    }

    fun post(postText: String) {
        viewModelScope.launch {
            if (parentPostRecord != null) {
                // リプライ投稿
                val reply = FeedPostReplyRef().also {
                    it.root = rootPostRecord
                    it.parent = parentPostRecord
                }
                BlueskyFactory
                    .instance(BSKY_SOCIAL.uri)
                    .feed()
                    .post(FeedPostRequest(_auth!!).also {
                        it.text = postText
                        it.reply = reply
                    })
            } else {
                // 通常投稿
                BlueskyFactory
                    .instance(BSKY_SOCIAL.uri)
                    .feed()
                    .post(FeedPostRequest(_auth!!).also {
                        it.text = postText
                    })
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