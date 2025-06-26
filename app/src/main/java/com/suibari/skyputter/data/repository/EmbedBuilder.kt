package com.suibari.skyputter.data.repository

import android.util.Log
import com.suibari.skyputter.ui.main.AttachedEmbed
import com.suibari.skyputter.util.SessionManager
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.BlueskyTypes
import work.socialhub.kbsky.api.entity.app.bsky.video.VideoGetJobStatusRequest
import work.socialhub.kbsky.api.entity.app.bsky.video.VideoGetUploadLimitsRequest
import work.socialhub.kbsky.api.entity.app.bsky.video.VideoUploadVideoRequest
import work.socialhub.kbsky.api.entity.com.atproto.repo.RepoUploadBlobRequest
import work.socialhub.kbsky.api.entity.com.atproto.server.ServerGetServiceAuthRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.embed.EmbedDefsAspectRatio
import work.socialhub.kbsky.model.app.bsky.embed.EmbedExternal
import work.socialhub.kbsky.model.app.bsky.embed.EmbedExternalExternal
import work.socialhub.kbsky.model.app.bsky.embed.EmbedImages
import work.socialhub.kbsky.model.app.bsky.embed.EmbedImagesImage
import work.socialhub.kbsky.model.app.bsky.embed.EmbedRecord
import work.socialhub.kbsky.model.app.bsky.embed.EmbedUnion
import work.socialhub.kbsky.model.app.bsky.embed.EmbedVideo
import work.socialhub.kbsky.model.share.Blob
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.ExperimentalTime

object EmbedBuilder {

    suspend fun createEmbedUnion(embeds: List<AttachedEmbed>?): EmbedUnion? {
        if (embeds.isNullOrEmpty()) return null

        // urlStringがセット -> リンクカード
        embeds.firstOrNull { it.type == BlueskyTypes.EmbedExternal }?.let {
            return createExternalEmbed(it)
        }

        // 画像blobがセット -> 画像
        embeds.firstOrNull { it.type == BlueskyTypes.EmbedImages }?.let {
            val imageEmbeds = embeds.filter { it.blob != null }
            if (imageEmbeds.isNotEmpty()) {
                return createImageEmbed(imageEmbeds)
            }
        }

        // 引用
        embeds.firstOrNull { it.type == BlueskyTypes.EmbedRecord }?.let {
            return createRecordEmbed(it)
        }

        // 動画
        embeds.firstOrNull { it.type == BlueskyTypes.EmbedVideo }?.let {
            return createVideoEmbed(it)
        }

        return null
    }

    private suspend fun createExternalEmbed(embed: AttachedEmbed): EmbedExternal {
        return EmbedExternal().apply {
            external = EmbedExternalExternal().apply {
                title = embed.title ?: ""
                uri = embed.urlString!!
                thumb = uploadBlob(embed)
                description = embed.description ?: ""
            }
        }
    }

    private suspend fun createImageEmbed(embeds: List<AttachedEmbed>): EmbedImages {
        return EmbedImages().apply {
            images = embeds.map { embed ->
                EmbedImagesImage().apply {
                    image = uploadBlob(embed)
                    alt = embed.filename ?: "image"
                    aspectRatio = embed.aspectRatio ?: EmbedDefsAspectRatio().apply {
                        width = 1
                        height = 1
                    }
                }
            }
        }
    }

    suspend fun uploadBlob(embed: AttachedEmbed): Blob? {
        return if (embed.blob != null && embed.filename != null) {
            try {
                SessionManager.runWithAuthRetry { auth ->
                    BlueskyFactory
                        .instance(BSKY_SOCIAL.uri)
                        .repo()
                        .uploadBlob(
                            RepoUploadBlobRequest(
                                auth = auth,
                                bytes = embed.blob!!,
                                name = embed.filename!!,
                                contentType = embed.contentType!!
                            )
                        )
                        .data.blob
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    private fun createRecordEmbed(embed: AttachedEmbed): EmbedRecord {
        return EmbedRecord().apply {
            record = embed.ref
        }
    }


    @OptIn(ExperimentalTime::class)
    private suspend fun createVideoEmbed(embed: AttachedEmbed): EmbedVideo? {
        return try {
            // 1. getServiceAuth
//            val expTime = Instant.now().plus(5, ChronoUnit.MINUTES).epochSecond
//            val authResponse = SessionManager.runWithAuthRetry { auth ->
//                BlueskyFactory
//                    .instance(Service.BSKY_SOCIAL.uri)
//                    .server()
//                    .getServiceAuth(ServerGetServiceAuthRequest(
//                        auth = auth,
//                        aud = "did:web:video.bsky.app",
//                        exp = expTime,
//                        lxm = "app.bsky.video.getUploadLimits",
//                    ))
//            }
//            val token = authResponse.data.token

            // 2. uploadVideo
            if (embed.blob == null) return null

            val uploadResponse = SessionManager.runWithAuthRetry { auth ->
                BlueskyFactory
                    .instance(BSKY_SOCIAL.uri)
                    .video()
                    .uploadVideo(
                        VideoUploadVideoRequest(
                            auth = auth,
                            bytes = embed.blob!!
                        )
                    )
            }

            var blob: Blob? = null
            for (i in 0 until 60) {
                val statusResponse = SessionManager.runWithAuthRetry { auth ->
                    BlueskyFactory
                        .instance(BSKY_SOCIAL.uri)
                        .video()
                        .getJobStatus(
                            VideoGetJobStatusRequest(
                                auth = auth,
                                jobId = uploadResponse.data.jobId,
                            )
                        )
                }

                if (statusResponse.data.jobStatus.state == "JOB_STATE_COMPLETED") {
                    blob = statusResponse.data.jobStatus.blob
                    Log.i("EmbedBuilder", "getJobStatus: completed")
                    break
                }

                // Waiting 1s
                Log.i("EmbedBuilder", "getJobStatus: processing")
                Thread.sleep(1000)
            }

            if (blob == null) {
                throw IllegalStateException("Video Process is failed")
            }

            EmbedVideo().also {
                it.video = blob
                it.alt = embed.filename
                it.aspectRatio = embed.aspectRatio
            }

        } catch (e: Exception) {
            Log.e("EmbedBuilder", "Create embed video error: ", e)
            null
        }
    }
}