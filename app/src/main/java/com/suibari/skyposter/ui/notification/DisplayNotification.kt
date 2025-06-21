package com.suibari.skyposter.ui.notification

import com.suibari.skyposter.ui.post.HasUri
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost
import work.socialhub.kbsky.model.app.bsky.notification.NotificationListNotificationsNotification
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

data class DisplayNotification(
    val raw: NotificationListNotificationsNotification,
    val isNew: Boolean,
    val parentPost: FeedPost? = null,
    val parentPostRecord: RepoStrongRef? = null,
    val rootPostRecord: RepoStrongRef? = null,
    val likeUri: String? = null,
    val repostUri: String? = null,
) : HasUri {
    override val uri: String?
        get() = raw.uri
}
