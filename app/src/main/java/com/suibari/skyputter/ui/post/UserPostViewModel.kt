package com.suibari.skyputter.ui.post

import androidx.lifecycle.viewModelScope
import com.suibari.skyputter.data.model.PaginatedListViewModel
import com.suibari.skyputter.data.repository.UserPostRepository
import com.suibari.skyputter.ui.type.DisplayFeed
import kotlinx.coroutines.launch

class UserPostViewModel (
    override val repo: UserPostRepository
) : PaginatedListViewModel<DisplayFeed>() {

    override suspend fun fetchItems(limit: Int, cursor: String?): Pair<List<DisplayFeed>, String?> {
        return repo.fetchUserPosts(limit, cursor)
    }

    fun deletePost(feed: DisplayFeed, onComplete: (Boolean) -> Unit) {
        val uri = feed.uri ?: run {
            onComplete(false)
            return
        }

        viewModelScope.launch {
            try {
                repo.deletePost(uri)
                // 削除成功
                _items.remove(feed)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }
}
