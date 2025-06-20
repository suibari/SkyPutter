package com.suibari.skyposter.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.suibari.skyposter.data.repository.NotificationRepoProvider
import com.suibari.skyposter.data.repository.NotificationRepository

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        Log.i("NotificationWorker", "doWork() called: polling notifications")

        val repo = NotificationRepoProvider.getInstance(context)
        repo.fetchNotifications(100)

        return Result.success()
    }
}