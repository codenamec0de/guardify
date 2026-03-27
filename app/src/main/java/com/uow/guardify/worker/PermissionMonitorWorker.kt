package com.uow.guardify.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.uow.guardify.util.AlertStorage
import com.uow.guardify.util.BackgroundUsageMonitor

/**
 * Periodic WorkManager worker that scans for apps using data in the background
 * while holding sensitive permissions, and stores the resulting alerts.
 */
class PermissionMonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "permission_monitor"
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        val now = System.currentTimeMillis()

        // Determine scan window: from last check to now
        var lastCheck = AlertStorage.getLastCheckTime(context)
        if (lastCheck == 0L) {
            // First run — look back 15 minutes
            lastCheck = now - 15 * 60 * 1000L
        }

        val alerts = BackgroundUsageMonitor.scan(context, lastCheck, now)

        if (alerts.isNotEmpty()) {
            AlertStorage.addAlerts(context, alerts)
        }

        AlertStorage.setLastCheckTime(context, now)
        return Result.success()
    }
}
