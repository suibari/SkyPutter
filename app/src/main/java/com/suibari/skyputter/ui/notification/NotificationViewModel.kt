package com.suibari.skyputter.ui.notification

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.suibari.skyputter.data.repository.NotificationRepository
import com.suibari.skyputter.data.model.PaginatedListViewModel
import com.suibari.skyputter.data.settings.NotificationSettings
import com.suibari.skyputter.ui.type.DisplayNotification
import com.suibari.skyputter.worker.DeviceNotifier
import com.suibari.skyputter.service.NotificationPollingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
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
        NotificationPollingService.startService(repo.context.applicationContext)
    }

    /** バックグラウンド通知ポーリングを停止する */
    fun stopBackgroundPolling() {
        NotificationPollingService.stopService(repo.context.applicationContext)
    }

    /** 手動フェッチ(未読を既読にしてリスト更新) */
    fun markAllAsReadAndReload(limit: Int = 25) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // アプリ側の履歴更新（ユーザーが見たことにする）
                repo.markAllAsRead()

                val (notifs, newCursor) = fetchItems(limit)
                val updated = notifs.map { it.copy(isNew = false) }

                // サーバ側にも既読情報を送る
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

    // notificationの設定変更を監視する
    private var settingsWatcherJob: Job? = null

    fun startSettingsWatcher(context: Context) {
        settingsWatcherJob?.cancel()
        settingsWatcherJob = viewModelScope.launch {
            NotificationSettings.getNotificationPollingEnabled(context)
                .distinctUntilChanged() // 値が変更された時のみ実行
                .collect { enabled ->
                    if (enabled) {
                        startBackgroundPolling()
                    } else {
                        stopBackgroundPolling()
                    }
                }
        }
    }

    fun stopSettingsWatcher() {
        settingsWatcherJob?.cancel()
        settingsWatcherJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopSettingsWatcher()
    }
}