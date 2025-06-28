package com.suibari.skyputter.data.repository

import android.content.Context
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import com.suibari.skyputter.ui.main.AttachedEmbed
import com.suibari.skyputter.util.SessionManager
import com.suibari.skyputter.util.Util.processVideoForUpload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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

    suspend fun createEmbedUnion(context: Context, embeds: List<AttachedEmbed>?): EmbedUnion? =
        withContext(Dispatchers.IO) {
            if (embeds.isNullOrEmpty()) return@withContext null

            // urlStringがセット -> リンクカード
            embeds.firstOrNull { it.type == BlueskyTypes.EmbedExternal }?.let {
                return@withContext createExternalEmbed(it)
            }

            // 画像blobがセット -> 画像
            embeds.firstOrNull { it.type == BlueskyTypes.EmbedImages }?.let {
                val imageEmbeds = embeds.filter { it.blob != null }
                if (imageEmbeds.isNotEmpty()) {
                    return@withContext createImageEmbed(imageEmbeds)
                }
            }

            // 引用
            embeds.firstOrNull { it.type == BlueskyTypes.EmbedRecord }?.let {
                return@withContext createRecordEmbed(it)
            }

            // 動画
            embeds.firstOrNull { it.type == BlueskyTypes.EmbedVideo && it.uriString != null }?.let {
                it.blob = processVideoForUpload(context, it.uri!!) // RepoでByteArrayに変換
                return@withContext createVideoEmbed(it)
            }

            return@withContext null
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
        if (embeds.isEmpty()) throw IllegalStateException("Not select image")

        val imageItems = embeds.mapIndexed { index, embed ->
            val blob = uploadBlob(embed)  // null を許容しない修正済み関数

            EmbedImagesImage().apply {
                image = blob
                alt = "image$index"
                aspectRatio = embed.aspectRatio ?: EmbedDefsAspectRatio().apply {
                    width = 1
                    height = 1
                }
            }
        }

        if (imageItems.isEmpty()) {
            throw IllegalStateException("Failed to upload all images")
        }

        return EmbedImages().apply {
            images = imageItems
        }
    }

    suspend fun uploadBlob(embed: AttachedEmbed): Blob {
        val filename = embed.filename ?: throw IllegalArgumentException("filename is null")
        val contentType = embed.contentType ?: throw IllegalArgumentException("contentType is null")
        val blobData = embed.blob ?: throw IllegalArgumentException("blob data is null")

        try {
            Log.d("uploadBlob", "Uploading: name=$filename, size=${blobData.size}, contentType=$contentType")

            val result = SessionManager.runWithAuthRetry { auth ->
                BlueskyFactory
                    .instance(BSKY_SOCIAL.uri)
                    .repo()
                    .uploadBlob(
                        RepoUploadBlobRequest(
                            auth = auth,
                            bytes = blobData,
                            name = filename,
                            contentType = contentType
                        )
                    )
                    .data.blob
            }

            Log.d("uploadBlob", "Upload successful: $filename")
            return result

        } catch (e: Exception) {
            Log.e("uploadBlob", "Upload failed for: $filename", e)
            throw IllegalStateException("Upload failed for: $filename", e)
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

                // 🔁 Thread.sleep → ✅ suspend-safe delay
                Log.i("EmbedBuilder", "getJobStatus: processing")
                delay(1000)
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