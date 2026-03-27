package com.uow.guardify.util

import android.content.Context
import com.uow.guardify.data.GuardifyDatabase
import com.uow.guardify.data.entity.AlertEntity
import com.uow.guardify.data.entity.AppSettingsEntity
import com.uow.guardify.model.PermissionAlert
import kotlinx.coroutines.runBlocking

/**
 * Alert storage backed by Room database.
 * Public API is kept synchronous (via runBlocking) so existing callers
 * don't need changes. Background callers (workers, services) should
 * prefer the suspend functions from AlertDao directly.
 */
object AlertStorage {

    private const val KEY_LAST_CHECK_TIME = "last_check_time"
    private const val MAX_ALERTS = 100

    private fun db(context: Context) = GuardifyDatabase.getInstance(context)

    fun getLastCheckTime(context: Context): Long = runBlocking {
        db(context).appSettingsDao().get(KEY_LAST_CHECK_TIME)?.toLongOrNull() ?: 0L
    }

    fun setLastCheckTime(context: Context, time: Long) = runBlocking {
        db(context).appSettingsDao().set(AppSettingsEntity(KEY_LAST_CHECK_TIME, time.toString()))
    }

    fun getAlerts(context: Context): List<PermissionAlert> = runBlocking {
        db(context).alertDao().getAll().map { it.toModel() }
    }

    fun addAlert(context: Context, alert: PermissionAlert) = runBlocking {
        val dao = db(context).alertDao()
        dao.insert(alert.toEntity())
        trimIfNeeded(dao)
    }

    fun addAlerts(context: Context, newAlerts: List<PermissionAlert>) = runBlocking {
        val dao = db(context).alertDao()
        dao.insertAll(newAlerts.map { it.toEntity() })
        trimIfNeeded(dao)
    }

    fun markAsRead(context: Context, alertId: String) = runBlocking {
        db(context).alertDao().markAsRead(alertId)
    }

    fun clearAlerts(context: Context) = runBlocking {
        db(context).alertDao().clearAll()
    }

    fun getUnreadCount(context: Context): Int = runBlocking {
        db(context).alertDao().getUnreadCount()
    }

    private suspend fun trimIfNeeded(dao: com.uow.guardify.data.dao.AlertDao) {
        val count = dao.getCount()
        if (count > MAX_ALERTS) {
            dao.deleteOldest(count - MAX_ALERTS)
        }
    }

    // -------------------------------------------------------------------------
    // Entity <-> Model mapping
    // -------------------------------------------------------------------------

    private fun AlertEntity.toModel() = PermissionAlert(
        id = id,
        packageName = packageName,
        appName = appName,
        permissions = permissions.split(",").filter { it.isNotEmpty() },
        dataUsedBytes = dataUsedBytes,
        backgroundDurationMs = backgroundDurationMs,
        timestamp = timestamp,
        isRead = isRead
    )

    private fun PermissionAlert.toEntity() = AlertEntity(
        id = id,
        packageName = packageName,
        appName = appName,
        permissions = permissions.joinToString(","),
        dataUsedBytes = dataUsedBytes,
        backgroundDurationMs = backgroundDurationMs,
        timestamp = timestamp,
        isRead = isRead
    )
}
