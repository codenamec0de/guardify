package com.uow.guardify.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.uow.guardify.R
import com.uow.guardify.model.AppInfo
import com.uow.guardify.model.PermissionGroup
import com.uow.guardify.model.PermissionGroupApp

object PermissionHelper {

    // Exact permission → human-readable name.
    // Adding a new permission here is the ONLY thing needed to give it a unique label.
    private val PERMISSION_NAMES: Map<String, String> = mapOf(
        // Camera
        "android.permission.CAMERA"                      to "Camera",
        // Microphone
        "android.permission.RECORD_AUDIO"                to "Microphone",
        // Location
        "android.permission.ACCESS_FINE_LOCATION"        to "Precise Location",
        "android.permission.ACCESS_COARSE_LOCATION"      to "Approximate Location",
        "android.permission.ACCESS_BACKGROUND_LOCATION"  to "Background Location",
        // Contacts
        "android.permission.READ_CONTACTS"               to "Read Contacts",
        "android.permission.WRITE_CONTACTS"              to "Modify Contacts",
        // SMS
        "android.permission.READ_SMS"                    to "Read SMS",
        "android.permission.SEND_SMS"                    to "Send SMS",
        "android.permission.RECEIVE_SMS"                 to "Receive SMS",
        // Call log
        "android.permission.READ_CALL_LOG"               to "Read Call Log",
        "android.permission.WRITE_CALL_LOG"              to "Modify Call Log",
        // Phone
        "android.permission.READ_PHONE_STATE"            to "Phone State",
        "android.permission.CALL_PHONE"                  to "Make Calls",
        "android.permission.ANSWER_PHONE_CALLS"          to "Answer Calls",
        // Calendar
        "android.permission.READ_CALENDAR"               to "Read Calendar",
        "android.permission.WRITE_CALENDAR"              to "Modify Calendar",
        // Storage (legacy)
        "android.permission.READ_EXTERNAL_STORAGE"       to "Read Storage",
        "android.permission.WRITE_EXTERNAL_STORAGE"      to "Write Storage",
        "android.permission.MANAGE_EXTERNAL_STORAGE"     to "Manage Storage",
        // Storage (Android 13+ granular media)
        "android.permission.READ_MEDIA_IMAGES"           to "Photos",
        "android.permission.READ_MEDIA_VIDEO"            to "Videos",
        "android.permission.READ_MEDIA_AUDIO"            to "Music",
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" to "Selected Media",
        // Sensors
        "android.permission.BODY_SENSORS"                to "Body Sensors",
        "android.permission.BODY_SENSORS_BACKGROUND"     to "Background Body Sensors",
        "android.permission.ACTIVITY_RECOGNITION"        to "Activity Recognition",
        // Bluetooth
        "android.permission.BLUETOOTH"                   to "Bluetooth",
        "android.permission.BLUETOOTH_ADMIN"             to "Bluetooth Admin",
        "android.permission.BLUETOOTH_CONNECT"           to "Bluetooth Connect",
        "android.permission.BLUETOOTH_SCAN"              to "Bluetooth Scan",
        "android.permission.BLUETOOTH_ADVERTISE"         to "Bluetooth Advertise",
        // Network
        "android.permission.INTERNET"                    to "Internet",
        "android.permission.ACCESS_WIFI_STATE"           to "Wi-Fi State",
        "android.permission.CHANGE_WIFI_STATE"           to "Change Wi-Fi",
        "android.permission.ACCESS_NETWORK_STATE"        to "Network State",
        // Notifications
        "android.permission.POST_NOTIFICATIONS"          to "Notifications",
        "android.permission.RECEIVE_BOOT_COMPLETED"      to "Auto-start",
        // Biometrics
        "android.permission.USE_BIOMETRIC"               to "Biometrics",
        "android.permission.USE_FINGERPRINT"             to "Fingerprint"
    )

