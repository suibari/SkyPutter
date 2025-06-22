package com.suibari.skyputter.data.repository

import android.util.Log
import com.suibari.skyputter.data.model.BskyPostActionRepository
import com.suibari.skyputter.ui.type.DisplayFeed
import com.suibari.skyputter.util.SessionManager
import work.socialhub.kbsky.ATProtocolException
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetFeedRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost

class LikesBackRepository: BskyPostActionRepository() {
    suspend fun fetchLikesBack (limit: Int, cursor: String?): Pair<List<DisplayFeed>, String?> {
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

            val displayFeeds = feeds.map { feed ->
                DisplayFeed(
                     raw = feed,
                )
            }

            Pair(displayFeeds, newCursor)
        } catch (e: ATProtocolException) {
            Log.e("LikesBackRepository", "fetch error", e)
            Pair(emptyList(), null)
        }
    }
}