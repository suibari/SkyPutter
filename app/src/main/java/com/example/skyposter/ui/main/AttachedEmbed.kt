package com.example.skyposter.ui.main

import android.net.Uri
import androidx.core.net.toUri
import work.socialhub.kbsky.model.app.bsky.embed.EmbedDefsAspectRatio
import java.net.URL

data class AttachedEmbed(
    var title: String? = null,
    var urlString: String? = null,
    var description: String? = null,
    var filename: String? = null,
    var imageUriString: String? = null,
    var blob: ByteArray? = null,
    var contentType: String? = null,
    var aspectRatio: EmbedDefsAspectRatio? = null
) {
    val imageUri: Uri?
        get() = imageUriString?.toUri()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttachedEmbed

        if (blob != null) {
            if (other.blob == null) return false
            if (!blob.contentEquals(other.blob)) return false
        } else if (other.blob != null) return false

        return true
    }

    override fun hashCode(): Int {
        return blob?.contentHashCode() ?: 0
    }
}
