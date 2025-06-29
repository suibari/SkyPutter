package com.suibari.skyputter.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.suibari.skyputter.data.settings.NotificationSettings
import com.suibari.skyputter.ui.main.MainViewModel
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogUtil {
    suspend fun writeDebugLogToFile(
        context: Context,
        mainViewModel: MainViewModel
    ): File {
        val logFile = File(context.cacheDir, "debug_log.txt")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = sdf.format(Date())

        val content = buildString {
            appendLine("[$timestamp] デバッグ情報")
            appendLine(mainViewModel.getDebugLogSnapshot())
            val isPolling = NotificationSettings.getNotificationPollingEnabled(context).first()
            appendLine("通知Polling有効: $isPolling")
        }

        logFile.writeText(content) // 上書き方式が安全
        return logFile
    }

    fun shareLogFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(shareIntent, "ログファイルを共有")
        )
    }
}
