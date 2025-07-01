package com.suibari.skyputter.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [SuggestionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SuggestionDatabase : RoomDatabase() {
    abstract fun suggestionDao(): SuggestionDao

    companion object {
        @Volatile
        private var INSTANCE: SuggestionDatabase? = null

        fun getInstance(context: Context): SuggestionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SuggestionDatabase::class.java,
                    "suggestion_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}