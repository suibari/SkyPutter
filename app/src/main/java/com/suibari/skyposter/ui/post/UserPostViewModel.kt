package com.suibari.skyposter.ui.post

import com.suibari.skyposter.data.model.PaginatedListViewModel
import com.suibari.skyposter.data.repository.UserPostRepository
import com.suibari.skyposter.ui.type.DisplayFeed

class UserPostViewModel (
    override val repo: UserPostRepository
) : PaginatedListViewModel<DisplayFeed>() {

    override suspend fun fetchItems(limit: Int, cursor: String?): Pair<List<DisplayFeed>, String?> {
        return repo.fetchUserPosts(limit, cursor)
    }
}
