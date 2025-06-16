package com.example.skyposter

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

abstract class PaginatedListViewModel<T> : ViewModel() {
    protected val _items = mutableStateListOf<T>()
    val items: List<T> = _items
    protected var cursor: String? = null
    protected var isLoading = false

    abstract suspend fun fetchItems(limit: Int, cursor: String? = null): Pair<List<T>, String?>

    fun loadInitialItems(limit: Int = 10) {
        viewModelScope.launch {
            isLoading = true
            val (newItems, newCursor) = fetchItems(limit)
            _items.clear()
            _items.addAll(newItems)
            cursor = newCursor
            isLoading = false
        }
    }

    fun loadMoreItems(limit: Int = 10) {
        if (isLoading || cursor == null) return
        viewModelScope.launch {
            isLoading = true
            val (newItems, newCursor) = fetchItems(limit, cursor)
            _items.addAll(newItems)
            cursor = newCursor
            isLoading = false
        }
    }
}
