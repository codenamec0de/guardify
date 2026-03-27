package com.uow.guardify.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import com.uow.guardify.model.AppInfo
import com.uow.guardify.model.RiskLevel

object AppScanner {
    
    fun scanInstalledApps(context: Context, includeSystemApps: Boolean = false): List<AppInfo> {
        val packageManager = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }
        
        return packages
            .filter { packageInfo ->
                if (includeSystemApps) {
                    true
                } else {
                    // Filter out system apps
                    (packageInfo.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) ?: 0) == 0
                }
            }
            .mapNotNull { packageInfo ->
                try {
                    createAppInfo(packageManager, packageInfo)
                } catch (e: Exception) {
                    null
                }
            }
            // Sort by sensitive permission count (most first), then by risk level
            .sortedWith(compareByDescending<AppInfo> { 
                it.permissions.count { perm -> perm in AppInfo.SENSITIVE_PERMISSIONS }
            }.thenByDescending { it.riskLevel.ordinal })
    }
    
    private fun createAppInfo(packageManager: PackageManager, packageInfo: PackageInfo): AppInfo {
        val applicationInfo = packageInfo.applicationInfo
        val appName = applicationInfo?.let { 
            packageManager.getApplicationLabel(it).toString() 
        } ?: packageInfo.packageName
        
        val icon = try {
            applicationInfo?.let { packageManager.getApplicationIcon(it) }
        } catch (e: Exception) {
            null
        }
        
        // Get only GRANTED permissions, remove duplicates
        val grantedPermissions = getGrantedPermissions(packageManager, packageInfo)
        val isSystemApp = (applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) ?: 0) != 0
        
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        
        return AppInfo(
            packageName = packageInfo.packageName,
            appName = appName,
            icon = icon,
            versionName = packageInfo.versionName ?: "Unknown",
            versionCode = versionCode,
            installedDate = packageInfo.firstInstallTime,
            lastUpdated = packageInfo.lastUpdateTime,
            permissions = grantedPermissions,
            riskLevel = calculateRiskLevel(grantedPermissions),
            isSystemApp = isSystemApp
        )
    }
    
    /**
     * Returns only dangerous permissions that the user has explicitly granted.
     * Normal permissions (INTERNET, WAKE_LOCK, etc.) are excluded — they are
     * auto-granted at install time and do not reflect any user decision.
     */
    private fun getGrantedPermissions(packageManager: PackageManager, packageInfo: PackageInfo): List<String> {
        val permissions = packageInfo.requestedPermissions ?: return emptyList()
        val flags = packageInfo.requestedPermissionsFlags ?: return emptyList()

        return permissions.filterIndexed { index, permission ->
            val isGranted = (flags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            if (!isGranted) return@filterIndexed false
            try {
                val permInfo = packageManager.getPermissionInfo(permission, 0)
                (permInfo.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE) == PermissionInfo.PROTECTION_DANGEROUS
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }.distinct()
    }
    
    /**
     * Count sensitive permissions for an app
     */
    fun countSensitivePermissions(permissions: List<String>): Int {
        return permissions.count { it in AppInfo.SENSITIVE_PERMISSIONS }
    }
    
    private fun calculateRiskLevel(permissions: List<String>): RiskLevel {
        val sensitivePermissions = permissions.filter { it in AppInfo.SENSITIVE_PERMISSIONS }
        val hasInternet = permissions.contains("android.permission.INTERNET")
        
        // High risk: 3+ sensitive permissions + internet access
        // or has access to SMS/Calls/Location in background
        val highRiskPermissions = listOf(
            "android.permission.READ_SMS",
            "android.permission.SEND_SMS",
            "android.permission.READ_CALL_LOG",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.RECORD_AUDIO",
            "android.permission.CAMERA"
        )
        
        val hasHighRiskPermission = permissions.any { it in highRiskPermissions }
        
        return when {
            sensitivePermissions.size >= 3 && hasInternet && hasHighRiskPermission -> RiskLevel.HIGH
            sensitivePermissions.size >= 2 && hasInternet -> RiskLevel.MEDIUM
            sensitivePermissions.isNotEmpty() -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }
    
    fun getAppsByRiskLevel(apps: List<AppInfo>, riskLevel: RiskLevel): List<AppInfo> {
        return apps.filter { it.riskLevel == riskLevel }
    }
    
    fun countByRiskLevel(apps: List<AppInfo>): Map<RiskLevel, Int> {
        return mapOf(
            RiskLevel.HIGH to apps.count { it.riskLevel == RiskLevel.HIGH },
            RiskLevel.MEDIUM to apps.count { it.riskLevel == RiskLevel.MEDIUM },
            RiskLevel.LOW to apps.count { it.riskLevel == RiskLevel.LOW }
        )
    }
    
    fun searchApps(apps: List<AppInfo>, query: String): List<AppInfo> {
        if (query.isBlank()) return apps
        val lowerQuery = query.lowercase()
        return apps.filter { 
            it.appName.lowercase().contains(lowerQuery) ||
            it.packageName.lowercase().contains(lowerQuery)
        }
    }
}
