package com.uow.guardify.util

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.telephony.TelephonyManager

/**
 * Helper class for tracking data usage per application
 */
object DataUsageHelper {

    data class AppDataUsage(
        val packageName: String,
        val mobileRxBytes: Long,  // Mobile data received
        val mobileTxBytes: Long,  // Mobile data transmitted
        val wifiRxBytes: Long,    // WiFi data received
        val wifiTxBytes: Long,    // WiFi data transmitted
        val totalBytes: Long
    ) {
        fun getFormattedTotal(): String = formatBytes(totalBytes)
        fun getFormattedMobile(): String = formatBytes(mobileRxBytes + mobileTxBytes)
        fun getFormattedWifi(): String = formatBytes(wifiRxBytes + wifiTxBytes)
    }

    /**
     * Check if Usage Access permission is granted
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Open Usage Access settings screen
     */
    fun requestUsageStatsPermission(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * Get data usage for a specific app over a time period
     * @param context Application context
     * @param uid The UID of the app to query
     * @param startTime Start time in milliseconds
     * @param endTime End time in milliseconds
     */
    fun getAppDataUsage(
        context: Context,
        uid: Int,
        startTime: Long = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000), // Last 30 days
        endTime: Long = System.currentTimeMillis()
    ): AppDataUsage? {
        if (!hasUsageStatsPermission(context)) {
            return null
        }

        try {
            val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
            
            var mobileRx = 0L
            var mobileTx = 0L
            var wifiRx = 0L
            var wifiTx = 0L

            // Get mobile data usage
            try {
                val mobileBucket = networkStatsManager.queryDetailsForUid(
                    ConnectivityManager.TYPE_MOBILE,
                    getSubscriberId(context),
                    startTime,
                    endTime,
                    uid
                )
                
                val bucket = NetworkStats.Bucket()
                while (mobileBucket.hasNextBucket()) {
                    mobileBucket.getNextBucket(bucket)
                    mobileRx += bucket.rxBytes
                    mobileTx += bucket.txBytes
                }
                mobileBucket.close()
            } catch (e: Exception) {
                // Mobile data might not be available
            }

            // Get WiFi data usage
            try {
                val wifiBucket = networkStatsManager.queryDetailsForUid(
                    ConnectivityManager.TYPE_WIFI,
                    "",
                    startTime,
                    endTime,
                    uid
                )
                
                val bucket = NetworkStats.Bucket()
                while (wifiBucket.hasNextBucket()) {
                    wifiBucket.getNextBucket(bucket)
                    wifiRx += bucket.rxBytes
                    wifiTx += bucket.txBytes
                }
                wifiBucket.close()
            } catch (e: Exception) {
                // WiFi stats might not be available
            }

            val total = mobileRx + mobileTx + wifiRx + wifiTx
            
            return AppDataUsage(
                packageName = "",
                mobileRxBytes = mobileRx,
                mobileTxBytes = mobileTx,
                wifiRxBytes = wifiRx,
                wifiTxBytes = wifiTx,
                totalBytes = total
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Get data usage for all apps
     */
    fun getAllAppsDataUsage(
        context: Context,
        startTime: Long = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000),
        endTime: Long = System.currentTimeMillis()
    ): Map<Int, AppDataUsage> {
        val result = mutableMapOf<Int, AppDataUsage>()
        
        if (!hasUsageStatsPermission(context)) {
            return result
        }

        try {
            val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
            val uidUsage = mutableMapOf<Int, MutableList<Long>>() // uid -> [mobileRx, mobileTx, wifiRx, wifiTx]

            // Get mobile data usage
            try {
                val mobileStats = networkStatsManager.querySummary(
                    ConnectivityManager.TYPE_MOBILE,
                    getSubscriberId(context),
                    startTime,
                    endTime
                )
                
                val bucket = NetworkStats.Bucket()
                while (mobileStats.hasNextBucket()) {
                    mobileStats.getNextBucket(bucket)
                    val uid = bucket.uid
                    val usage = uidUsage.getOrPut(uid) { mutableListOf(0L, 0L, 0L, 0L) }
                    usage[0] += bucket.rxBytes
                    usage[1] += bucket.txBytes
                }
                mobileStats.close()
            } catch (e: Exception) {
                // Mobile data might not be available
            }

            // Get WiFi data usage
            try {
                val wifiStats = networkStatsManager.querySummary(
                    ConnectivityManager.TYPE_WIFI,
                    "",
                    startTime,
                    endTime
                )
                
                val bucket = NetworkStats.Bucket()
                while (wifiStats.hasNextBucket()) {
                    wifiStats.getNextBucket(bucket)
                    val uid = bucket.uid
                    val usage = uidUsage.getOrPut(uid) { mutableListOf(0L, 0L, 0L, 0L) }
                    usage[2] += bucket.rxBytes
                    usage[3] += bucket.txBytes
                }
                wifiStats.close()
            } catch (e: Exception) {
                // WiFi stats might not be available
            }

            // Convert to AppDataUsage objects
            for ((uid, usage) in uidUsage) {
                val total = usage.sum()
                if (total > 0) {
                    result[uid] = AppDataUsage(
                        packageName = "",
                        mobileRxBytes = usage[0],
                        mobileTxBytes = usage[1],
                        wifiRxBytes = usage[2],
                        wifiTxBytes = usage[3],
                        totalBytes = total
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    private fun getSubscriberId(context: Context): String? {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            null // Return null for privacy - modern Android doesn't allow access without permission
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Format bytes to human readable format
     */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
}
