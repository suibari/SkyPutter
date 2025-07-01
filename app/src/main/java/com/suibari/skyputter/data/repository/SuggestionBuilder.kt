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
import kotlinx.serialization.json.*
import work.socialhub.kbsky.api.entity.app.bsky.feed.FeedGetAuthorFeedRequest.Filter
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost

object SuggestionBuilder {
    suspend fun fetchOwnPosts(userDid: String, limit: Int = 1000): List<FeedPost> = withContext(Dispatchers.IO) {
        val posts = mutableListOf<FeedPost>()
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
                .mapNotNull { it.post.record?.asFeedPost }

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
    suspend fun sendToMorphServerAll(
        posts: List<FeedPost>,
    ): List<SuggestionEntity> = withContext(Dispatchers.IO) {
        try {
            // 投稿本文を抽出（null除外）
            val texts = posts.mapNotNull { it.text }

            // JSONで送信
            val json = Json.encodeToString(mapOf("texts" to texts))
            val conn = URL("https://negaposi-api.onrender.com/analyze").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            conn.outputStream.use { it.write(json.toByteArray()) }

            val response = conn.inputStream.bufferedReader().readText()
            val jsonObject = Json.parseToJsonElement(response).jsonObject

            // "nouns" は List<List<String>> 形式で返ってくる前提
            val nounsList = jsonObject["nouns"]?.jsonArray ?: return@withContext emptyList()
            val sentimentList = jsonObject["average_sentiments"]?.jsonArray ?: return@withContext emptyList()

            // DB保存
            return@withContext nounsList.mapIndexedNotNull { i, element ->
                val post = posts.getOrNull(i) ?: return@mapIndexedNotNull null
                val sentiment = sentimentList.getOrNull(i)?.jsonPrimitive?.floatOrNull ?: return@mapIndexedNotNull null

                // 各ポストに対応する名詞のリストを取得
                val tokens = element.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }
                val tokensString = tokens.joinToString(" ") // 空白区切り

                Log.i(
                    "SuggestionBuilder",
                    "text: ${post.text}, tokens: $tokensString, sentiment: $sentiment, createdAt: ${post.createdAt}"
                )

                SuggestionEntity(
                    text = post.text ?: "",
                    tokens = tokensString,
                    sentiment = sentiment.toString(),
                    createdAt = post.createdAt.toString()
                )
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
            val nounsList = jsonObject["nouns"]?.jsonArray ?: return@withContext emptyList()

            // nounsList: <List<List<String>>> なのでそれに対応
            val flattenedNouns = nounsList.flatMap { innerElement ->
                innerElement.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }
            }

            return@withContext flattenedNouns
        } catch (e: Exception) {
            Log.e("SuggestionBuilder", "sendToMorphServerSingle failed", e)
            emptyList()
        }
    }
}
