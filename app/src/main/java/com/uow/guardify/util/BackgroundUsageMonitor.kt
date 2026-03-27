package com.uow.guardify.util

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.net.ConnectivityManager
import android.os.Build
import com.uow.guardify.model.PermissionAlert
import java.util.UUID

/**
 * Detects apps that used data while running in the background and hold sensitive permissions.
 *
 * How it works:
 * 1. Query UsageStatsManager events to find apps that were in the background during the window.
 * 2. For each such app, check if it holds any dangerous (user-granted) sensitive permissions.
 * 3. Query NetworkStatsManager for data usage during that background window.
 * 4. Generate a PermissionAlert for any app that transmitted data from the background.
 */
object BackgroundUsageMonitor {

    /** Minimum data usage (bytes) to generate an alert — avoids noise from keep-alive pings. */
    private const val MIN_DATA_THRESHOLD = 50 * 1024L // 50 KB

    /**
     * Scan for background permission usage in the given time window.
     * Returns a list of new alerts.
     */
    fun scan(context: Context, startTime: Long, endTime: Long): List<PermissionAlert> {
        if (!DataUsageHelper.hasUsageStatsPermission(context)) return emptyList()

        val backgroundApps = getBackgroundAppDurations(context, startTime, endTime)
        if (backgroundApps.isEmpty()) return emptyList()

        val pm = context.packageManager
        val alerts = mutableListOf<PermissionAlert>()

        for ((packageName, durationMs) in backgroundApps) {
            // Skip our own app
            if (packageName == context.packageName) continue

            // Get the app's granted dangerous permissions
            val sensitivePerms = getGrantedSensitivePermissions(pm, packageName)
            if (sensitivePerms.isEmpty()) continue

            // Measure data usage during the window
            val dataUsed = getDataUsageForApp(context, pm, packageName, startTime, endTime)
            if (dataUsed < MIN_DATA_THRESHOLD) continue

            val appName = try {
                val ai = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(ai).toString()
            } catch (e: Exception) {
                packageName
            }

            alerts.add(
                PermissionAlert(
                    id = UUID.randomUUID().toString(),
                    packageName = packageName,
                    appName = appName,
                    permissions = sensitivePerms,
                    dataUsedBytes = dataUsed,
                    backgroundDurationMs = durationMs,
                    timestamp = endTime
                )
            )
        }

        return alerts
    }

    // -------------------------------------------------------------------------
    // Background duration detection via UsageEvents
    // -------------------------------------------------------------------------

    /**
     * Returns a map of packageName -> total background duration (ms) during the window.
     * An app is "in background" between ACTIVITY_PAUSED and the next ACTIVITY_RESUMED.
     */
    private fun getBackgroundAppDurations(
        context: Context,
        startTime: Long,
        endTime: Long
    ): Map<String, Long> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyMap()

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        // Track the last known state per package:  timestamp of move-to-background
        val bgStartTimes = mutableMapOf<String, Long>()
        val bgDurations = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    // App moved to background
                    if (pkg !in bgStartTimes) {
                        bgStartTimes[pkg] = event.timeStamp
                    }
                }
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    // App came to foreground — close the background window
                    val bgStart = bgStartTimes.remove(pkg)
                    if (bgStart != null) {
                        val duration = event.timeStamp - bgStart
                        bgDurations[pkg] = (bgDurations[pkg] ?: 0L) + duration
                    }
                }
            }
        }

        // For apps still in the background at endTime, close the window at endTime
        for ((pkg, bgStart) in bgStartTimes) {
            val duration = endTime - bgStart
            bgDurations[pkg] = (bgDurations[pkg] ?: 0L) + duration
        }

        return bgDurations
    }

    // -------------------------------------------------------------------------
    // Permission checking
    // -------------------------------------------------------------------------

    /**
     * Returns the human-readable names of granted dangerous sensitive permissions for [packageName].
     */
    private fun getGrantedSensitivePermissions(
        pm: PackageManager,
        packageName: String
    ): List<String> {
        val packageInfo: PackageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }
        } catch (e: Exception) {
            return emptyList()
        }

        val permissions = packageInfo.requestedPermissions ?: return emptyList()
        val flags = packageInfo.requestedPermissionsFlags ?: return emptyList()

        return permissions.filterIndexed { index, permission ->
            val isGranted =
                (flags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            if (!isGranted) return@filterIndexed false
            val isDangerous = try {
                val permInfo = pm.getPermissionInfo(permission, 0)
                (permInfo.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE) ==
                        PermissionInfo.PROTECTION_DANGEROUS
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
            isDangerous && PermissionHelper.isSensitivePermission(permission)
        }.map { PermissionHelper.getPermissionName(it) }
    }

    // -------------------------------------------------------------------------
    // Data usage per app
    // -------------------------------------------------------------------------

    /**
     * Public entry point for querying a single app's data usage.
     * Used by TestDataUsageService to verify detection of its own traffic.
     */
    fun getDataUsageForPackage(
        context: Context,
        packageName: String,
        startTime: Long,
        endTime: Long
    ): Long {
        val pm = context.packageManager
        return getDataUsageForApp(context, pm, packageName, startTime, endTime)
    }

    private fun getDataUsageForApp(
        context: Context,
        pm: PackageManager,
        packageName: String,
        startTime: Long,
        endTime: Long
    ): Long {
        val uid = try {
            pm.getApplicationInfo(packageName, 0).uid
        } catch (e: Exception) {
            return 0L
        }

        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
            ?: return 0L

        var total = 0L

        // Mobile data
        try {
            val stats = nsm.queryDetailsForUid(
                ConnectivityManager.TYPE_MOBILE, null, startTime, endTime, uid
            )
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                total += bucket.rxBytes + bucket.txBytes
            }
            stats.close()
        } catch (_: Exception) { }

        // Wi-Fi data
        try {
            val stats = nsm.queryDetailsForUid(
                ConnectivityManager.TYPE_WIFI, "", startTime, endTime, uid
            )
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                total += bucket.rxBytes + bucket.txBytes
            }
            stats.close()
        } catch (_: Exception) { }

        return total
    }
}
