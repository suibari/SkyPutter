package com.example.skyposter

import android.content.Context
import android.util.Log
import work.socialhub.kbsky.ATProtocolException
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetAuthorFeedRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetFeedRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsPostView

class LikesBackRepository (
    private val sessionManager: SessionManager,
    val context: Context
) {
    suspend fun fetchLikesBack (limit: Int, cursor: String?): Pair<List<FeedDefsPostView>, String?> {
        return try {
            val auth = sessionManager.getAuth()
            val did = sessionManager.getSession().did

            val response = BlueskyFactory
                .instance(BSKY_SOCIAL.uri)
                .feed()
                .getFeed(FeedGetFeedRequest(auth!!).also {
                    it.feed = "at://did:plc:uixgxpiqf4i63p6rgpu7ytmx/app.bsky.feed.generator/likesBack"
                    it.limit = limit
                    it.cursor = cursor
                })

            val posts = response.data.feed.map { item -> item.post }
            val newCursor = response.data.cursor

            Pair(posts, newCursor)
        } catch (e: ATProtocolException) {
            Log.e("LikesBackRepository", "fetch error", e)
            Pair(emptyList(), null)
        }
    }
}