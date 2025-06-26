package com.suibari.skyputter.data.repository

import com.suibari.skyputter.ui.main.AttachedEmbed
import com.suibari.skyputter.util.SessionManager
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.BlueskyTypes
import work.socialhub.kbsky.api.entity.com.atproto.repo.RepoUploadBlobRequest
import work.socialhub.kbsky.domain.Service
import work.socialhub.kbsky.model.app.bsky.embed.EmbedDefsAspectRatio
import work.socialhub.kbsky.model.app.bsky.embed.EmbedExternal
import work.socialhub.kbsky.model.app.bsky.embed.EmbedExternalExternal
import work.socialhub.kbsky.model.app.bsky.embed.EmbedImages
import work.socialhub.kbsky.model.app.bsky.embed.EmbedImagesImage
import work.socialhub.kbsky.model.app.bsky.embed.EmbedRecord
import work.socialhub.kbsky.model.app.bsky.embed.EmbedUnion
import work.socialhub.kbsky.model.share.Blob

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
                        .instance(Service.BSKY_SOCIAL.uri)
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
}