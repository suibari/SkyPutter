package com.example.skyposter

import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsFeedViewPost

class UserPostViewModel (
    private val repo: UserPostRepository
) : PaginatedListViewModel<FeedDefsFeedViewPost>() {

    override suspend fun fetchItems(limit: Int, cursor: String?): Pair<List<FeedDefsFeedViewPost>, String?> {
        return repo.fetchUserPosts(limit, cursor)
    }
}