    // Exact permission → description.
    private val PERMISSION_DESCRIPTIONS: Map<String, String> = mapOf(
        // Camera
        "android.permission.CAMERA"                      to "Take photos and record videos",
        // Microphone
        "android.permission.RECORD_AUDIO"                to "Record audio using the microphone",
        // Location
        "android.permission.ACCESS_FINE_LOCATION"        to "Access your precise GPS location",
        "android.permission.ACCESS_COARSE_LOCATION"      to "Access your approximate location",
        "android.permission.ACCESS_BACKGROUND_LOCATION"  to "Access location while in background",
        // Contacts
        "android.permission.READ_CONTACTS"               to "Read your contacts list",
        "android.permission.WRITE_CONTACTS"              to "Modify your contacts",
        // SMS
        "android.permission.READ_SMS"                    to "Read your text messages",
        "android.permission.SEND_SMS"                    to "Send SMS messages",
        "android.permission.RECEIVE_SMS"                 to "Receive SMS messages",
        // Call log
        "android.permission.READ_CALL_LOG"               to "View your call history",
        "android.permission.WRITE_CALL_LOG"              to "Modify your call history",
        // Phone
        "android.permission.READ_PHONE_STATE"            to "Read phone status and identity",
        "android.permission.CALL_PHONE"                  to "Make phone calls directly",
        "android.permission.ANSWER_PHONE_CALLS"          to "Answer incoming phone calls",
        // Calendar
        "android.permission.READ_CALENDAR"               to "Read calendar events",
        "android.permission.WRITE_CALENDAR"              to "Create or modify calendar events",
        // Storage (legacy)
        "android.permission.READ_EXTERNAL_STORAGE"       to "Access photos, media, and files",
        "android.permission.WRITE_EXTERNAL_STORAGE"      to "Modify or delete files",
        "android.permission.MANAGE_EXTERNAL_STORAGE"     to "Full access to all files on device",
        // Storage (Android 13+ granular media)
        "android.permission.READ_MEDIA_IMAGES"           to "Access photos and images",
        "android.permission.READ_MEDIA_VIDEO"            to "Access video files",
        "android.permission.READ_MEDIA_AUDIO"            to "Access music and audio files",
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" to "Access user-selected media",
        // Sensors
        "android.permission.BODY_SENSORS"                to "Access body sensor data",
        "android.permission.BODY_SENSORS_BACKGROUND"     to "Access body sensors in background",
        "android.permission.ACTIVITY_RECOGNITION"        to "Recognize physical activity",
        // Bluetooth
        "android.permission.BLUETOOTH"                   to "Connect to paired Bluetooth devices",
        "android.permission.BLUETOOTH_ADMIN"             to "Discover and pair Bluetooth devices",
        "android.permission.BLUETOOTH_CONNECT"           to "Connect to Bluetooth devices",
        "android.permission.BLUETOOTH_SCAN"              to "Scan for nearby Bluetooth devices",
        "android.permission.BLUETOOTH_ADVERTISE"         to "Advertise via Bluetooth",
        // Network
        "android.permission.INTERNET"                    to "Full network access",
        "android.permission.ACCESS_WIFI_STATE"           to "View Wi-Fi connections",
        "android.permission.CHANGE_WIFI_STATE"           to "Connect and disconnect from Wi-Fi",
        "android.permission.ACCESS_NETWORK_STATE"        to "View network connections",
        // Notifications / system
        "android.permission.POST_NOTIFICATIONS"          to "Show notifications",
        "android.permission.RECEIVE_BOOT_COMPLETED"      to "Start automatically on device boot",
        // Biometrics
        "android.permission.USE_BIOMETRIC"               to "Authenticate using biometrics",
        "android.permission.USE_FINGERPRINT"             to "Authenticate using fingerprint"
    )

    // Exact set of sensitive permissions. Checked first; keyword fallback handles unknowns.
    private val SENSITIVE_PERMISSION_SET: Set<String> = setOf(
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
        "android.permission.ANSWER_PHONE_CALLS",
        "android.permission.READ_CALENDAR",
        "android.permission.WRITE_CALENDAR",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.READ_MEDIA_AUDIO",
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
        "android.permission.BODY_SENSORS",
        "android.permission.BODY_SENSORS_BACKGROUND",
        "android.permission.ACTIVITY_RECOGNITION",
        "android.permission.USE_BIOMETRIC",
        "android.permission.USE_FINGERPRINT"
    )

