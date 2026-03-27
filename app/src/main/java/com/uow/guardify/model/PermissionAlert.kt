package com.uow.guardify.model

/**
 * Represents a background permission usage alert.
 * Generated when an app with sensitive permissions is detected
 * using data while running in the background.
 */
data class PermissionAlert(
    val id: String,
    val packageName: String,
    val appName: String,
    val permissions: List<String>,
    val dataUsedBytes: Long,
    val backgroundDurationMs: Long,
    val timestamp: Long,
    val isRead: Boolean = false
) {
    /** Human-readable data usage (e.g. "2.5 MB") */
    val formattedDataUsed: String
        get() = when {
            dataUsedBytes < 1024 -> "$dataUsedBytes B"
            dataUsedBytes < 1024 * 1024 -> String.format("%.1f KB", dataUsedBytes / 1024.0)
            dataUsedBytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", dataUsedBytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", dataUsedBytes / (1024.0 * 1024 * 1024))
        }

    /** Human-readable duration (e.g. "12 min", "2 hr 5 min") */
    val formattedDuration: String
        get() {
            val totalMinutes = backgroundDurationMs / (60 * 1000)
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return when {
                hours > 0 && minutes > 0 -> "${hours} hr ${minutes} min"
                hours > 0 -> "${hours} hr"
                minutes > 0 -> "${minutes} min"
                else -> "< 1 min"
            }
        }
}
