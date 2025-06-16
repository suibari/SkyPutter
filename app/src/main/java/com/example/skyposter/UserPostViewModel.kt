package com.example.skyposter

import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsPostView

class UserPostViewModel constructor(
    private val repo: UserPostRepository
) : PaginatedListViewModel<FeedDefsPostView>() {

    override suspend fun fetchItems(limit: Int, cursor: String?): Pair<List<FeedDefsPostView>, String?> {
        return repo.fetchUserPosts(limit, cursor)
    }
}
