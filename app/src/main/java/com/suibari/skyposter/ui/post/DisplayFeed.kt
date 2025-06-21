package com.suibari.skyposter.ui.post

import work.socialhub.kbsky.model.app.bsky.feed.FeedDefsFeedViewPost

interface HasUri {
    val uri: String?
}

data class DisplayFeed(
    val raw: FeedDefsFeedViewPost,
    val likeUri: String? = null,
    val repostUri: String? = null,
) : HasUri {
    override val uri: String? get() = raw.post.uri
}
