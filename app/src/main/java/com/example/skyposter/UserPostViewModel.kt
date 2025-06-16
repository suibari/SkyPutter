package com.example.skyposter

import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsFeedViewPost

class UserPostViewModel (
    private val repo: UserPostRepository
) : PaginatedListViewModel<DisplayFeed>() {

    override suspend fun fetchItems(limit: Int, cursor: String?): Pair<List<DisplayFeed>, String?> {
        return repo.fetchUserPosts(limit, cursor)
    }
}
