package com.example.skyposter

import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.com.atproto.server.ServerCreateSessionRequest
import work.socialhub.kbsky.auth.BearerTokenAuthProvider
import work.socialhub.kbsky.domain.Service

object BskyClient {
    private var accessJwt: String = ""
    private lateinit var auth: BearerTokenAuthProvider

    fun login(did: String, password: String) {
        try {
            val response = BlueskyFactory
                .instance(Service.BSKY_SOCIAL.uri)
                .server()
                .createSession(
                    ServerCreateSessionRequest().also {
                        it.identifier = did
                        it.password = password
                    }
                )

            accessJwt = response.data.accessJwt
            auth = BearerTokenAuthProvider(accessJwt)
        } catch (e: Exception) {
            throw e
        }
    }

    fun getAuth(): BearerTokenAuthProvider {
        return auth
    }
}