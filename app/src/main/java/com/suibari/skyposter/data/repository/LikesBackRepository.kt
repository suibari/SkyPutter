package com.suibari.skyposter.data.repository

import android.util.Log
import com.suibari.skyposter.ui.post.DisplayFeed
import com.suibari.skyposter.util.SessionManager
import work.socialhub.kbsky.ATProtocolException
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetFeedRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetPostsRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedLikeRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedRepostRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsFeedViewPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

class LikesBackRepository: BskyPostActionRepository () {
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

            // いいねリポスト状態取得
            val uris = feeds.map { feed -> feed.post.uri!! }
            val viewerStatusMap = fetchViewerStatusMap(uris)

            val displayFeeds = feeds.map { feed ->
                val viewer = viewerStatusMap[feed.post.uri]
                DisplayFeed(
                    raw = feed,
                    likeUri = viewer?.like,
                    repostUri = viewer?.repost,
                )
            }

            Pair(displayFeeds, newCursor)
        } catch (e: ATProtocolException) {
            Log.e("LikesBackRepository", "fetch error", e)
            Pair(emptyList(), null)
        }
    }
}