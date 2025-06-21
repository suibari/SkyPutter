package com.suibari.skyposter.data.model

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suibari.skyposter.ui.type.HasUri
import kotlinx.coroutines.launch
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsViewerState
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

abstract class PaginatedListViewModel<T : HasUri> :
    ViewModel() {

    protected abstract val repo: BskyPostActionRepository
    protected val _items = mutableStateListOf<T>()
    val items: List<T> = _items

    protected val viewerStatusMap = mutableStateMapOf<String, FeedDefsViewerState?>()
    val viewerStatus: Map<String, FeedDefsViewerState?>
        get() = viewerStatusMap

    protected var cursor: String? = null
    protected var isLoading = false
    private var initialized = false

    abstract suspend fun fetchItems(limit: Int, cursor: String? = null): Pair<List<T>, String?>

    fun loadInitialItems(limit: Int = 25) {
        viewModelScope.launch {
            Log.d("PaginatedViewModel", "loadInitialItems: start, limit=$limit")
            isLoading = true
            val (newItems, newCursor) = fetchItems(limit)
            Log.d("PaginatedViewModel", "loadInitialItems: start, fetched =${newItems.size}, cursor = $cursor")
            _items.clear()
            _items.addAll(newItems)
            Log.d("PaginatedViewModel", "loadInitialItems: addAll, _items =${_items.size}")
            updateViewerStatus(newItems)
            Log.d("PaginatedViewModel", "loadInitialItems: updateViewerStatus")
            cursor = newCursor
            isLoading = false
        }
    }

    fun loadInitialItemsIfNeeded(limit: Int = 25) {
        if (initialized) return
        initialized = true
        loadInitialItems(limit)
    }

    fun loadMoreItems(limit: Int = 25) {
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
