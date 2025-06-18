package com.suibari.skyposter.ui.post

import com.suibari.skyposter.data.model.PaginatedListViewModel
import com.suibari.skyposter.data.repository.DisplayFeed
import com.suibari.skyposter.data.repository.UserPostRepository

class UserPostViewModel (
    private val repo: UserPostRepository
) : PaginatedListViewModel<DisplayFeed>() {

    override suspend fun fetchItems(limit: Int, cursor: String?): Pair<List<DisplayFeed>, String?> {
        return repo.fetchUserPosts(limit, cursor)
    }
}
