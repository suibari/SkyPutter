package com.suibari.skyputter.data.model

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suibari.skyputter.ui.type.HasUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsViewerState
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

abstract class PaginatedListViewModel<T : HasUri> : ViewModel() {

    protected abstract val repo: BskyPostActionRepository

    protected val _items = mutableStateListOf<T>()
    val items: List<T> = _items

    protected val viewerStatusMap = mutableStateMapOf<String, FeedDefsViewerState?>()
    val viewerStatus: Map<String, FeedDefsViewerState?>
        get() = viewerStatusMap

    // ローディング状態を外部に公開
    protected val _isRefreshing = mutableStateOf(false)
    val isRefreshing: Boolean get() = _isRefreshing.value

    private val _isLoadingMore = mutableStateOf(false)
    val isLoadingMore: Boolean get() = _isLoadingMore.value

    protected var cursor: String? = null
    private var initialized = false

    // 並行実行制御用のMutex
    private val loadMutex = Mutex()
    private val actionMutex = Mutex()

    // 強制スクロール用のターゲットURI
    var targetUri: String? by mutableStateOf(null)

    abstract suspend fun fetchItems(limit: Int, cursor: String? = null): Pair<List<T>, String?>

    fun loadInitialItems(limit: Int = 25) {
        viewModelScope.launch {
            loadMutex.withLock {
                try {
                    Log.d("PaginatedViewModel", "loadInitialItems: start, limit=$limit")
                    _isRefreshing.value = true

                    withContext(Dispatchers.IO) {
                        val (newItems, newCursor) = fetchItems(limit)
                        Log.d("PaginatedViewModel", "loadInitialItems: fetched ${newItems.size} items")

                        withContext(Dispatchers.Main) {
                            _items.clear()
                            _items.addAll(newItems)
                            cursor = newCursor
                        }

                        updateViewerStatus(newItems)
                    }

                    Log.d("PaginatedViewModel", "loadInitialItems: completed, total items=${_items.size}")
                } catch (e: Exception) {
                    Log.e("PaginatedViewModel", "loadInitialItems: error", e)
                    // エラー状態を必要に応じて公開
                } finally {
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun loadInitialItemsIfNeeded(limit: Int = 25) {
        if (initialized) return
        initialized = true
        loadInitialItems(limit)
    }

    suspend fun loadMoreItems(limit: Int = 25) {
        if (_isLoadingMore.value || _isRefreshing.value || cursor == null) return

        loadMutex.withLock {
            try {
                _isLoadingMore.value = true
                Log.d("PaginatedViewModel", "loadMoreItems: start, cursor=$cursor")

                withContext(Dispatchers.IO) {
                    val (newItems, newCursor) = fetchItems(limit, cursor)
                    Log.d("PaginatedViewModel", "loadMoreItems: fetched ${newItems.size} items")

                    if (newItems.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            _items.addAll(newItems)
                            cursor = newCursor
                        }

                        updateViewerStatus(newItems)
                    }
                }

                Log.d("PaginatedViewModel", "loadMoreItems: completed, total items=${_items.size}")
            } catch (e: Exception) {
                Log.e("PaginatedViewModel", "loadMoreItems: error", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    suspend fun refreshItems(limit: Int = 25) {
        cursor = null
        loadInitialItems(limit)
    }

    protected suspend fun updateViewerStatus(newItems: List<T>) {
        try {
            withContext(Dispatchers.IO) {
                val uris = newItems.mapNotNull { it.uri }
                if (uris.isNotEmpty()) {
                    val map = repo.fetchViewerStatusMap(uris)

                    withContext(Dispatchers.Main) {
                        viewerStatusMap.putAll(map)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PaginatedViewModel", "updateViewerStatus: error", e)
        }
    }

    fun getViewer(uri: String): FeedDefsViewerState? = viewerStatusMap[uri]

    fun toggleLike(ref: RepoStrongRef) {
        val uri = ref.uri ?: return
        val current = viewerStatusMap[uri]
        val isLiked = current?.like != null

        viewModelScope.launch {
            actionMutex.withLock {
                try {
                    withContext(Dispatchers.IO) {
                        if (isLiked) {
                            current?.like?.let { likeUri ->
                                repo.unlikePost(likeUri)

                                withContext(Dispatchers.Main) {
                                    viewerStatusMap[uri] = FeedDefsViewerState().apply {
                                        repost = current.repost
                                        like = null
                                    }
                                }
                            }
                        } else {
                            val likeUri = repo.likePost(ref)

                            withContext(Dispatchers.Main) {
                                viewerStatusMap[uri] = FeedDefsViewerState().apply {
                                    repost = current?.repost
                                    like = likeUri
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PaginatedViewModel", "toggleLike: error", e)
                    // エラー時は元の状態に戻すなどの処理を実装可能
                }
            }
        }
    }

    fun toggleRepost(ref: RepoStrongRef) {
        val uri = ref.uri ?: return
        val current = viewerStatusMap[uri]
        val isReposted = current?.repost != null

        viewModelScope.launch {
            actionMutex.withLock {
                try {
                    withContext(Dispatchers.IO) {
                        if (isReposted) {
                            current?.repost?.let { repostUri ->
                                repo.unRepostPost(repostUri)

                                withContext(Dispatchers.Main) {
                                    viewerStatusMap[uri] = FeedDefsViewerState().apply {
                                        like = current.like
                                        repost = null
                                    }
                                }
                            }
                        } else {
                            val repostUri = repo.repostPost(ref)

                            withContext(Dispatchers.Main) {
                                viewerStatusMap[uri] = FeedDefsViewerState().apply {
                                    like = current?.like
                                    repost = repostUri
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PaginatedViewModel", "toggleRepost: error", e)
                }
            }
        }
    }

    suspend fun getRecord(ref: RepoStrongRef): FeedPost? {
        return repo.getRecord(ref)
    }
}