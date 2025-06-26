package com.suibari.skyputter.data.model

import android.util.Log
import com.suibari.skyputter.util.BskyUtil
import com.suibari.skyputter.util.SessionManager
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedDeleteLikeRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedDeleteRepostRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetPostsRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedLikeRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedPostRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedRepostRequest
import work.socialhub.kbsky.api.entity.com.atproto.repo.RepoGetRecordRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsViewerState
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

open class BskyPostActionRepository : PostActionRepository {

    // レコードキャッシュを共通化
    private val recordCache = mutableMapOf<String, FeedPost>()

    /**
     * limitは25件以下でないとAPIエラーとなる!!
     * */
    suspend fun fetchViewerStatusMap(uris: List<String>): Map<String, FeedDefsViewerState?> {
        if (uris.isNotEmpty()) {
            return try {
                val response = SessionManager.runWithAuthRetry { auth ->
                    BlueskyFactory
                        .instance(BSKY_SOCIAL.uri)
                        .feed()
                        .getPosts(FeedGetPostsRequest(auth).also {
                            it.uris = uris
                        })
                }
                response.data.posts.associate { post -> post.uri!! to post.viewer }
            } catch (e: Exception) {
                Log.e("BskyPostActionRepo", "Failed to fetch viewer status", e)
                emptyMap()
            }
        } else {
            return emptyMap()
        }
    }

    /**
     * レコードを取得（キャッシュ機能付き）
     */
    suspend fun getRecord(refRecord: RepoStrongRef?): FeedPost? {
        return try {
            refRecord?.let { ref ->
                val uri = ref.uri
                recordCache[uri] ?: run {
                    val (repo, collection, rkey) = BskyUtil.parseAtUri(uri)
                        ?: return@let null
                    val record = SessionManager.runWithAuthRetry { auth ->
                        BlueskyFactory
                            .instance(BSKY_SOCIAL.uri)
                            .repo()
                            .getRecord(
                                RepoGetRecordRequest(repo, collection, rkey)
                            )
                    }
                    val feedPost = record.data.value.asFeedPost
                    feedPost?.also { recordCache[uri] = it }
                }
            }
        } catch (e: Exception) {
            Log.w("getRecord", "Record not found", e)
            return null
        }
    }

    /**
     * レコードキャッシュをクリア
     */
    protected fun clearRecordCache() {
        recordCache.clear()
    }

    /**
     * 特定のURIのレコードキャッシュを削除
     */
    protected fun removeFromRecordCache(uri: String) {
        recordCache.remove(uri)
    }

    /**
     * レコードキャッシュのサイズを取得（デバッグ用）
     */
    protected fun getRecordCacheSize(): Int {
        return recordCache.size
    }

    override suspend fun likePost(ref: RepoStrongRef): String {
        val response = SessionManager.runWithAuthRetry { auth ->
            BlueskyFactory
                .instance(BSKY_SOCIAL.uri)
                .feed()
                .like(FeedLikeRequest(auth).apply { subject = ref })
        }
        return response.data.uri
    }

    override suspend fun unlikePost(uri: String) {
        val rkey = BskyUtil.parseAtUri(uri)?.third
        SessionManager.runWithAuthRetry { auth ->
            BlueskyFactory
                .instance(BSKY_SOCIAL.uri)
                .feed()
                .deleteLike(FeedDeleteLikeRequest(auth).also {
                    it.uri = uri
                    it.rkey = rkey
                })
        }
    }

    override suspend fun repostPost(ref: RepoStrongRef): String {
        val response = SessionManager.runWithAuthRetry { auth ->
            BlueskyFactory
                .instance(BSKY_SOCIAL.uri)
                .feed()
                .repost(FeedRepostRequest(auth).apply { subject = ref })
        }
        return response.data.uri
    }

    override suspend fun unRepostPost(uri: String) {
        val rkey = BskyUtil.parseAtUri(uri)?.third
        SessionManager.runWithAuthRetry { auth ->
            BlueskyFactory
                .instance(BSKY_SOCIAL.uri)
                .feed()
                .deleteRepost(FeedDeleteRepostRequest(auth).also {
                    it.uri = uri
                    it.rkey = rkey
                })
        }
    }
}