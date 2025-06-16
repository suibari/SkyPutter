package com.example.skyposter

import android.content.Context
import android.util.Log
import work.socialhub.kbsky.ATProtocolException
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetAuthorFeedRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsFeedViewPost

class UserPostRepository (
    private val sessionManager: SessionManager,
    val context: Context
) {
    suspend fun fetchUserPosts (limit: Int, cursor: String?): Pair<List<FeedDefsFeedViewPost>, String?> {
        return try {
            val auth = sessionManager.getAuth()
            val did = sessionManager.getSession().did

            val response = BlueskyFactory
                .instance(BSKY_SOCIAL.uri)
                .feed()
                .getAuthorFeed(
                    FeedGetAuthorFeedRequest(auth!!).also {
                        it.actor = did!!
                        it.limit = limit
                        it.cursor = cursor
                    }
                )
            val feeds = response.data.feed
            val newCursor = response.data.cursor

            Pair(feeds, newCursor)
        } catch (e: ATProtocolException) {
            Log.e("UserPostRepository", "fetch error", e)
            Pair(emptyList(), null)
        }
    }
}