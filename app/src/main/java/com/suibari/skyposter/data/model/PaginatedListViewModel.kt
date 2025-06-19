package com.suibari.skyposter.data.model

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suibari.skyposter.data.repository.PostActionRepository
import kotlinx.coroutines.launch
import work.socialhub.kbsky.api.entity.share.RKeyRequest
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

abstract class PaginatedListViewModel<T> : ViewModel() {
    protected abstract val repo: PostActionRepository
    protected val _items = mutableStateListOf<T>()
    val items: List<T> = _items
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
            cursor = newCursor
            isLoading = false
        }
    }

    suspend fun toggleLike(ref: RepoStrongRef, isLiked: Boolean) {
        if (isLiked) {
            repo.unlikePost(ref)
        } else {
            repo.likePost(ref)
        }
    }

    suspend fun toggleRepost(ref: RepoStrongRef, isReposted: Boolean) {
        if (isReposted) {
            repo.unRepostPost(ref)
        } else {
            repo.repostPost(ref)
        }
    }
}
