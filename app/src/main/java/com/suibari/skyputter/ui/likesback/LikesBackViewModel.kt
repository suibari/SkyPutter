package com.suibari.skyputter.ui.likesback

import com.suibari.skyputter.data.model.PaginatedListViewModel
import com.suibari.skyputter.data.repository.LikesBackRepository
import com.suibari.skyputter.ui.type.DisplayFeed

class LikesBackViewModel(
    override val repo: LikesBackRepository
) : PaginatedListViewModel<DisplayFeed>() {

    override suspend fun fetchItems(limit: Int, cursor: String?): Pair<List<DisplayFeed>, String?> {
        return repo.fetchLikesBack(limit, cursor)
    }
}
