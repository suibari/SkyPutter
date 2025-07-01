package com.suibari.skyputter.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Fts4
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.json.Json
import work.socialhub.kbsky.model.app.bsky.feed.FeedPost

@Fts4
@Entity(tableName = "suggestion_entries")
data class SuggestionEntity(
    @PrimaryKey(autoGenerate = true)
    val rowid: Int = 0,
    val text: String,
    val createdAt: String,
    val tokens: String,
    val sentiment: String,
)