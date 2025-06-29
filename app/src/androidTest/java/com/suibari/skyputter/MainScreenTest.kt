package com.suibari.skyputter.ui.main

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.suibari.skyputter.ErrorMainViewModel
import com.suibari.skyputter.FakeSkyPutterApp
import com.suibari.skyputter.MainActivity
import com.suibari.skyputter.SkyPutterApp
import com.suibari.skyputter.SuccessMainViewModel
import com.suibari.skyputter.data.repository.MainRepository
import com.suibari.skyputter.data.repository.NotificationRepository
import com.suibari.skyputter.data.repository.UserPostRepository
import com.suibari.skyputter.ui.notification.NotificationViewModel
import com.suibari.skyputter.ui.post.UserPostViewModel
import com.suibari.skyputter.util.DraftViewModel
import com.suibari.skyputter.worker.DeviceNotifier
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import work.socialhub.kbsky.BlueskyTypes

@RunWith(AndroidJUnit4::class)
class MainScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    val fakeApp = FakeSkyPutterApp()

    val context = InstrumentationRegistry.getInstrumentation().targetContext

    val repo = MainRepository()
    val repoUserPost = UserPostRepository()
    val repoNotif = NotificationRepository(context)
    val notifier = DeviceNotifier(context)

    val userPostViewModel = UserPostViewModel(repoUserPost)
    val notificationViewModel = NotificationViewModel(repoNotif, notifier)

    val fakeViewModel = ErrorMainViewModel(
        repo,
        userPostViewModel,
        notificationViewModel
    )
    val draftViewModel = DraftViewModel(context)

    @Test
    fun postSuccess_clearsTextAndEmbeds() {

        // UIを構築
        composeTestRule.setContent {
            MainScreen(
                application = fakeApp,
                viewModel = fakeViewModel,
                notificationViewModel = notificationViewModel,
                draftViewModel = draftViewModel,
                initialText = "",
                onLogout = {},
                onOpenNotification = {},
                onOpenUserPost = {},
                onOpenDraft = {},
                onOpenSettings = {},
                onOpenAbout = {},
                onDraftTextCleared = {},
            )
        }

        // テキスト入力
        val inputText = "テスト投稿"
        composeTestRule.onNodeWithTag("PostInput")
            .performTextInput(inputText)

        // 画像添付
        composeTestRule.runOnUiThread {
            fakeViewModel.addEmbed(
                AttachedEmbed(
                    type = BlueskyTypes.EmbedImages,
                )
            )
        }

        // プレビュー確認
        composeTestRule.onAllNodesWithContentDescription("削除")
            .assertCountEquals(1)

        // 投稿ボタン押下
        composeTestRule.onNodeWithText("ポスト")
            .performClick()

        // 投稿完了後、テキスト欄がクリアされていること
        composeTestRule.onNodeWithTag("PostInput")
            .assert(hasText(""))

        // プレビュー欄が表示されていないこと（削除ボタンなど）
        composeTestRule.onAllNodesWithContentDescription("削除")
            .assertCountEquals(0)
    }

    @Test
    fun postFailure_remainsTextAndEmbeds() {

        // UIを構築
        composeTestRule.setContent {
            MainScreen(
                application = fakeApp,
                viewModel = fakeViewModel,
                notificationViewModel = notificationViewModel,
                draftViewModel = draftViewModel,
                initialText = "",
                onLogout = {},
                onOpenNotification = {},
                onOpenUserPost = {},
                onOpenDraft = {},
                onOpenSettings = {},
                onOpenAbout = {},
                onDraftTextCleared = {},
            )
        }

        // テキスト入力
        val inputText = "テスト投稿"
        composeTestRule.onNodeWithTag("PostInput")
            .performTextInput(inputText)

        // 画像添付
        composeTestRule.runOnUiThread {
            fakeViewModel.addEmbed(
                AttachedEmbed(
                    type = BlueskyTypes.EmbedImages,
                )
            )
        }

        // プレビュー確認
        composeTestRule.onAllNodesWithContentDescription("削除")
            .assertCountEquals(1)

        // 投稿ボタン押下
        composeTestRule.onNodeWithText("ポスト")
            .performClick()

        // 投稿完了後、テキスト欄がそのまま
        composeTestRule.onNodeWithTag("PostInput")
            .assert(hasText(inputText))

        // プレビュー欄が表示されたまま
        composeTestRule.onAllNodesWithContentDescription("削除")
            .assertCountEquals(1)
    }

    @Test
    fun errorMessage_showsSnackbar() {
        val errorMessage = "投稿に失敗しました"

        // UI構築
        composeTestRule.setContent {
            MainScreen(
                application = fakeApp,
                viewModel = fakeViewModel,
                notificationViewModel = notificationViewModel,
                draftViewModel = draftViewModel,
                initialText = "",
                onLogout = {},
                onOpenNotification = {},
                onOpenUserPost = {},
                onOpenDraft = {},
                onOpenSettings = {},
                onOpenAbout = {},
                onDraftTextCleared = {},
            )
        }

        // エラーメッセージを発火させる
        composeTestRule.runOnUiThread {
            fakeViewModel.emitErrorForTest(errorMessage)
        }

        // Snackbar にエラーメッセージが表示されていることを確認: 非同期なので少し待つ
        composeTestRule.waitUntil(3_000) {
            composeTestRule.onAllNodesWithText(errorMessage).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }
}
