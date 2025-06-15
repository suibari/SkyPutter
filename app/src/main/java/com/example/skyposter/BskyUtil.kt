package com.example.skyposter

object BskyUtil {
    fun parseAtUri(atUri: String): Triple<String, String, String>? {
        // "at://" を除去して分割
        val parts = atUri.removePrefix("at://").split("/")

        return if (parts.size == 3) {
            val (repo, collection, rkey) = parts
            Triple(repo, collection, rkey)
        } else {
            null // 形式が不正
        }
    }
}