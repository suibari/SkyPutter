package com.suibari.skyputter.data.repository

import android.os.Build
import com.suibari.skyputter.ui.main.AttachedEmbed
import com.suibari.skyputter.util.SessionManager
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
import work.socialhub.kbsky.model.app.bsky.richtext.RichtextFacet
import work.socialhub.kbsky.model.share.Blob
import work.socialhub.kbsky.util.facet.FacetUtil
import java.net.URL
import java.util.Locale

sealed class PostResult {
    object Success : PostResult()
    data class Error(val message: String, val exception: Throwable? = null) : PostResult()
}

sealed class ProfileResult {
    data class Success(val profile: ActorDefsProfileViewDetailed) : ProfileResult()
    data class Error(val message: String, val exception: Throwable? = null) : ProfileResult()
}

sealed class OgImageResult {
    data class Success(val embed: AttachedEmbed) : OgImageResult()
    data class Error(val message: String, val exception: Throwable? = null) : OgImageResult()
    object NotFound : OgImageResult()
}

class MainRepository {

    suspend fun getProfile(): ProfileResult {
        return try {
            val profile = SessionManager.runWithAuthRetry { auth ->
                BlueskyFactory
                    .instance(BSKY_SOCIAL.uri)
                    .actor()
                    .getProfile(
                        ActorGetProfileRequest(auth).also { it.actor = auth.did }
                    ).data
            }
            ProfileResult.Success(profile)
        } catch (e: Exception) {
            ProfileResult.Error("プロフィールの取得に失敗しました", e)
        }
    }

    suspend fun postText(
        postText: String,
        embeds: List<AttachedEmbed>?,
        replyTo: FeedPostReplyRef? = null
    ): PostResult {
        return try {
            val embedUnion = createEmbedUnion(embeds)
            val (displayText, facets) = extractTextAndFacets(postText)

            SessionManager.runWithAuthRetry { auth ->
                BlueskyFactory.instance(BSKY_SOCIAL.uri)
                    .feed()
                    .post(
                        FeedPostRequest(auth).apply {
                            this.text = displayText
                            this.embed = embedUnion
                            this.reply = replyTo
                            this.facets = facets
                            this.langs = listOf(Locale.getDefault().language)
                        }
                    )
            }
            PostResult.Success
        } catch (e: Exception) {
            PostResult.Error("投稿に失敗しました", e)
        }
    }

    /**
     * URL入力時、URLからOGP情報を取得しEmbedセット
     */
    suspend fun fetchOgImageEmbed(url: String): OgImageResult = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url).get()
            val ogImageRaw = doc.select("meta[property=og:image]").attr("content")
            val ogTitle = doc.select("meta[property=og:title]").attr("content")
            val ogDescription = doc.select("meta[property=og:description]").attr("content")

            if (ogImageRaw.isEmpty()) {
                return@withContext OgImageResult.NotFound
            }

            val ogImage = URL(URL(url), ogImageRaw).toString()
            val (imageData, contentType) = getByteArrayFromUrl(ogImage)
                ?: return@withContext OgImageResult.Error("画像の取得に失敗しました")

            val ext = extensionFromContentType(contentType)

            val embed = AttachedEmbed(
                title = ogTitle.takeIf { it.isNotEmpty() },
                description = ogDescription.takeIf { it.isNotEmpty() },
                filename = "ogp.$ext",
                urlString = url,
                imageUriString = ogImage,
                blob = imageData,
                contentType = contentType,
                aspectRatio = null
            )

            OgImageResult.Success(embed)
        } catch (e: Exception) {
            OgImageResult.Error("OG画像の取得に失敗しました", e)
        }
    }

    private suspend fun createEmbedUnion(embeds: List<AttachedEmbed>?): EmbedUnion? {
        if (embeds.isNullOrEmpty()) return null

        // URLがある場合は最初のURLのものだけをExternalEmbedとして返す（他は無視）
        embeds.firstOrNull { it.urlString != null }?.let { urlEmbed ->
            return createExternalEmbed(urlEmbed)
        }

        // 画像や動画のblobがある場合は、それらすべてをEmbedImagesにまとめる
        val imageEmbeds = embeds.filter { it.blob != null }
        if (imageEmbeds.isNotEmpty()) {
            return createImageEmbed(imageEmbeds)
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

    private fun extractTextAndFacets(postText: String): Pair<String, List<RichtextFacet>> {
        val facetlist = FacetUtil.extractFacets(postText)
        val displayText = facetlist.displayText()
        val facets = facetlist.richTextFacets(mutableMapOf())
        return displayText to facets
    }

    private suspend fun uploadBlob(embed: AttachedEmbed): Blob? {
        return if (embed.blob != null && embed.filename != null) {
            try {
                SessionManager.runWithAuthRetry { auth ->
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
            } catch (e: Exception) {
                null
            }
        } else {
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
            } else {
                null
            }
        } catch (e: Exception) {
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