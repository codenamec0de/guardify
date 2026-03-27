package com.uow.guardify.util

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import com.uow.guardify.data.GuardifyDatabase
import com.uow.guardify.data.entity.DeviceCheckEntity

/**
 * Checks device security posture and calculates a 0-100 score.
 *
 * Scoring weights:
 *   Screen lock enabled:       20 pts
 *   Biometric enrolled:        15 pts
 *   Disk encryption:           15 pts
 *   OS version up to date:     15 pts
 *   Developer options OFF:     10 pts
 *   USB debugging OFF:         10 pts
 *   Unknown sources OFF:       10 pts
 *   Guardify protection on:     5 pts
 */
object DeviceSecurityChecker {

    data class SecurityResult(
        val score: Int,
        val screenLockEnabled: Boolean,
        val biometricEnrolled: Boolean,
        val diskEncrypted: Boolean,
        val osUpToDate: Boolean,
        val developerOptionsOff: Boolean,
        val usbDebuggingOff: Boolean,
        val unknownSourcesOff: Boolean,
        val guardifyProtectionOn: Boolean
    ) {
        val items: List<SecurityItem>
            get() = listOf(
                SecurityItem("Screen Lock", "PIN, pattern, or password is set", screenLockEnabled, 20),
                SecurityItem("Biometric Auth", "Fingerprint or face unlock enrolled", biometricEnrolled, 15),
                SecurityItem("Disk Encryption", "Device storage is encrypted", diskEncrypted, 15),
                SecurityItem("OS Version", "Running a recent Android version", osUpToDate, 15),
                SecurityItem("Developer Options", "Developer mode is turned off", developerOptionsOff, 10),
                SecurityItem("USB Debugging", "ADB debugging is turned off", usbDebuggingOff, 10),
                SecurityItem("Unknown Sources", "Sideloading apps is restricted", unknownSourcesOff, 10),
                SecurityItem("Guardify Protection", "Background monitoring is active", guardifyProtectionOn, 5)
            )

        val passedCount: Int get() = items.count { it.passed }
        val totalCount: Int get() = items.size

        val grade: String
            get() = when {
                score >= 90 -> "Excellent"
                score >= 70 -> "Good"
                score >= 50 -> "Fair"
                else -> "At Risk"
            }

        val gradeColor: GradeColor
            get() = when {
                score >= 90 -> GradeColor.GREEN
                score >= 70 -> GradeColor.BLUE
                score >= 50 -> GradeColor.YELLOW
                else -> GradeColor.RED
            }
    }

    data class SecurityItem(
        val name: String,
        val description: String,
        val passed: Boolean,
        val points: Int
    )

    enum class GradeColor { GREEN, BLUE, YELLOW, RED }

    /**
     * Run all security checks and return a composite result.
     */
    fun check(context: Context): SecurityResult {
        val screenLock = checkScreenLock(context)
        val biometric = checkBiometric(context)
        val encryption = checkEncryption(context)
        val osUpToDate = checkOsVersion()
        val devOptionsOff = checkDeveloperOptions(context)
        val usbDebugOff = checkUsbDebugging(context)
        val unknownSourcesOff = checkUnknownSources(context)
        val guardifyOn = com.uow.guardify.service.GuardifyMonitorService.isRunning(context)

        var score = 0
        if (screenLock) score += 20
        if (biometric) score += 15
        if (encryption) score += 15
        if (osUpToDate) score += 15
        if (devOptionsOff) score += 10
        if (usbDebugOff) score += 10
        if (unknownSourcesOff) score += 10
        if (guardifyOn) score += 5

        return SecurityResult(
            score = score,
            screenLockEnabled = screenLock,
            biometricEnrolled = biometric,
            diskEncrypted = encryption,
            osUpToDate = osUpToDate,
            developerOptionsOff = devOptionsOff,
            usbDebuggingOff = usbDebugOff,
            unknownSourcesOff = unknownSourcesOff,
            guardifyProtectionOn = guardifyOn
        )
    }

    /**
     * Run check and save to Room database.
     */
    suspend fun checkAndSave(context: Context): SecurityResult {
        val result = check(context)
        val db = GuardifyDatabase.getInstance(context)
        db.deviceCheckDao().insert(
            DeviceCheckEntity(
                score = result.score,
                screenLockEnabled = result.screenLockEnabled,
                biometricEnrolled = result.biometricEnrolled,
                diskEncrypted = result.diskEncrypted,
                developerOptionsOff = result.developerOptionsOff,
                usbDebuggingOff = result.usbDebuggingOff,
                unknownSourcesOff = result.unknownSourcesOff,
                osUpToDate = result.osUpToDate
            )
        )
        db.deviceCheckDao().pruneOldChecks()
        return result
    }

    // -------------------------------------------------------------------------
    // Individual checks
    // -------------------------------------------------------------------------

    private fun checkScreenLock(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return km.isDeviceSecure
    }

    private fun checkBiometric(context: Context): Boolean {
        val bm = BiometricManager.from(context)
        return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun checkEncryption(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val status = dpm.storageEncryptionStatus
        return status == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE ||
                status == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY ||
                status == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER
    }

    /**
     * Considers the OS "up to date" if running Android 12+ (API 31+).
     * Devices on older versions are flagged as potentially insecure.
     */
    private fun checkOsVersion(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S // Android 12+
    }

    private fun checkDeveloperOptions(context: Context): Boolean {
        return try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
            ) == 0
        } catch (e: Exception) {
            true // If we can't read it, assume it's off
        }
    }

    private fun checkUsbDebugging(context: Context): Boolean {
        return try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED, 0
            ) == 0
        } catch (e: Exception) {
            true
        }
    }

    private fun checkUnknownSources(context: Context): Boolean {
        // On Android 8+, this is per-app. Check the global legacy setting.
        return try {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.INSTALL_NON_MARKET_APPS, 0
            ) == 0
        } catch (e: Exception) {
            true // On newer Android, global toggle doesn't exist — effectively off
        }
    }
}
