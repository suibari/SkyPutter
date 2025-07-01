package com.suibari.skyputter.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SuggestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<SuggestionEntity>)

    @Query("SELECT rowid, text, createdAt, tokens, sentiment FROM suggestion_entries WHERE tokens MATCH :query")
    suspend fun searchByTokens(query: String): List<SuggestionEntity>

    @Query("DELETE FROM suggestion_entries")
    suspend fun clearAll()
}