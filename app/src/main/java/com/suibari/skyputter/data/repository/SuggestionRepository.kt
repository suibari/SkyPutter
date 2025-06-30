package com.suibari.skyputter.data.repository

import android.util.Log
import com.suibari.skyputter.data.db.SuggestionEntity
import com.suibari.skyputter.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetAuthorFeedRequest
import work.socialhub.kbsky.domain.Service.BSKY_SOCIAL
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SuggestionRepository() {
    suspend fun fetchOwnPosts(userDid: String, limit: Int = 1000): List<String> = withContext(Dispatchers.IO) {
        val posts = mutableListOf<String>()
        var cursor: String? = null

        repeat(10) { // 最大1000件 = 100件×10ページ
            val response = SessionManager.runWithAuthRetry { auth ->
                BlueskyFactory
                    .instance(BSKY_SOCIAL.uri)
                    .feed()
                    .getAuthorFeed(
                        FeedGetAuthorFeedRequest(auth).also {
                            it.actor = userDid
                            it.limit = limit
                            it.cursor = cursor
                        }
                    )
            }
            val items = response.data.feed
                .mapNotNull { it.post.record?.asFeedPost?.text }

            posts.addAll(items)
            cursor = response.data.cursor ?: return@repeat
        }

        return@withContext posts.take(limit)
    }

    suspend fun sendToMorphServer(texts: List<String>): List<SuggestionEntity> = withContext(Dispatchers.IO) {
        try {
            val json = Json.encodeToString(mapOf("texts" to texts))
            val conn = URL("https://negaposi-api.onrender.com/analyze").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            conn.outputStream.use { it.write(json.toByteArray()) }

            val response = conn.inputStream.bufferedReader().readText()
            val jsonObject = Json.parseToJsonElement(response).jsonObject

            val wakatiList = jsonObject["wakati"]?.jsonArray ?: return@withContext emptyList()

            return@withContext wakatiList.mapIndexed { i, arr ->
                val tokens = arr.jsonArray.map { it.jsonPrimitive.content }
                SuggestionEntity(text = texts[i], tokens = tokens.toString())
            }
        } catch (e: Exception) {
            Log.e("SuggestionRepo", "形態素解析失敗", e)
            return@withContext emptyList()
        }
    }
}
