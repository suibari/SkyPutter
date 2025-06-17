package com.example.skyposter.data.repository

import android.util.Log
import com.example.skyposter.util.SessionManager
import work.socialhub.kbsky.ATProtocolException
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetFeedRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetPostsRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedLikeRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedRepostRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsFeedViewPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

data class DisplayFeed(
    val raw: FeedDefsFeedViewPost,
    val isLiked: Boolean? = false,
    val isReposted: Boolean? = false,
)

class LikesBackRepository () {
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
            val responsePosts = SessionManager.runWithAuthRetry { auth ->
                BlueskyFactory
                    .instance(BSKY_SOCIAL.uri)
                    .feed()
                    .getPosts(FeedGetPostsRequest(auth).also {
                        it.uris = uris
                    })
            }
            val viewerStatusMap = responsePosts.data.posts.associateBy { it.uri }

            // DisplayFeed に変換
            val displayFeeds = feeds.map { feed ->
                val viewer = viewerStatusMap[feed.post.uri]?.viewer
                DisplayFeed(
                    raw = feed,
                    isLiked = viewer?.like != null,
                    isReposted = viewer?.repost != null
                )
            }

            Pair(displayFeeds, newCursor)
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