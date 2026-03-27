package com.uow.guardify.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.uow.guardify.AppDetailActivity
import com.uow.guardify.R
import com.uow.guardify.data.GuardifyDatabase
import com.uow.guardify.data.entity.MonitoredAppEntity
import com.uow.guardify.util.AppIntegrityChecker
import com.uow.guardify.util.AppScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Detects new app installations and updates.
 *
 * When a new app is installed:
 *   1. Runs AppScanner to get permissions + risk level
 *   2. Runs AppIntegrityChecker for security flags
 *   3. Auto-adds the app to monitored list (enabled if medium/high risk)
 *   4. Sends a notification summarising the findings
 */
class AppInstallReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "guardify_install_channel"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val packageUri = intent.data ?: return
        val packageName = packageUri.schemeSpecificPart ?: return

        // Skip Guardify itself
        if (packageName == context.packageName) return

        // Only handle new installs and updates
        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        if (action != Intent.ACTION_PACKAGE_ADDED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                processInstall(context, packageName, isReplacing)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processInstall(context: Context, packageName: String, isUpdate: Boolean) {
        // 1. Scan the app
        val pm = context.packageManager
        val packageInfo = try {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
        } catch (e: Exception) {
            return
        }

        val appInfo = AppScanner.scanSinglePackage(context, packageInfo) ?: return

        // 2. Run integrity checks
        val integrity = AppIntegrityChecker.check(context, packageName)

        // 3. Auto-add to monitored apps table
        val db = GuardifyDatabase.getInstance(context)
        val existing = db.monitoredAppDao().getByPackage(packageName)
        if (existing == null) {
            db.monitoredAppDao().insert(
                MonitoredAppEntity(
                    packageName = packageName,
                    appName = appInfo.appName,
                    riskLevel = appInfo.riskLevel.name,
                    isMonitored = appInfo.riskLevel.name != "LOW"
                )
            )
        }

        // 4. Build and send notification
        sendNotification(context, appInfo, integrity, isUpdate)
    }

    private fun sendNotification(
        context: Context,
        appInfo: com.uow.guardify.model.AppInfo,
        integrity: AppIntegrityChecker.IntegrityResult,
        isUpdate: Boolean
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel
        val channel = NotificationChannel(
            CHANNEL_ID,
            "New App Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when new apps are installed or updated"
        }
        nm.createNotificationChannel(channel)

        // Build summary
        val actionWord = if (isUpdate) "updated" else "installed"
        val title = "${appInfo.appName} $actionWord"

        val issues = mutableListOf<String>()
        // Risk level
        when (appInfo.riskLevel.name) {
            "HIGH" -> issues.add("HIGH risk")
            "MEDIUM" -> issues.add("Medium risk")
        }
        // Sensitive permissions
        val sensitiveCount = appInfo.sensitivePermissionCount
        if (sensitiveCount > 0) {
            issues.add("$sensitiveCount sensitive permission${if (sensitiveCount > 1) "s" else ""}")
        }
        // Integrity issues
        val integrityFails = integrity.failedChecks.size
        if (integrityFails > 0) {
            issues.add("$integrityFails integrity warning${if (integrityFails > 1) "s" else ""}")
        }

        val body = if (issues.isEmpty()) {
            "No issues found — app looks safe."
        } else {
            issues.joinToString(" \u2022 ")
        }

        // Tap opens app detail
        val detailIntent = Intent(context, AppDetailActivity::class.java).apply {
            putExtra(AppDetailActivity.EXTRA_PACKAGE_NAME, appInfo.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, appInfo.packageName.hashCode(), detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Use unique notification ID per package
        nm.notify(appInfo.packageName.hashCode(), notification)
    }
}
