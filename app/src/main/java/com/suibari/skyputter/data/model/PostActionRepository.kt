package com.suibari.skyputter.data.model

import work.socialhub.kbsky.model.com.atproto.repo.RepoStrongRef

interface PostActionRepository {
    suspend fun likePost(ref: RepoStrongRef): String
    suspend fun unlikePost(uri: String)
    suspend fun repostPost(ref: RepoStrongRef) : String
    suspend fun unRepostPost(uri: String)
}
