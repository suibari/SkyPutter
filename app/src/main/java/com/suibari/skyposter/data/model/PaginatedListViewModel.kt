package com.suibari.skyposter.data.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suibari.skyposter.data.repository.BskyPostActionRepository
import com.suibari.skyposter.data.repository.PostActionRepository
import com.suibari.skyposter.ui.post.HasUri
import kotlinx.coroutines.launch
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsViewerState
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

interface ViewerStatusProvider {
    val viewerStatus: Map<String, FeedDefsViewerState?>
}

abstract class PaginatedListViewModel<T : HasUri> :
    ViewModel(), ViewerStatusProvider {

    protected abstract val repo: BskyPostActionRepository
    protected val _items = mutableStateListOf<T>()
    val items: List<T> = _items

    protected val viewerStatusMap = mutableStateMapOf<String, FeedDefsViewerState?>()
    override val viewerStatus: Map<String, FeedDefsViewerState?>
        get() = viewerStatusMap

    protected var cursor: String? = null
    protected var isLoading = false
    private var initialized = false

    abstract suspend fun fetchItems(limit: Int, cursor: String? = null): Pair<List<T>, String?>

    fun loadInitialItems(limit: Int = 10) {
        viewModelScope.launch {
            isLoading = true
            val (newItems, newCursor) = fetchItems(limit)
            _items.clear()
            _items.addAll(newItems)
            updateViewerStatus(newItems)
            cursor = newCursor
            isLoading = false
        }
    }

    fun loadInitialItemsIfNeeded(limit: Int = 10) {
        if (initialized) return
        initialized = true
        loadInitialItems(limit)
    }

    fun loadMoreItems(limit: Int = 10) {
        if (isLoading || cursor == null) return
        viewModelScope.launch {
            isLoading = true
            val (newItems, newCursor) = fetchItems(limit, cursor)
            _items.addAll(newItems)
            updateViewerStatus(newItems)
            cursor = newCursor
            isLoading = false
        }
    }

    protected suspend fun updateViewerStatus(newItems: List<T>) {
        val uris = newItems.mapNotNull { it.uri }
        val map = repo.fetchViewerStatusMap(uris)
        viewerStatusMap.putAll(map)
    }

    fun getViewer(uri: String): FeedDefsViewerState? = viewerStatusMap[uri]

    fun toggleLike(ref: RepoStrongRef) {
        val uri = ref.uri ?: return
        val current = viewerStatusMap[uri]
        val isLiked = current?.like != null

        viewModelScope.launch {
            if (isLiked) {
                repo.unlikePost(viewerStatusMap[uri]?.like!!)
                viewerStatusMap[uri] = FeedDefsViewerState().apply {
                    repost = current?.repost
                    like = null
                }
            } else {
                val likeUri = repo.likePost(ref)
                viewerStatusMap[uri] = FeedDefsViewerState().apply {
                    repost = current?.repost
                    like = likeUri
                }
            }
        }
    }

    fun toggleRepost(ref: RepoStrongRef) {
        val uri = ref.uri ?: return
        val current = viewerStatusMap[uri]
        val isReposted = current?.repost != null

        viewModelScope.launch {
            if (isReposted) {
                repo.unRepostPost(viewerStatusMap[uri]?.repost!!)
                viewerStatusMap[uri] = FeedDefsViewerState().apply {
                    like = current?.like
                    repost = null
                }
            } else {
                val repostUri = repo.repostPost(ref)
                viewerStatusMap[uri] = FeedDefsViewerState().apply {
                    like = current?.like
                    repost = repostUri
                }
            }
        }
    }

}
