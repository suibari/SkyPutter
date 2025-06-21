package com.suibari.skyposter.worker

import com.suibari.skyposter.data.repository.NotificationRepository
import com.suibari.skyposter.ui.type.DisplayNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NotificationPoller(
    private val repo: NotificationRepository,
    private val onNewNotifications: (List<DisplayNotification>) -> Unit
) {
    private var isPolling = false
    private val notifiedCids = mutableSetOf<String>()

    fun start() {
        if (isPolling) return
        isPolling = true

        CoroutineScope(Dispatchers.IO).launch {
            while (isPolling) {
                try {
                    val (notifs, _) = repo.fetchNotifications(15)
                    val newNotifs = notifs.filter { it.isNew && it.raw.cid !in notifiedCids }
                    if (newNotifs.isNotEmpty()) {
                        onNewNotifications(newNotifs)
                        notifiedCids.addAll(newNotifs.map { it.raw.cid })
                        if (notifiedCids.size > 1000) {
                            notifiedCids.removeAll(notifiedCids.take(notifiedCids.size - 1000))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(60_000)
            }
        }
    }

    fun stop() {
        isPolling = false
    }
}
