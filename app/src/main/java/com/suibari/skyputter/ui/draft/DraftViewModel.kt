package com.suibari.skyputter.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date
import java.util.UUID

data class DraftData(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

class DraftViewModel(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("draft_preferences", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val DRAFTS_KEY = "drafts_key"
    }

    fun saveDraft(text: String): DraftData {
        val drafts = getDrafts().toMutableList()
        val newDraft = DraftData(text = text)
        drafts.add(0, newDraft) // 最新を先頭に
        saveDrafts(drafts)
        return newDraft
    }

    fun getDrafts(): List<DraftData> {
        val json = sharedPreferences.getString(DRAFTS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<DraftData>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteDraft(draftId: String) {
        val drafts = getDrafts().toMutableList()
        drafts.removeAll { it.id == draftId }
        saveDrafts(drafts)
    }

    fun hasDrafts(): Boolean = getDrafts().isNotEmpty()

    private fun saveDrafts(drafts: List<DraftData>) {
        val json = gson.toJson(drafts)
        sharedPreferences.edit().putString(DRAFTS_KEY, json).apply()
    }
}