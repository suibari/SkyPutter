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

// カスタム例外クラス群
sealed class EmbedException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data class InvalidInput(val field: String, val value: String?) : EmbedException("Invalid input: $field = $value")
    data class UploadFailed(val filename: String, override val cause: Throwable) : EmbedException("Upload failed: $filename", cause)
    data class VideoProcessingFailed(val jobId: String) : EmbedException("Video processing failed: jobId = $jobId")
    data class VideoProcessingTimeout(val jobId: String) : EmbedException("Video processing timeout: jobId = $jobId")
    data class ExternalEmbedFailed(override val cause: Throwable) : EmbedException("External embed creation failed", cause)
    data class ImageEmbedFailed(override val cause: Throwable) : EmbedException("Image embed creation failed", cause)
    data class RecordEmbedFailed(override val cause: Throwable) : EmbedException("Record embed creation failed", cause)
    data class VideoEmbedFailed(override val cause: Throwable) : EmbedException("Video embed creation failed", cause)
    data class EmbedUnionFailed(override val cause: Throwable) : EmbedException("Embed union creation failed", cause)
}

object EmbedBuilder {

    suspend fun createEmbedUnion(context: Context, embeds: List<AttachedEmbed>?): EmbedUnion? =
        withContext(Dispatchers.IO) {
            try {
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
            } catch (e: EmbedException) {
                Log.e("EmbedBuilder", "Embed creation failed", e)
                throw e
            } catch (e: Exception) {
                Log.e("EmbedBuilder", "Unexpected error in embed creation", e)
                throw EmbedException.EmbedUnionFailed(e)
            }
        }

    private suspend fun createExternalEmbed(embed: AttachedEmbed): EmbedExternal {
        try {
            return EmbedExternal().apply {
                external = EmbedExternalExternal().apply {
                    title = embed.title ?: ""
                    uri = embed.urlString ?: throw EmbedException.InvalidInput("urlString", null)
                    thumb = uploadBlob(embed)
                    description = embed.description ?: ""
                }
            }
        } catch (e: EmbedException) {
            Log.e("EmbedBuilder", "External embed creation failed", e)
            throw e
        } catch (e: Exception) {
            Log.e("EmbedBuilder", "Unexpected error in external embed creation", e)
            throw EmbedException.ExternalEmbedFailed(e)
        }
    }

    private suspend fun createImageEmbed(embeds: List<AttachedEmbed>): EmbedImages {
        if (embeds.isEmpty()) throw EmbedException.InvalidInput("embeds", "empty")

        try {
            val imageItems = embeds.mapIndexed { index, embed ->
                val blob = uploadBlob(embed)

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
                throw EmbedException.InvalidInput("imageItems", "empty after upload")
            }

            return EmbedImages().apply {
                images = imageItems
            }
        } catch (e: EmbedException) {
            Log.e("EmbedBuilder", "Image embed creation failed", e)
            throw e
        } catch (e: Exception) {
            Log.e("EmbedBuilder", "Unexpected error in image embed creation", e)
            throw EmbedException.ImageEmbedFailed(e)
        }
    }

    suspend fun uploadBlob(embed: AttachedEmbed): Blob {
        val filename = embed.filename ?: throw EmbedException.InvalidInput("filename", null)
        val contentType = embed.contentType ?: throw EmbedException.InvalidInput("contentType", null)
        val blobData = embed.blob ?: throw EmbedException.InvalidInput("blob", null)

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
            throw EmbedException.UploadFailed(filename, e)
        }
    }

    private fun createRecordEmbed(embed: AttachedEmbed): EmbedRecord {
        try {
            return EmbedRecord().apply {
                record = embed.ref ?: throw EmbedException.InvalidInput("ref", null)
            }
        } catch (e: EmbedException) {
            Log.e("EmbedBuilder", "Record embed creation failed", e)
            throw e
        } catch (e: Exception) {
            Log.e("EmbedBuilder", "Unexpected error in record embed creation", e)
            throw EmbedException.RecordEmbedFailed(e)
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun createVideoEmbed(embed: AttachedEmbed): EmbedVideo {
        try {
            if (embed.blob == null) {
                throw EmbedException.InvalidInput("blob", null)
            }

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
            val jobId = uploadResponse.data.jobId

            for (i in 0 until 60) {
                val statusResponse = SessionManager.runWithAuthRetry { auth ->
                    BlueskyFactory
                        .instance(BSKY_SOCIAL.uri)
                        .video()
                        .getJobStatus(
                            VideoGetJobStatusRequest(
                                auth = auth,
                                jobId = jobId,
                            )
                        )
                }

                when (statusResponse.data.jobStatus.state) {
                    "JOB_STATE_COMPLETED" -> {
                        blob = statusResponse.data.jobStatus.blob
                        Log.i("EmbedBuilder", "Video processing completed: $jobId")
                        break
                    }
                    "JOB_STATE_FAILED" -> {
                        throw EmbedException.VideoProcessingFailed(jobId)
                    }
                }

                Log.i("EmbedBuilder", "Video processing: $jobId")
                delay(1000)
            }

            if (blob == null) {
                throw EmbedException.VideoProcessingTimeout(jobId)
            }

            return EmbedVideo().also {
                it.video = blob
                it.alt = "video0"
                it.aspectRatio = embed.aspectRatio
            }

        } catch (e: EmbedException) {
            Log.e("EmbedBuilder", "Video embed creation failed", e)
            throw e
        } catch (e: Exception) {
            Log.e("EmbedBuilder", "Unexpected error in video embed creation", e)
            throw EmbedException.VideoEmbedFailed(e)
        }
    }
}