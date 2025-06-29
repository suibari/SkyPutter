package com.suibari.skyputter

import android.content.Context
import com.suibari.skyputter.data.repository.EmbedBuilder
import com.suibari.skyputter.data.repository.MainRepository
import com.suibari.skyputter.data.repository.PostResult
import com.suibari.skyputter.ui.main.AttachedEmbed
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.mockk.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class MainRepoUnitTest {

    @Test
    fun `uploadBlob throws exception, error is set`() = runTest {
        // setup
        mockkObject(EmbedBuilder)

        val context = mockk<Context>()
        val embed = mockk<AttachedEmbed>()
        val repo = MainRepository()
        coEvery { EmbedBuilder.uploadBlob(embed) } throws Exception("invalid file")

        // Act
        val result = repo.postText(
            context = context,
            postText = "テスト投稿",
            embeds = listOf(embed)
        )

        // Assert
        assertTrue(result is PostResult.Error)
        assertEquals("投稿に失敗しました", result.message)
    }
}