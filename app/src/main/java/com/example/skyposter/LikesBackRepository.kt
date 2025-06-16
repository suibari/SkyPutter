package com.example.skyposter

import android.content.Context
import android.util.Log
import work.socialhub.kbsky.ATProtocolException
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetFeedRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedLikeRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedRepostRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsFeedViewPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

class LikesBackRepository () {
    suspend fun fetchLikesBack (limit: Int, cursor: String?): Pair<List<FeedDefsFeedViewPost>, String?> {
        return try {
            val response = SessionManager.runWithAuthRetry { auth ->
                BlueskyFactory
                    .instance(BSKY_SOCIAL.uri)
                    .feed()
                    .getFeed(FeedGetFeedRequest(auth).also {
                        it.feed =
                            "at://did:plc:uixgxpiqf4i63p6rgpu7ytmx/app.bsky.feed.generator/likesBack"
                        it.limit = limit
                        it.cursor = cursor
                    })
            }

            val feeds = response.data.feed
            val newCursor = response.data.cursor

            Pair(feeds, newCursor)
        } catch (e: ATProtocolException) {
            Log.e("LikesBackRepository", "fetch error", e)
            Pair(emptyList(), null)
        }
    }

    suspend fun likePost(record: RepoStrongRef) {
        SessionManager.runWithAuthRetry { auth ->
            BlueskyFactory
                .instance(BSKY_SOCIAL.uri)
                .feed()
                .like(FeedLikeRequest(auth).also { it.subject = record })
        }
    }

    suspend fun repostPost(record: RepoStrongRef) {
        SessionManager.runWithAuthRetry { auth ->
            BlueskyFactory
                .instance(BSKY_SOCIAL.uri)
                .feed()
                .repost(FeedRepostRequest(auth).also { it.subject = record })
        }
    }
}