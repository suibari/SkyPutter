package com.suibari.skyposter.ui.notification

import androidx.lifecycle.viewModelScope
import com.suibari.skyposter.data.repository.NotificationRepository
import com.suibari.skyposter.data.model.PaginatedListViewModel
import com.suibari.skyposter.worker.DeviceNotifier
import com.suibari.skyposter.worker.NotificationPoller
import kotlinx.coroutines.launch


class NotificationViewModel(
    override val repo: NotificationRepository,
    private val notifier: DeviceNotifier
) : PaginatedListViewModel<DisplayNotification>() {

    private val poller = NotificationPoller(repo) { newNotifs ->
        _items.addAll(0, newNotifs)
        viewModelScope.launch {
            updateViewerStatus(newNotifs)
        }
        notifier.notify(newNotifs)
    }

    override suspend fun fetchItems(limit: Int, cursor: String?): Pair<List<DisplayNotification>, String?> {
        val (items, newCursor) = repo.fetchNotifications(limit, cursor)
        updateViewerStatus(items)
        return Pair(items, newCursor)
    }

    /** 通知ポーリングを開始する */
    fun startPolling() {
        poller.start()
    }

    /** 通知ポーリングを停止する */
    fun stopPolling() {
        poller.stop()
    }

    /** 手動フェッチ（未読を既読にしてリスト更新） */
    suspend fun fetchNow() {
        repo.markAllAsRead()
        val (newNotifs, newCursor) = repo.fetchNotifications(limit = 15)
        _items.clear()
        _items.addAll(newNotifs)
        updateViewerStatus(newNotifs)
        cursor = newCursor
    }
}
