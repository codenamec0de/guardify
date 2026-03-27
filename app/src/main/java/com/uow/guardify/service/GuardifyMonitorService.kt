package com.uow.guardify.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import com.uow.guardify.MainActivity
import com.uow.guardify.R
import com.uow.guardify.data.GuardifyDatabase
import com.uow.guardify.model.PermissionAlert
import com.uow.guardify.util.AlertStorage
import com.uow.guardify.util.BackgroundUsageMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Always-on foreground service that monitors background data usage.
 *
 * Runs a scan loop every [SCAN_INTERVAL_MS] (10 minutes), checking only
 * apps that the user has enabled in the Monitor tab.
 *
 * Survives app close. Restarted by [BootReceiver] after device reboot.
 * WorkManager remains as a fallback in case this service is killed.
 */
class GuardifyMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "guardify_monitor_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.uow.guardify.MONITOR_START"
        const val ACTION_STOP = "com.uow.guardify.MONITOR_STOP"

        /** Scan interval — 10 minutes */
        private const val SCAN_INTERVAL_MS = 10 * 60 * 1000L

        fun start(context: Context) {
            val intent = Intent(context, GuardifyMonitorService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, GuardifyMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (GuardifyMonitorService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification("Guardify is protecting your device"))
                startMonitorLoop()
            }
        }
        // If killed, Android will restart with the last intent
        return START_STICKY
    }

    private fun startMonitorLoop() {
        scope.launch {
            while (isActive) {
                try {
                    acquireWakeLock()
                    runScan()
                } catch (e: Exception) {
                    // Don't crash the service loop
                } finally {
                    releaseWakeLock()
                }
                delay(SCAN_INTERVAL_MS)
            }
        }
    }

    private suspend fun runScan() {
        val db = GuardifyDatabase.getInstance(this)
        val now = System.currentTimeMillis()

        // Get last check time
        val lastCheckStr = db.appSettingsDao().get("last_check_time")
        var lastCheck = lastCheckStr?.toLongOrNull() ?: 0L
        if (lastCheck == 0L) {
            lastCheck = now - SCAN_INTERVAL_MS
        }

        // Get packages the user has enabled for monitoring
        val monitoredApps = db.monitoredAppDao().getMonitored()
        val monitoredPackages = monitoredApps.map { it.packageName }.toSet()

        // Run the scan
        val allAlerts = BackgroundUsageMonitor.scan(this, lastCheck, now)

        // Filter to only monitored apps
        val filteredAlerts = if (monitoredPackages.isNotEmpty()) {
            allAlerts.filter { it.packageName in monitoredPackages }
        } else {
            // If no monitor preferences set yet, report all (backwards compatible)
            allAlerts
        }

        if (filteredAlerts.isNotEmpty()) {
            AlertStorage.addAlerts(this, filteredAlerts)
            updateNotification(
                "Guardify is protecting your device | ${filteredAlerts.size} new alert${if (filteredAlerts.size > 1) "s" else ""}"
            )

            // Send a heads-up notification for new alerts
            sendAlertNotification(filteredAlerts)
        }

        // Update last check time
        db.appSettingsDao().set(
            com.uow.guardify.data.entity.AppSettingsEntity("last_check_time", now.toString())
        )

        // Update data usage stats for monitored apps
        for (alert in filteredAlerts) {
            db.monitoredAppDao().updateDataUsage(
                alert.packageName, alert.dataUsedBytes, now
            )
        }
    }

    /**
     * Sends a separate heads-up notification when new alerts are detected.
     */
    private fun sendAlertNotification(alerts: List<PermissionAlert>) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create alert channel (high importance for heads-up)
        val alertChannel = NotificationChannel(
            "guardify_alert_channel",
            "Privacy Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when apps use data in the background"
        }
        nm.createNotificationChannel(alertChannel)

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("navigate_to", "alerts")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (alerts.size == 1) {
            "${alerts[0].appName} used data in background"
        } else {
            "${alerts.size} apps used data in background"
        }

        val text = if (alerts.size == 1) {
            "${alerts[0].formattedDataUsed} of data while using ${alerts[0].permissions.firstOrNull() ?: "permissions"}"
        } else {
            alerts.joinToString(", ") { it.appName }
        }

        val notification = Notification.Builder(this, "guardify_alert_channel")
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID + 1, notification)
    }

    // -------------------------------------------------------------------------
    // Notification management
    // -------------------------------------------------------------------------

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Guardify Protection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent notification while Guardify monitors your device"
            setShowBadge(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("Guardify")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    // -------------------------------------------------------------------------
    // Wake lock — keeps CPU alive during scan
    // -------------------------------------------------------------------------

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "guardify:monitor_scan"
        ).apply {
            acquire(60 * 1000L) // 60 second timeout max
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    override fun onDestroy() {
        scope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }
}
