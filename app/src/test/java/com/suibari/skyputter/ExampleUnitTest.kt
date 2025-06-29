package com.suibari.skyputter

import android.content.Context
import com.suibari.skyputter.data.repository.EmbedBuilder
import com.suibari.skyputter.data.repository.MainRepository
import com.suibari.skyputter.data.repository.PostResult
import com.suibari.skyputter.ui.main.AttachedEmbed
import com.suibari.skyputter.util.SessionManager
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.mockk.*
import work.socialhub.kbsky.BlueskyTypes

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class MainRepoUnitTest {

    @Test
    fun unittest_uploadBlobException() = runTest {
        // setup
        mockkObject(EmbedBuilder)

        val context = mockk<Context>(relaxed = true)
        SessionManager.initialize(context)

        val dummyEmbed = AttachedEmbed(
            type = BlueskyTypes.EmbedImages,
            filename = "dummy.jpg",
            contentType = "image/jpeg",
            blob = ByteArray(1)
        )
        val repo = MainRepository()
        coEvery { EmbedBuilder.uploadBlob(dummyEmbed) } throws IllegalStateException("Failed to upload all images")

        // Act
        val result = repo.postText(
            context = context,
            postText = "テスト投稿",
            embeds = listOf(dummyEmbed)
        )

        // Assert
        assertTrue(result is PostResult.Error)
        assertEquals("画像や動画のアップロードに失敗しました: Failed to upload all images", result.message)
    }
}