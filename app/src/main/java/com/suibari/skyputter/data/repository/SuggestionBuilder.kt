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
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetAuthorFeedRequest.Filter

object SuggestionBuilder {
    suspend fun fetchOwnPosts(userDid: String, limit: Int = 1000): List<String> = withContext(Dispatchers.IO) {
        val posts = mutableListOf<String>()
        var cursor: String? = null

        while (true) {
            val response = SessionManager.runWithAuthRetry { auth ->
                BlueskyFactory
                    .instance(BSKY_SOCIAL.uri)
                    .feed()
                    .getAuthorFeed(
                        FeedGetAuthorFeedRequest(auth).also {
                            it.actor = userDid
                            it.limit = 100
                            it.cursor = cursor
                            it.filter = Filter.PostsAndAuthorThreads
                        }
                    )
            }

            val items = response.data.feed
                .filter { it.reason == null }
                .mapNotNull { it.post.record?.asFeedPost?.text }

            posts.addAll(items)
            Log.i("SuggestionRepo", "fetched ${items.size} posts (total=${posts.size})")

            // 終了条件
            if (posts.size >= limit || response.data.cursor == null) {
                break
            }

            cursor = response.data.cursor
        }

        return@withContext posts.take(limit)
    }

    /**
     * 過去ポストをまとめて形態素解析サーバに投げ、結果をDBに格納するための関数
     */
    suspend fun sendToMorphServerAll(texts: List<String>): List<SuggestionEntity> = withContext(Dispatchers.IO) {
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
            Log.e("SuggestionBuilder", "sendToMorphServerAll failed", e)
            emptyList()
        }
    }

    /**
     * 入力テキストを形態素解析してクエリ化するための関数
     */
    suspend fun sendToMorphServerSingle(text: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val json = Json.encodeToString(mapOf("texts" to listOf(text)))
            val conn = URL("https://negaposi-api.onrender.com/analyze").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            conn.outputStream.use { it.write(json.toByteArray()) }

            val response = conn.inputStream.bufferedReader().readText()
            val jsonObject = Json.parseToJsonElement(response).jsonObject
            val tokensJsonArray = jsonObject["wakati"]?.jsonArray?.firstOrNull()?.jsonArray
                ?: return@withContext emptyList()

            return@withContext tokensJsonArray.map { it.jsonPrimitive.content }
        } catch (e: Exception) {
            Log.e("SuggestionBuilder", "sendToMorphServerSingle failed", e)
            emptyList()
        }
    }
}
