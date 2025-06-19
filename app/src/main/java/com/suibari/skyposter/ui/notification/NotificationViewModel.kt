package com.suibari.skyposter.ui.notification

import com.suibari.skyposter.data.repository.NotificationRepository
import com.suibari.skyposter.data.model.PaginatedListViewModel
import com.suibari.skyposter.worker.DeviceNotifier
import com.suibari.skyposter.worker.NotificationPoller

class NotificationViewModel(
    override val repo: NotificationRepository,
    private val notifier: DeviceNotifier
) : PaginatedListViewModel<DisplayNotification>() {

    private val poller = NotificationPoller(repo) { newNotifs ->
        _items.addAll(0, newNotifs)
        notifier.notify(newNotifs)
    }

    override suspend fun fetchItems(limit: Int, cursor: String?): Pair<List<DisplayNotification>, String?> {
        return repo.fetchNotifications(limit, cursor)
    }

    fun startPolling() {
        poller.start()
    }

    fun stopPolling() {
        poller.stop()
    }

    suspend fun fetchNow() {
        repo.markAllAsRead()
        val (newNotifs, newCursor) = repo.fetchNotifications(15)
        _items.clear()
        _items.addAll(newNotifs)
        cursor = newCursor
    }

    private fun updatePostState(uri: String, transform: (DisplayNotification) -> DisplayNotification) {
        val index = _items.indexOfFirst { it.raw.uri == uri }
        if (index != -1) {
            _items[index] = transform(_items[index])
        }
    }
}