package com.uow.guardify.util

import android.content.Context
import android.content.SharedPreferences

object PreferencesManager {
    
    private const val PREFS_NAME = "guardify_prefs"
    private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    private const val KEY_PERMISSIONS_GRANTED = "permissions_granted"
    private const val KEY_LAST_SCAN_TIME = "last_scan_time"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
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
    
    fun getLastScanTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_SCAN_TIME, 0)
    }
    
    fun setLastScanTime(context: Context, time: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_SCAN_TIME, time).apply()
    }
    
    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
