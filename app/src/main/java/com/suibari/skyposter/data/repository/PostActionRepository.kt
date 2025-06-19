package com.suibari.skyposter.data.repository

import work.socialhub.kbsky.api.entity.share.RKeyRequest
import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

interface PostActionRepository {
    suspend fun likePost(ref: RepoStrongRef)
    suspend fun unlikePost(ref: RepoStrongRef)
    suspend fun repostPost(ref: RepoStrongRef)
    suspend fun unRepostPost(ref: RepoStrongRef)
}
