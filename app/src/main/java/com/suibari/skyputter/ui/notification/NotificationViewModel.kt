package com.suibari.skyputter.ui.notification

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.suibari.skyputter.data.repository.NotificationRepository
import com.suibari.skyputter.data.model.PaginatedListViewModel
import com.suibari.skyputter.ui.type.DisplayNotification
import com.suibari.skyputter.worker.DeviceNotifier
import com.suibari.skyputter.service.NotificationPollingService
import kotlinx.coroutines.launch

class NotificationViewModel(
    override val repo: NotificationRepository,
    private val notifier: DeviceNotifier
) : PaginatedListViewModel<DisplayNotification>() {

    /** フェッチ: 通知更新用 */
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
    fun markAllAsReadAndReload(limit: Int = 25) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repo.markAllAsRead()

                val (notifs, newCursor) = fetchItems(limit)
                val updated = notifs.map { it.copy(isNew = false) }

                // フェッチして最新取得通知時刻を更新した後、updateSeen
                repo.updateSeenToLatest()

                _items.clear()
                _items.addAll(updated)
                cursor = newCursor

                updateViewerStatus(updated)
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "markAllAsReadAndReload: error", e)
            } finally {
                _isRefreshing.value = false
            }
        }
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