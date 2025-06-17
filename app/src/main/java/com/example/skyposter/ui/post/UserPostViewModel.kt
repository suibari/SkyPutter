package com.example.skyposter.ui.post

import com.example.skyposter.data.model.PaginatedListViewModel
import com.example.skyposter.data.repository.DisplayFeed
import com.example.skyposter.data.repository.UserPostRepository

class UserPostViewModel (
    private val repo: UserPostRepository
) : PaginatedListViewModel<DisplayFeed>() {

    override suspend fun fetchItems(limit: Int, cursor: String?): Pair<List<DisplayFeed>, String?> {
        return repo.fetchUserPosts(limit, cursor)
    }
}
