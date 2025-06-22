package com.suibari.skyputter.ui.post

import com.suibari.skyputter.data.model.PaginatedListViewModel
import com.suibari.skyputter.data.repository.UserPostRepository
import com.suibari.skyputter.ui.type.DisplayFeed

class UserPostViewModel (
    override val repo: UserPostRepository
) : PaginatedListViewModel<DisplayFeed>() {

    override suspend fun fetchItems(limit: Int, cursor: String?): Pair<List<DisplayFeed>, String?> {
        return repo.fetchUserPosts(limit, cursor)
    }
}