    // Fallback keyword → label for permissions not in the map above.
    // Ordered from most specific to least specific.
    private val FALLBACK_NAME_KEYWORDS: List<Pair<String, String>> = listOf(
        "RECORD_AUDIO"    to "Microphone",
        "CALL_LOG"        to "Call Log",
        "FINE_LOCATION"   to "Precise Location",
        "COARSE_LOCATION" to "Approximate Location",
        "BACKGROUND_LOCATION" to "Background Location",
        "LOCATION"        to "Location",
        "CAMERA"          to "Camera",
        "CONTACTS"        to "Contacts",
        "CALL_PHONE"      to "Make Calls",
        "PHONE_STATE"     to "Phone State",
        "PHONE"           to "Phone",
        "CALENDAR"        to "Calendar",
        "EXTERNAL_STORAGE" to "Storage",
        "MEDIA_IMAGES"    to "Photos",
        "MEDIA_VIDEO"     to "Videos",
        "MEDIA_AUDIO"     to "Music",
        "MEDIA"           to "Media",
        "STORAGE"         to "Storage",
        "SENSORS"         to "Sensors",
        "ACTIVITY"        to "Activity",
        "BLUETOOTH"       to "Bluetooth",
        "WIFI"            to "Wi-Fi",
        "INTERNET"        to "Internet",
        "NOTIFICATION"    to "Notifications",
        "BIOMETRIC"       to "Biometrics",
        "FINGERPRINT"     to "Fingerprint"
    )

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Returns a unique, human-readable label for [permission]. */
    fun getPermissionName(permission: String): String {
        PERMISSION_NAMES[permission]?.let { return it }
        FALLBACK_NAME_KEYWORDS.forEach { (keyword, label) ->
            if (permission.contains(keyword)) return label
        }
        return permission.substringAfterLast(".")
            .replace("_", " ")
            .lowercase()
            .replaceFirstChar { it.uppercase() }
    }

    /** Returns a human-readable description for [permission]. */
    fun getPermissionDescription(permission: String): String {
        PERMISSION_DESCRIPTIONS[permission]?.let { return it }
        return "Access ${getPermissionName(permission).lowercase()}"
    }

    /** Returns true if [permission] is considered sensitive/dangerous. */
    fun isSensitivePermission(permission: String): Boolean {
        if (permission in SENSITIVE_PERMISSION_SET) return true
        // Fallback for permissions not yet in the set
        val sensitiveKeywords = listOf(
            "CAMERA", "RECORD_AUDIO", "LOCATION", "CONTACTS", "SMS",
            "CALL_LOG", "CALL_PHONE", "PHONE_STATE", "CALENDAR",
            "STORAGE", "MEDIA", "SENSORS", "ACTIVITY_RECOGNITION",
            "BIOMETRIC", "FINGERPRINT"
        )
        return sensitiveKeywords.any { permission.contains(it) }
    }

    // Open app settings
    fun openAppSettings(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // Open app permission settings directly
    fun openAppPermissionSettings(context: Context, packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openAppSettings(context, packageName)
        }
    }

