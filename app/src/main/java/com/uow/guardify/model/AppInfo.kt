package com.uow.guardify.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val versionName: String,
    val versionCode: Long,
    val installedDate: Long,
    val lastUpdated: Long,
    val permissions: List<String>,
    val riskLevel: RiskLevel,
    val isSystemApp: Boolean
) {
    val sensitivePermissions: List<String>
        get() = permissions.filter { it in SENSITIVE_PERMISSIONS }
    
    val permissionCount: Int
        get() = permissions.size
    
    val sensitivePermissionCount: Int
        get() = sensitivePermissions.size
    
    companion object {
        val SENSITIVE_PERMISSIONS = listOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.READ_SMS",
            "android.permission.SEND_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.READ_PHONE_STATE",
            "android.permission.CALL_PHONE",
            "android.permission.READ_CALENDAR",
            "android.permission.WRITE_CALENDAR",
            "android.permission.BODY_SENSORS",
            "android.permission.ACTIVITY_RECOGNITION",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_MEDIA_AUDIO"
        )
        
        val PERMISSION_DESCRIPTIONS = mapOf(
            "android.permission.CAMERA" to "Camera" to "Take photos and videos",
            "android.permission.RECORD_AUDIO" to "Microphone" to "Record audio",
            "android.permission.ACCESS_FINE_LOCATION" to "Precise Location" to "Access your precise location",
            "android.permission.ACCESS_COARSE_LOCATION" to "Approximate Location" to "Access your approximate location",
            "android.permission.ACCESS_BACKGROUND_LOCATION" to "Background Location" to "Access location in background",
            "android.permission.READ_CONTACTS" to "Contacts" to "Read your contacts",
            "android.permission.WRITE_CONTACTS" to "Modify Contacts" to "Modify your contacts",
            "android.permission.READ_SMS" to "SMS" to "Read your text messages",
            "android.permission.SEND_SMS" to "Send SMS" to "Send text messages",
            "android.permission.RECEIVE_SMS" to "Receive SMS" to "Receive text messages",
            "android.permission.READ_CALL_LOG" to "Call Log" to "Read your call history",
            "android.permission.WRITE_CALL_LOG" to "Modify Call Log" to "Modify call history",
            "android.permission.READ_PHONE_STATE" to "Phone State" to "Read phone status and identity",
            "android.permission.CALL_PHONE" to "Phone Calls" to "Make phone calls",
            "android.permission.READ_CALENDAR" to "Calendar" to "Read calendar events",
            "android.permission.WRITE_CALENDAR" to "Modify Calendar" to "Add or modify calendar events",
            "android.permission.BODY_SENSORS" to "Body Sensors" to "Access body sensors",
            "android.permission.ACTIVITY_RECOGNITION" to "Activity" to "Recognize physical activity",
            "android.permission.READ_EXTERNAL_STORAGE" to "Storage" to "Read storage",
            "android.permission.WRITE_EXTERNAL_STORAGE" to "Modify Storage" to "Write to storage",
            "android.permission.READ_MEDIA_IMAGES" to "Photos" to "Access your photos",
            "android.permission.READ_MEDIA_VIDEO" to "Videos" to "Access your videos",
            "android.permission.READ_MEDIA_AUDIO" to "Music" to "Access your music files",
            "android.permission.INTERNET" to "Internet" to "Full network access"
        )
    }
}

enum class RiskLevel {
    HIGH,
    MEDIUM,
    LOW
}
