package com.example.skyposter

import android.util.Log
import work.socialhub.kbsky.ATProtocolException
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetAuthorFeedRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsFeedViewPost

class UserPostRepository () {
    suspend fun fetchUserPosts (limit: Int, cursor: String?): Pair<List<FeedDefsFeedViewPost>, String?> {
        return try {
            val did = SessionManager.getSession().did

            val response = SessionManager.runWithAuthRetry { auth ->
                BlueskyFactory
                    .instance(BSKY_SOCIAL.uri)
                    .feed()
                    .getAuthorFeed(
                        FeedGetAuthorFeedRequest(auth).also {
                            it.actor = did!!
                            it.limit = limit
                            it.cursor = cursor
                        }
                    )
            }
            val feeds = response.data.feed
            val newCursor = response.data.cursor

            Pair(feeds, newCursor)
        } catch (e: ATProtocolException) {
            Log.e("UserPostRepository", "fetch error", e)
            Pair(emptyList(), null)
        }
    }
}