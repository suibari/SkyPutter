package com.suibari.skyputter.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import work.socialhub.kbsky.model.app.bsky.embed.EmbedDefsAspectRatio
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

object Util {
    suspend fun getImageFromUri(
        context: Context,
        uri: Uri
    ): Triple<ByteArray, String, EmbedDefsAspectRatio?> = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val contentType = contentResolver.getType(uri) ?: "image/jpeg"

        val bitmap = contentResolver.openInputStream(uri)?.use { inputStream ->
            // メタデータを削除（単純にBitmapFactoryで読み込めばEXIF等は破棄される）
            BitmapFactory.decodeStream(inputStream)
        } ?: throw IllegalArgumentException("画像を読み込めません")

        val outputStream = ByteArrayOutputStream()
        var quality = 100
        var imageBytes: ByteArray

        do {
            outputStream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            imageBytes = outputStream.toByteArray()
            quality -= 10
            Log.i("getImageFromUri", "quality: $quality, size: ${imageBytes.size}")
        } while (imageBytes.size > 1024 * 1024 && quality >= 10)

        if (imageBytes.size > 1024 * 1024) {
            throw IllegalArgumentException("画像が1MB以下に圧縮できませんでした")
        }

        val aspectRatio = EmbedDefsAspectRatio().apply {
            width = bitmap.width
            height = bitmap.height
        }

        Triple(imageBytes, "image/jpeg", aspectRatio)
    }

    // 動画のメタデータのみ取得（軽量）
    suspend fun getVideoAspectRatio(context: Context, uri: Uri): EmbedDefsAspectRatio? =
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)

                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()

                retriever.release()

                if (width != null && height != null && height != 0) {
                    EmbedDefsAspectRatio().apply {
                        this.width = width
                        this.height = height
                    }
                } else null
            } catch (e: Exception) {
                null
            }
        }

    // 動画の実際の処理（投稿時のみ実行）
    suspend fun processVideoForUpload(
        context: Context,
        uri: Uri,
        onProgress: (Float) -> Unit = {}
    ): ByteArray = withContext(Dispatchers.IO) {
        onProgress(0.1f)

        // 動画圧縮処理
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("動画を読み込めません")

        onProgress(0.3f)

        // 実際の圧縮・変換処理
        val processedData = compressVideo(inputStream)

        onProgress(0.9f)

        inputStream.close()
        processedData
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private suspend fun compressVideo(inputStream: InputStream): ByteArray {
        // 動画圧縮の実装
        // MediaMetadataRetriever や FFmpeg ライブラリを使用
        // 実装は複雑になるため、必要に応じて詳細を提供
        return inputStream.readBytes() // 暫定実装
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

    fun formatDeviceLocaleDate(isoString: String): String {
        return try {
            val instant = Instant.parse(isoString)
            val local = instant.atZone(ZoneId.systemDefault())
            String.format(
                Locale.getDefault(),
                "%d年%d月%d日%d時%d分",
                local.year,
                local.monthValue,
                local.dayOfMonth,
                local.hour,
                local.minute
            )
        } catch (e: Exception) {
            isoString // パース失敗時は元の文字列を返す
        }
    }
}