package com.example.skyposter

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.i("NotificationWorker", "doWork() called: polling notifications")
        val repository = NotificationRepository(appContext)

        repository.fetchNotifications(50)

        return Result.success()
    }
}