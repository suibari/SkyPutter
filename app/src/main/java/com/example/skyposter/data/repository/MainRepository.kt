package com.example.skyposter.data.repository

import com.example.skyposter.util.SessionManager
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.actor.ActorGetProfileRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewDetailed

class MainRepository {
    suspend fun getProfile(): ActorDefsProfileViewDetailed {
        return SessionManager.runWithAuthRetry { auth ->
            BlueskyFactory
                .instance(BSKY_SOCIAL.uri)
                .actor()
                .getProfile(
                    ActorGetProfileRequest(auth).also { it.actor = auth.did }
                ).data
        }
    }
}
