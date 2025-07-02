package com.suibari.skyputter.ui.settings

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.suibari.skyputter.data.db.SuggestionDatabase
import com.suibari.skyputter.data.repository.SuggestionBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class SuggestionProgressState {
    Idle, CollectingPosts, AnalyzingPosts, SavingSuggestions
}

class SettingsViewModel(
    private val context: Context,
) : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    private val _suggestionProgress = MutableStateFlow(SuggestionProgressState.Idle)
    val suggestionProgress: StateFlow<SuggestionProgressState> = _suggestionProgress

    suspend fun initializeSuggestion(userDid: String) {
        if (isLoading) return

        isLoading = true
        try {
            Log.i("SettingsViewModel", "start initialize")

            _suggestionProgress.value = SuggestionProgressState.CollectingPosts
            val posts = SuggestionBuilder.fetchOwnPosts(userDid, limit = 1000)
            Log.i("SettingsViewModel", "posts: ${posts.size}")

            _suggestionProgress.value = SuggestionProgressState.AnalyzingPosts
            val suggestions = SuggestionBuilder.getEntitiesFromMorphServer(posts)
            Log.i("SettingsViewModel", "suggestions: ${suggestions.size}")

            _suggestionProgress.value = SuggestionProgressState.SavingSuggestions
            SuggestionDatabase.getInstance(context).suggestionDao().clearAll()
            SuggestionDatabase.getInstance(context).suggestionDao().insertAll(suggestions)
            Log.i("SettingsViewModel", "saved database")

            _suggestionProgress.value = SuggestionProgressState.Idle
        } catch (e: Exception) {
            // エラー処理
        } finally {
            isLoading = false
        }
    }
}
