package com.suibari.skyposter.data.repository

import com.suibari.skyposter.ui.main.AttachedEmbed
import com.suibari.skyposter.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.actor.ActorGetProfileRequest
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedPostRequest
import work.socialhub.kbsky.api.entity.com.atproto.repo.RepoUploadBlobRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewDetailed
import work.socialhub.kbsky.model.app.bsky.embed.EmbedDefsAspectRatio
import work.socialhub.kbsky.model.app.bsky.embed.EmbedExternal
import work.socialhub.kbsky.model.app.bsky.embed.EmbedExternalExternal
import work.socialhub.kbsky.model.app.bsky.embed.EmbedImages
import work.socialhub.kbsky.model.app.bsky.embed.EmbedImagesImage
import work.socialhub.kbsky.model.app.bsky.embed.EmbedUnion
import work.socialhub.kbsky.model.app.bsky.feed.FeedPostReplyRef
import work.socialhub.kbsky.model.share.Blob
import work.socialhub.kbsky.util.facet.FacetUtil
import java.net.URL

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

    suspend fun postText(
        postText: String,
        embed: AttachedEmbed?,
        replyTo: FeedPostReplyRef? = null
    ) = SessionManager.runWithAuthRetry { auth ->
        var embedUnion: EmbedUnion? = null

        if (embed?.urlString != null) {
            // External
            embedUnion = embed.let {
                EmbedExternal().apply {
                    external = EmbedExternalExternal().apply {
                        title = embed.title ?: ""
                        uri = embed.urlString!!
                        thumb = uploadBlob(embed)
                        description = embed.description ?: ""
                    }
                }
            }
        } else if (embed?.blob != null) {
            // Images
            embedUnion = embed.let {
                EmbedImages().apply {
                    images = listOf(
                        EmbedImagesImage().apply {
                            image = uploadBlob(embed)
                            alt = "image from SkyPoster"
                            aspectRatio = embed.aspectRatio ?: EmbedDefsAspectRatio().apply {
                                width = 1
                                height = 1
                            }
                        }
                    )
                }
            }
        }

        // Facets判別
        val facetlist = FacetUtil.extractFacets(postText)
        val displayText = facetlist.displayText()
        val facets = facetlist.richTextFacets(mutableMapOf())

        BlueskyFactory.instance(BSKY_SOCIAL.uri)
            .feed()
            .post(
                FeedPostRequest(auth).apply {
                    this.text = displayText
                    this.embed = embedUnion
                    this.reply = replyTo
                    this.facets = facets
                }
            )
    }

    private suspend fun uploadBlob(embed: AttachedEmbed): Blob? {
        if (embed.blob != null && embed.filename != null) {
            return SessionManager.runWithAuthRetry { auth ->
                val response = BlueskyFactory
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
                response.data.blob
            }
        } else {
            return null
        }
    }

    suspend fun fetchOgImageEmbed(url: String): AttachedEmbed? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url).get()
            val ogImageRaw = doc.select("meta[property=og:image]").attr("content")
            val ogTitle = doc.select("meta[property=og:title]").attr("content")
            val ogImage = URL(URL(url), ogImageRaw).toString()

            val (imageData, contentType) = getByteArrayFromUrl(ogImage) ?: return@withContext null
            val ext = extensionFromContentType(contentType)

            AttachedEmbed(
                title = ogTitle,
                filename = "ogp.$ext",
                urlString = url,
                imageUriString = ogImage,
                blob = imageData,
                contentType = contentType,
                aspectRatio = null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun getByteArrayFromUrl(url: String): Pair<ByteArray?, String?>? {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        return try {
            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }
            if (response.isSuccessful) {
                val blob = response.body?.bytes()
                val contentType = response.body?.contentType()?.toString() ?: "image/jpeg"
                Pair(blob, contentType)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extensionFromContentType(contentType: String?): String {
        return when {
            contentType?.contains("jpeg") == true -> "jpg"
            contentType?.contains("png") == true -> "png"
            contentType?.contains("webp") == true -> "webp"
            contentType?.contains("gif") == true -> "gif"
            else -> "bin"
        }
    }
}