    // Uninstall app
    fun uninstallApp(context: Context, packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // -------------------------------------------------------------------------
    // Permission group definitions
    // -------------------------------------------------------------------------

    /** Ordered list of permission category groups used by the segmented audit view. */
    fun getPermissionGroups(): List<PermissionGroup> = listOf(
        PermissionGroup(
            id = "location",
            displayName = "Location",
            iconRes = R.drawable.ic_group_location,
            permissions = listOf(
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_BACKGROUND_LOCATION"
            )
        ),
        PermissionGroup(
            id = "camera_media",
            displayName = "Camera & Media",
            iconRes = R.drawable.ic_group_camera,
            permissions = listOf(
                "android.permission.CAMERA",
                "android.permission.READ_MEDIA_IMAGES",
                "android.permission.READ_MEDIA_VIDEO",
                "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
            )
        ),
        PermissionGroup(
            id = "microphone",
            displayName = "Microphone",
            iconRes = R.drawable.ic_group_mic,
            permissions = listOf(
                "android.permission.RECORD_AUDIO"
            )
        ),
        PermissionGroup(
            id = "contacts",
            displayName = "Contacts",
            iconRes = R.drawable.ic_group_contacts,
            permissions = listOf(
                "android.permission.READ_CONTACTS",
                "android.permission.WRITE_CONTACTS"
            )
        ),
        PermissionGroup(
            id = "phone_calls",
            displayName = "Phone & Calls",
            iconRes = R.drawable.ic_group_phone,
            permissions = listOf(
                "android.permission.READ_PHONE_STATE",
                "android.permission.CALL_PHONE",
                "android.permission.ANSWER_PHONE_CALLS",
                "android.permission.READ_CALL_LOG",
                "android.permission.WRITE_CALL_LOG"
            )
        ),
        PermissionGroup(
            id = "messaging",
            displayName = "Messaging",
            iconRes = R.drawable.ic_group_sms,
            permissions = listOf(
                "android.permission.READ_SMS",
                "android.permission.SEND_SMS",
                "android.permission.RECEIVE_SMS"
            )
        ),
        PermissionGroup(
            id = "calendar",
            displayName = "Calendar",
            iconRes = R.drawable.ic_group_calendar,
            permissions = listOf(
                "android.permission.READ_CALENDAR",
                "android.permission.WRITE_CALENDAR"
            )
        ),
        PermissionGroup(
            id = "storage",
            displayName = "Storage & Files",
            iconRes = R.drawable.ic_group_storage,
            permissions = listOf(
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.MANAGE_EXTERNAL_STORAGE",
                "android.permission.READ_MEDIA_AUDIO"
            )
        ),
        PermissionGroup(
            id = "body_activity",
            displayName = "Body & Activity",
            iconRes = R.drawable.ic_group_body,
            permissions = listOf(
                "android.permission.BODY_SENSORS",
                "android.permission.BODY_SENSORS_BACKGROUND",
                "android.permission.ACTIVITY_RECOGNITION"
            )
        )
    )

    /**
     * Build permission groups populated with apps that hold matching permissions.
     * Groups with zero matching apps are excluded.
     */
    fun buildPermissionGroups(apps: List<AppInfo>): List<PermissionGroup> {
        return getPermissionGroups().mapNotNull { group ->
            val groupApps = apps.mapNotNull { app ->
                val matched = app.permissions.filter { it in group.permissions }
                if (matched.isNotEmpty()) {
                    PermissionGroupApp(
                        appInfo = app,
                        matchedPermissions = matched.map { getPermissionName(it) }
                    )
                } else null
            }
            if (groupApps.isNotEmpty()) {
                group.copy().also { it.apps = groupApps }
            } else null
        }
    }

    // Get permission icon resource
    fun getPermissionIcon(permission: String): String {
        return when {
            permission.contains("CAMERA")      -> "camera"
            permission.contains("RECORD_AUDIO") -> "mic"
            permission.contains("LOCATION")    -> "location_on"
            permission.contains("CONTACTS")    -> "contacts"
            permission.contains("SMS")         -> "sms"
            permission.contains("CALL_LOG")    -> "call"
            permission.contains("PHONE")       -> "phone"
            permission.contains("CALENDAR")    -> "calendar_today"
            permission.contains("STORAGE") || permission.contains("MEDIA") -> "folder"
            permission.contains("SENSORS")     -> "sensors"
            permission.contains("INTERNET")    -> "wifi"
            permission.contains("BLUETOOTH")   -> "bluetooth"
            permission.contains("NOTIFICATION") -> "notifications"
            else                               -> "security"
        }
    }
}
