package com.suibari.skyputter.ui.sentiment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.suibari.skyputter.data.db.SuggestionDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class SentimentCalendarViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val suggestionDao = SuggestionDatabase.getInstance(application).suggestionDao()
    private val _dailySentiments = MutableStateFlow<Map<LocalDate, Float>>(emptyMap())
    val dailySentiments: StateFlow<Map<LocalDate, Float>> = _dailySentiments

    init {
        viewModelScope.launch {
            val result = suggestionDao.getAverageSentimentPerDay()
            _dailySentiments.value = result.associate {
                LocalDate.parse(it.date) to it.average_sentiment
            }
        }
    }
}
