package com.suibari.skyposter.ui.notification

import androidx.lifecycle.viewModelScope
import com.suibari.skyposter.data.repository.NotificationRepository
import com.suibari.skyposter.data.model.PaginatedListViewModel
import com.suibari.skyposter.ui.type.DisplayNotification
import com.suibari.skyposter.worker.DeviceNotifier
import com.suibari.skyposter.service.NotificationPollingService
import kotlinx.coroutines.launch

class NotificationViewModel(
    override val repo: NotificationRepository,
    private val notifier: DeviceNotifier
) : PaginatedListViewModel<DisplayNotification>() {

    override suspend fun fetchItems(limit: Int, cursor: String?): Pair<List<DisplayNotification>, String?> {
        val (items, newCursor) = repo.fetchNotifications(limit, cursor)
        updateViewerStatus(items)
        return Pair(items, newCursor)
    }

    /** バックグラウンド通知ポーリングを開始する（1分間隔） */
    fun startBackgroundPolling() {
        NotificationPollingService.startService(repo.context)
    }

    /** バックグラウンド通知ポーリングを停止する */
    fun stopBackgroundPolling() {
        NotificationPollingService.stopService(repo.context)
    }

    /** 手動フェッチ(未読を既読にしてリスト更新) */
    suspend fun fetchNow() {
        repo.markAllAsRead()
        val (newNotifs, newCursor) = repo.fetchNotifications(limit = 15)
        _items.clear()
        _items.addAll(newNotifs)
        updateViewerStatus(newNotifs)
        cursor = newCursor
    }

    /**
     * アプリがフォアグラウンドの時の即座通知用
     * (バックグラウンド通知とは別で、リアルタイム性を重視)
     */
    suspend fun checkForImmediateNotifications() {
        viewModelScope.launch {
            try {
                val newNotifs = repo.fetchNewNotificationsForPolling()
                if (newNotifs.isNotEmpty()) {
                    // UIリストに追加
                    _items.addAll(0, newNotifs)
                    updateViewerStatus(newNotifs)

                    // フォアグラウンドでも通知表示
                    notifier.notify(newNotifs)

                    // 通知済みとしてマーク
                    repo.markAsNotified(newNotifs)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}