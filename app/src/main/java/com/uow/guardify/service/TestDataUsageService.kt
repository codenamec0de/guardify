package com.uow.guardify.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.uow.guardify.R
import com.uow.guardify.model.PermissionAlert
import com.uow.guardify.util.AlertStorage
import com.uow.guardify.util.BackgroundUsageMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Foreground service that downloads a random amount of data (1-10 MB) over ~20 seconds
 * to test the background data usage detection pipeline.
 *
 * Flow:
 *  1. User taps "Test" → service starts as foreground with a persistent notification.
 *  2. User minimizes the app.
 *  3. Service downloads data in chunks over ~20 seconds.
 *  4. After download, waits a few seconds for NetworkStatsManager to register the traffic.
 *  5. Queries data usage for Guardify's own UID and creates a test alert.
 *  6. Stops itself.
 */
class TestDataUsageService : Service() {

    companion object {
        const val CHANNEL_ID = "guardify_test_channel"
        const val NOTIFICATION_ID = 9001
        const val ACTION_START = "com.uow.guardify.TEST_START"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START) {
            startForeground(NOTIFICATION_ID, buildNotification("Preparing test download..."))
            scope.launch { runTest() }
        }
        return START_NOT_STICKY
    }

    private suspend fun runTest() {
        val targetBytes = (1..10).random() * 1024 * 1024  // 1-10 MB
        val targetMB = targetBytes / (1024 * 1024)
        updateNotification("Downloading ~${targetMB} MB in background...")

        val startTime = System.currentTimeMillis()

        // Download data in small chunks spread over ~20 seconds
        val chunkCount = 20
        val chunkSize = targetBytes / chunkCount

        var totalDownloaded = 0L
        for (i in 1..chunkCount) {
            try {
                val downloaded = downloadChunk(chunkSize)
                totalDownloaded += downloaded
            } catch (e: Exception) {
                // If one chunk fails, continue with next
            }
            updateNotification("Downloading... ${i * 5}% (~${totalDownloaded / (1024 * 1024)} MB)")
            delay(1000) // ~1 second per chunk → ~20 seconds total
        }

        val endTime = System.currentTimeMillis()
        updateNotification("Download complete. Checking detection...")

        // Wait for NetworkStatsManager to register the traffic
        delay(5000)

        // Query our own app's data usage during the test window
        val detectedBytes = BackgroundUsageMonitor.getDataUsageForPackage(
            this,
            packageName,
            startTime - 60_000, // widen the window slightly for stats lag
            endTime + 60_000
        )

        // Build the test alert
        val alert = PermissionAlert(
            id = UUID.randomUUID().toString(),
            packageName = packageName,
            appName = "Guardify (Test)",
            permissions = listOf("Internet (Test Download)"),
            dataUsedBytes = if (detectedBytes > 0) detectedBytes else totalDownloaded,
            backgroundDurationMs = endTime - startTime,
            timestamp = System.currentTimeMillis()
        )
        AlertStorage.addAlert(this, alert)

        val detectedMB = String.format("%.1f", detectedBytes / (1024.0 * 1024))
        val downloadedMB = String.format("%.1f", totalDownloaded / (1024.0 * 1024))
        updateNotification("Test done! Downloaded: ${downloadedMB} MB, Detected: ${detectedMB} MB")

        delay(3000)
        stopSelf()
    }

    /**
     * Downloads approximately [size] bytes from Cloudflare's speed test endpoint.
     */
    private fun downloadChunk(size: Int): Long {
        var downloaded = 0L
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        try {
            val url = URL("https://speed.cloudflare.com/__down?bytes=$size")
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"

            inputStream = connection.inputStream
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                downloaded += bytesRead
            }
        } finally {
            inputStream?.close()
            connection?.disconnect()
        }
        return downloaded
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Test Data Usage",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background data usage test notifications"
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("Guardify Test")
            .setContentText(text)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
