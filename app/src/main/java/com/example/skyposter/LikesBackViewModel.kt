package com.example.skyposter

import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsPostView

class LikesBackViewModel (
    private val repo: LikesBackRepository
) : PaginatedListViewModel<FeedDefsPostView>() {

    override suspend fun fetchItems(limit: Int, cursor: String?): Pair<List<FeedDefsPostView>, String?> {
        return repo.fetchLikesBack(limit, cursor)
    }
}
