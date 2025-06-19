package com.suibari.skyposter.ui.notification

import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.app.bsky.notification.NotificationListNotificationsNotification
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

data class DisplayNotification(
    val raw: NotificationListNotificationsNotification,
    val isNew: Boolean,
    val parentPost: FeedPost? = null,
    val parentPostRecord: RepoStrongRef? = null,
    val rootPostRecord: RepoStrongRef? = null,
    val isLiked: Boolean = false,
    val isReposted: Boolean = false,
    val likeUri: String? = null,
    val repostUri: String? = null,
)
