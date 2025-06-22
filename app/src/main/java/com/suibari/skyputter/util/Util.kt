package com.suibari.skyputter.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import work.socialhub.kbsky.model.app.bsky.embed.EmbedDefsAspectRatio

object Util {
    fun getByteArrayFromUri(context: Context, uri: Uri): Triple<ByteArray?, String?, EmbedDefsAspectRatio?> {

        val blob = uriToByteArray(context, uri)
        val contentType = context.contentResolver.getType(uri)
        val aspectRatio = getAspectRatioObject(context, uri)

        return Triple(blob, contentType, aspectRatio)
    }

    fun uriToByteArray(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAspectRatioObject(context: Context, uri: Uri): EmbedDefsAspectRatio? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (options.outWidth > 0 && options.outHeight > 0) {
                EmbedDefsAspectRatio().apply {
                    width = options.outWidth
                    height = options.outHeight
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getFileName(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    return it.getString(nameIndex)
                }
            }
        }
        return null
    }
}