package com.suibari.skyputter.ui.settings

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.suibari.skyputter.data.db.SuggestionDatabase
import com.suibari.skyputter.data.repository.SuggestionRepository

class SettingsViewModel(
    private val suggestionRepository: SuggestionRepository,
    private val context: Context,
) : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    suspend fun initializeSuggestion(userDid: String) {
        if (isLoading) return

        isLoading = true
        try {
            Log.i("SettingsViewModel", "start initialize")

            val posts = suggestionRepository.fetchOwnPosts(userDid, limit = 1000)
            Log.i("SettingsViewModel", "posts: ${posts.size}")

            val suggestions = suggestionRepository.sendToMorphServer(posts)
            Log.i("SettingsViewModel", "suggestions: ${suggestions.size}")

            // DB保存
            SuggestionDatabase.getInstance(context).suggestionDao().insertAll(suggestions)
            Log.i("SettingsViewModel", "saved database")
        } catch (e: Exception) {
            // エラー処理
        } finally {
            isLoading = false
        }
    }
}
