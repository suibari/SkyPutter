package com.suibari.skyposter.ui.likesback

import com.suibari.skyposter.data.model.PaginatedListViewModel
import com.suibari.skyposter.data.repository.LikesBackRepository
import com.suibari.skyposter.ui.post.DisplayFeed
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

class LikesBackViewModel (
    override val repo: LikesBackRepository
) : PaginatedListViewModel<DisplayFeed>() {

    override suspend fun fetchItems(limit: Int, cursor: String?): Pair<List<DisplayFeed>, String?> {
        return repo.fetchLikesBack(limit, cursor)
    }
}
