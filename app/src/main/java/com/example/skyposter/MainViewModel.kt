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

class MainViewModel(
    sessionManager: SessionManager
) : ViewModel() {
    private var _auth: AuthProvider? = null
    private var _profile = mutableStateOf<ActorDefsProfileViewDetailed?>(null)

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
            val response = BlueskyFactory
                .instance(BSKY_SOCIAL.uri)
                .feed()
                .post(FeedPostRequest(_auth!!).also {
                    it.text = postText
                })
        }
    }
}