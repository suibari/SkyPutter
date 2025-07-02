package com.suibari.skyputter.data.repository

import android.util.Log
import com.suibari.skyputter.data.model.BskyPostActionRepository
import com.suibari.skyputter.ui.type.DisplayFeed
import com.suibari.skyputter.util.SessionManager
import work.socialhub.kbsky.ATProtocolException
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedDeletePostRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetAuthorFeedRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL

class UserPostRepository: BskyPostActionRepository() {
    suspend fun fetchUserPosts (limit: Int, cursor: String?): Pair<List<DisplayFeed>, String?> {
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
                            it.filter = FeedGetAuthorFeedRequest.Filter.PostsWithReplies
                            it.cursor = cursor
                        }
                    )
            }
            val feeds = response.data.feed
                .filter { feed -> feed.reason == null } // リポストを除外
            val newCursor = response.data.cursor

            val result = feeds.map { feed ->
                DisplayFeed(
                    raw = feed,
                )
            }

            Pair(result, newCursor)
        } catch (e: ATProtocolException) {
            Log.e("UserPostRepository", "fetch error", e)
            Pair(emptyList(), null)
        }
    }

    suspend fun deletePost(uri: String) {
        SessionManager.runWithAuthRetry { auth ->
            BlueskyFactory.instance(BSKY_SOCIAL.uri)
                .feed()
                .deletePost(
                    FeedDeletePostRequest(auth).apply {
                        this.uri = uri
                    }
                )
        }
    }
}