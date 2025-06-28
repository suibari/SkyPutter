package com.suibari.skyputter.data.repository

import android.content.Context
import android.util.Log
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
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import work.socialhub.kbsky.model.app.bsky.actor.ActorDefsProfileViewDetailed
import work.socialhub.kbsky.model.app.bsky.feed.FeedPostReplyRef
import work.socialhub.kbsky.model.app.bsky.richtext.RichtextFacet
import work.socialhub.kbsky.util.facet.FacetUtil
import java.net.URL
import android.net.Uri.encode
import com.suibari.skyputter.data.repository.EmbedBuilder.createEmbedUnion
import org.json.JSONObject
import work.socialhub.kbsky.BlueskyTypes
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

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
        context: Context,
        postText: String,
        embeds: List<AttachedEmbed>?,
        replyTo: FeedPostReplyRef? = null
    ): PostResult {
        return try {
            val embedUnion = createEmbedUnion(context, embeds)
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

        } catch (e: IllegalArgumentException) {
            // 入力値がおかしい場合（blobがnullなど）
            PostResult.Error("添付ファイルが不正です（${e.message}）", e)

        } catch (e: IllegalStateException) {
            // アプリ内の状態異常（uploadBlob失敗など）
            PostResult.Error("画像や動画のアップロードに失敗しました（${e.message}）", e)

        } catch (e: IOException) {
            // 通信エラー
            PostResult.Error("通信エラーが発生しました。ネットワークをご確認ください。", e)

        } catch (e: Exception) {
            // その他未分類
            PostResult.Error("投稿に失敗しました（${e.localizedMessage}）", e)
        }
    }

    /**
     * URL入力時、URLからOGP情報を取得しEmbedセット
     */
    suspend fun fetchOgImageEmbed(url: String): OgImageResult = withContext(Dispatchers.IO) {
        try {
            Log.d("OGP", "Fetching OGP for: $url")

            val finalUrl = fetchFinalUrl(url) ?: return@withContext tryCardybFallback(url)
            Log.d("OGP", "Resolved final URL: $finalUrl")

            // Agent偽装
            val doc = Jsoup.connect(finalUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .get()

            val ogImageRaw = doc.select("meta[property=og:image]").attr("content")
            val ogTitle = doc.select("meta[property=og:title]").attr("content")
            val ogDescription = doc.select("meta[property=og:description]").attr("content")

            Log.d("OGP", "OG:image = $ogImageRaw")
            Log.d("OGP", "OG:title = $ogTitle")
            Log.d("OGP", "OG:desc  = $ogDescription")

            if (ogImageRaw.isEmpty() || ogTitle.isEmpty()) {
                Log.w("OGP", "Missing og:image or og:title. Falling back.")
                return@withContext tryCardybFallback(url)
            }

            val ogImage = URL(URL(finalUrl), ogImageRaw).toString()
            Log.d("OGP", "Resolved og:image full URL: $ogImage")

            val (imageData, contentType) = getByteArrayFromUrl(ogImage)
                ?: return@withContext tryCardybFallback(url)

            val ext = extensionFromContentType(contentType)
            Log.d("OGP", "Image contentType: $contentType, extension: $ext")

            return@withContext OgImageResult.Success(
                AttachedEmbed(
                    type = BlueskyTypes.EmbedExternal,
                    title = ogTitle.takeIf { it.isNotEmpty() },
                    description = ogDescription.takeIf { it.isNotEmpty() },
                    filename = "ogp.$ext",
                    urlString = finalUrl,
                    uriString = ogImage,
                    blob = imageData,
                    contentType = contentType,
                    aspectRatio = null
                )
            )
        } catch (e: Exception) {
            Log.e("OGP", "Primary OGP fetch failed: ${e.message}", e)
            return@withContext tryCardybFallback(url)
        }
    }

    /**
     * リダイレクト時に最終URLを得る
     */
    private suspend fun fetchFinalUrl(startUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .followRedirects(false) // 手動でリダイレクト追跡
                .build()

            var request = Request.Builder().url(startUrl).build()
            var response = client.newCall(request).execute()
            var redirectCount = 0

            while (response.isRedirect && redirectCount < 5) {
                val location = response.header("Location") ?: return@withContext null
                val nextUrl = URL(URL(startUrl), location).toString()

                request = Request.Builder().url(nextUrl).build()
                response = client.newCall(request).execute()
                redirectCount++
            }

            return@withContext response.request.url.toString()
        } catch (e: Exception) {
            return@withContext null
        }
    }

    /**
     * OPGを返す公式APIへのフェッチ関数
     */
    private suspend fun tryCardybFallback(url: String): OgImageResult {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val encodedUrl = encode(url)
            val request = Request.Builder()
                .url("https://cardyb.bsky.app/v1/extract?url=$encodedUrl")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return OgImageResult.NotFound

            val json = response.body?.string() ?: return OgImageResult.NotFound
            val obj = JSONObject(json)

            val title = obj.optString("title")
            val description = obj.optString("description")
            val imageUrl = obj.optString("image")

            if (title == "" && description == "" && imageUrl == "") return OgImageResult.NotFound

            val (blob, contentType) = if (imageUrl != "") {
                getByteArrayFromUrl(imageUrl) ?: (null to null)
            } else {
                null to null
            }

            val ext = extensionFromContentType(contentType)

            val embed = AttachedEmbed(
                type = BlueskyTypes.EmbedExternal,
                title = title,
                description = description,
                filename = if (blob != null) "ogp.$ext" else null,
                urlString = url,
                uriString = imageUrl,
                blob = blob,
                contentType = contentType,
                aspectRatio = null
            )
            Log.d("OGP", "Cardyb fallback succeeded")
            OgImageResult.Success(embed)
        } catch (e: Exception) {
            Log.e("OGP", "Cardyb fallback failed", e)
            OgImageResult.Error("Cardyb fallback failed", e)
        }
    }

    private fun extractTextAndFacets(postText: String): Pair<String, List<RichtextFacet>> {
        val facetlist = FacetUtil.extractFacets(postText)
        val displayText = facetlist.displayText()
        val facets = facetlist.richTextFacets(mutableMapOf())
        return displayText to facets
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