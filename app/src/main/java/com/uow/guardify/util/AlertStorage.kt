package com.uow.guardify.util

import android.content.Context
import com.uow.guardify.model.PermissionAlert
import org.json.JSONArray
import org.json.JSONObject

/**
 * SharedPreferences-based storage for permission alerts.
 * Stores alerts as a JSON array, capped at [MAX_ALERTS].
 */
object AlertStorage {

    private const val PREFS_NAME = "guardify_alerts"
    private const val KEY_ALERTS = "alerts_json"
    private const val KEY_LAST_CHECK_TIME = "last_check_time"
    private const val MAX_ALERTS = 100

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLastCheckTime(context: Context): Long =
        getPrefs(context).getLong(KEY_LAST_CHECK_TIME, 0L)

    fun setLastCheckTime(context: Context, time: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_CHECK_TIME, time).apply()
    }

    fun getAlerts(context: Context): List<PermissionAlert> {
        val json = getPrefs(context).getString(KEY_ALERTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i -> fromJson(array.getJSONObject(i)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addAlert(context: Context, alert: PermissionAlert) {
        val alerts = getAlerts(context).toMutableList()
        alerts.add(0, alert) // newest first
        // Cap at MAX_ALERTS
        val capped = if (alerts.size > MAX_ALERTS) alerts.take(MAX_ALERTS) else alerts
        saveAlerts(context, capped)
    }

    fun addAlerts(context: Context, newAlerts: List<PermissionAlert>) {
        val alerts = getAlerts(context).toMutableList()
        alerts.addAll(0, newAlerts)
        val capped = if (alerts.size > MAX_ALERTS) alerts.take(MAX_ALERTS) else alerts
        saveAlerts(context, capped)
    }

    fun markAsRead(context: Context, alertId: String) {
        val alerts = getAlerts(context).map {
            if (it.id == alertId) it.copy(isRead = true) else it
        }
        saveAlerts(context, alerts)
    }

    fun clearAlerts(context: Context) {
        getPrefs(context).edit().remove(KEY_ALERTS).apply()
    }

    fun getUnreadCount(context: Context): Int =
        getAlerts(context).count { !it.isRead }

    private fun saveAlerts(context: Context, alerts: List<PermissionAlert>) {
        val array = JSONArray()
        alerts.forEach { array.put(toJson(it)) }
        getPrefs(context).edit().putString(KEY_ALERTS, array.toString()).apply()
    }

    private fun toJson(alert: PermissionAlert): JSONObject = JSONObject().apply {
        put("id", alert.id)
        put("packageName", alert.packageName)
        put("appName", alert.appName)
        put("permissions", JSONArray(alert.permissions))
        put("dataUsedBytes", alert.dataUsedBytes)
        put("backgroundDurationMs", alert.backgroundDurationMs)
        put("timestamp", alert.timestamp)
        put("isRead", alert.isRead)
    }

    private fun fromJson(obj: JSONObject): PermissionAlert {
        val permsArray = obj.getJSONArray("permissions")
        val permissions = (0 until permsArray.length()).map { permsArray.getString(it) }
        return PermissionAlert(
            id = obj.getString("id"),
            packageName = obj.getString("packageName"),
            appName = obj.getString("appName"),
            permissions = permissions,
            dataUsedBytes = obj.getLong("dataUsedBytes"),
            backgroundDurationMs = obj.getLong("backgroundDurationMs"),
            timestamp = obj.getLong("timestamp"),
            isRead = obj.optBoolean("isRead", false)
        )
    }
}
