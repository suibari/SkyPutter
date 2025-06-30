package com.suibari.skyputter.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Fts4

@Fts4
@Entity(tableName = "suggestion_entries")
data class SuggestionEntity(
    @PrimaryKey(autoGenerate = true)
    val rowid: Int = 0,
    val text: String,
    val tokens: String,
)