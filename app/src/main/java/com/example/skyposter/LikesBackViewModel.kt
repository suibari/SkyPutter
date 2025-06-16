package com.example.skyposter

import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsFeedViewPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

class LikesBackViewModel (
    private val repo: LikesBackRepository
) : PaginatedListViewModel<FeedDefsFeedViewPost>() {

    override suspend fun fetchItems(limit: Int, cursor: String?): Pair<List<FeedDefsFeedViewPost>, String?> {
        return repo.fetchLikesBack(limit, cursor)
    }

    suspend fun likePost(record: RepoStrongRef) {
        repo.likePost(record)
    }

    suspend fun repostPost(record: RepoStrongRef) {
        repo.repostPost(record)
    }
}
