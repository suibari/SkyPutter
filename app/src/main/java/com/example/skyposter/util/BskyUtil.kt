package com.example.skyposter.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import work.socialhub.kbsky.model.app.bsky.embed.EmbedDefsAspectRatio

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


