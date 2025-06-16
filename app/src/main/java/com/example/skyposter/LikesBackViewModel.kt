package com.example.skyposter

import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsFeedViewPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

class LikesBackViewModel (
    private val repo: LikesBackRepository
) : PaginatedListViewModel<DisplayFeed>() {

    override suspend fun fetchItems(limit: Int, cursor: String?): Pair<List<DisplayFeed>, String?> {
        return repo.fetchLikesBack(limit, cursor)
    }

    suspend fun likePost(record: RepoStrongRef) {
        repo.likePost(record)

        val index = _items.indexOfFirst { it.raw.post.uri == record.uri }
        if (index != -1) {
            val item = _items[index]
            _items[index] = item.copy(isLiked = !(item.isLiked ?: false))
        }
    }

    suspend fun repostPost(record: RepoStrongRef) {
        repo.repostPost(record)

        val index = _items.indexOfFirst { it.raw.post.uri == record.uri }
        if (index != -1) {
            val item = _items[index]
            _items[index] = item.copy(isReposted = !(item.isReposted ?: false))
        }
    }
}
