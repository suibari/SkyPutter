package com.suibari.skyposter.ui.post

import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsFeedViewPost

data class DisplayFeed(
    val raw: FeedDefsFeedViewPost,
    val isLiked: Boolean? = false,
    val isReposted: Boolean? = false,
    val likeUri: String? = null,
    val repostUri: String? = null,
)
