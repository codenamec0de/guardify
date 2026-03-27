package com.uow.guardify.util

import android.content.Context
import android.content.SharedPreferences
import com.uow.guardify.data.GuardifyDatabase
import com.uow.guardify.data.entity.AppSettingsEntity
import kotlinx.coroutines.runBlocking

/**
 * Manages app preferences.
 * Onboarding/permissions flags stay in SharedPreferences (needed before DB is ready at startup).
 * Scan-related data uses Room via AppSettingsDao for consistency with the rest of the database.
 */
object PreferencesManager {

    private const val PREFS_NAME = "guardify_prefs"
    private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    private const val KEY_PERMISSIONS_GRANTED = "permissions_granted"
    private const val KEY_LAST_SCAN_TIME = "last_scan_time"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // -------------------------------------------------------------------------
    // SharedPreferences (startup flags — must be fast and synchronous)
    // -------------------------------------------------------------------------

    fun isOnboardingComplete(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    fun setOnboardingComplete(context: Context, complete: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_COMPLETE, complete).apply()
    }

    fun arePermissionsGranted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PERMISSIONS_GRANTED, false)
    }

    fun setPermissionsGranted(context: Context, granted: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PERMISSIONS_GRANTED, granted).apply()
    }

    // -------------------------------------------------------------------------
    // Room-backed settings
    // -------------------------------------------------------------------------

    fun getLastScanTime(context: Context): Long = runBlocking {
        GuardifyDatabase.getInstance(context).appSettingsDao()
            .get(KEY_LAST_SCAN_TIME)?.toLongOrNull() ?: 0L
    }

    fun setLastScanTime(context: Context, time: Long) = runBlocking {
        GuardifyDatabase.getInstance(context).appSettingsDao()
            .set(AppSettingsEntity(KEY_LAST_SCAN_TIME, time.toString()))
    }

    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
        runBlocking {
            GuardifyDatabase.getInstance(context).appSettingsDao().clearAll()
        }
    }
}
