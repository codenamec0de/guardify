package com.uow.guardify.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * Checks installed apps for signs of tampering, modification, or insecure configuration.
 *
 * Inspired by common Android vulnerability vectors:
 *   - Modified/re-signed APKs (signature mismatch with Play Store)
 *   - Debuggable builds running in production
 *   - Cleartext (HTTP) traffic allowed — vulnerable to MITM
 *   - Backup enabled — data extractable via ADB
 *   - Test-only builds on a production device
 *   - Not installed from a trusted store
 */
object AppIntegrityChecker {

    data class IntegrityResult(
        val packageName: String,
        val checks: List<IntegrityCheck>
    ) {
        val passedCount: Int get() = checks.count { it.passed }
        val totalCount: Int get() = checks.size
        val failedChecks: List<IntegrityCheck> get() = checks.filter { !it.passed }
        val hasIssues: Boolean get() = failedChecks.isNotEmpty()

        val overallStatus: Status
            get() = when {
                failedChecks.any { it.severity == Severity.CRITICAL } -> Status.CRITICAL
                failedChecks.any { it.severity == Severity.WARNING } -> Status.WARNING
                failedChecks.isNotEmpty() -> Status.INFO
                else -> Status.CLEAN
            }
    }

    data class IntegrityCheck(
        val name: String,
        val description: String,
        val passed: Boolean,
        val severity: Severity,
        val detail: String
    )

    enum class Severity { CRITICAL, WARNING, INFO }
    enum class Status { CLEAN, INFO, WARNING, CRITICAL }

    /** Known trusted installer packages. */
    private val TRUSTED_INSTALLERS = setOf(
        "com.android.vending",          // Google Play Store
        "com.amazon.venezia",           // Amazon Appstore
        "com.huawei.appmarket",         // Huawei AppGallery
        "com.samsung.android.vending",  // Samsung Galaxy Store
        "com.sec.android.app.samsungapps",
        "com.xiaomi.market",            // Xiaomi GetApps
        "com.oppo.market",              // OPPO App Market
        "com.heytap.market"             // OnePlus/realme store
    )

    /**
     * Run all integrity checks on a single package.
     */
    fun check(context: Context, packageName: String): IntegrityResult {
        val pm = context.packageManager
        val packageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(
                        (PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES).toLong()
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(
                    packageName,
                    PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            return IntegrityResult(packageName, emptyList())
        }

        val appInfo = packageInfo.applicationInfo ?: return IntegrityResult(packageName, emptyList())

        val checks = mutableListOf<IntegrityCheck>()

        checks.add(checkDebuggable(appInfo))
        checks.add(checkCleartextTraffic(appInfo))
        checks.add(checkBackupAllowed(appInfo))
        checks.add(checkTestOnly(appInfo))
        checks.add(checkInstallSource(context, packageName))
        checks.add(checkSignatureCount(packageInfo))

        return IntegrityResult(packageName, checks)
    }

    /**
     * Debuggable flag — production apps should never be debuggable.
     * A debuggable app can be attached to with a debugger, allowing
     * memory inspection, data extraction, and code manipulation.
     */
    private fun checkDebuggable(appInfo: ApplicationInfo): IntegrityCheck {
        val isDebuggable = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return IntegrityCheck(
            name = "Debuggable",
            description = "App is compiled as debuggable",
            passed = !isDebuggable,
            severity = Severity.CRITICAL,
            detail = if (isDebuggable)
                "This app has the debug flag enabled. An attacker could attach a debugger to inspect memory, extract data, or manipulate app behavior."
            else
                "App is not debuggable — this is correct for production."
        )
    }

    /**
     * Cleartext traffic — apps allowing HTTP are vulnerable to MITM attacks.
     * Since Android 9 (API 28), cleartext is blocked by default unless
     * the app explicitly opts in via usesCleartextTraffic or a network security config.
     */
    private fun checkCleartextTraffic(appInfo: ApplicationInfo): IntegrityCheck {
        // On API 23+ ApplicationInfo exposes the flag directly
        val allowsCleartext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (appInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0
        } else {
            true // Can't determine on older APIs, assume allowed
        }

        return IntegrityCheck(
            name = "Cleartext Traffic",
            description = "App allows unencrypted HTTP connections",
            passed = !allowsCleartext,
            severity = Severity.WARNING,
            detail = if (allowsCleartext)
                "This app allows cleartext (HTTP) network traffic. Data sent over HTTP can be intercepted by attackers on the same network (man-in-the-middle attack)."
            else
                "App enforces encrypted HTTPS connections only."
        )
    }

    /**
     * Backup allowed — if android:allowBackup is true, app data can be
     * extracted via ADB backup, potentially exposing tokens, credentials, etc.
     */
    private fun checkBackupAllowed(appInfo: ApplicationInfo): IntegrityCheck {
        val backupAllowed = (appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
        return IntegrityCheck(
            name = "Backup Extraction",
            description = "App data can be extracted via USB backup",
            passed = !backupAllowed,
            severity = Severity.INFO,
            detail = if (backupAllowed)
                "This app allows ADB backup. If someone gains physical access to your device with USB debugging enabled, they could extract the app's private data."
            else
                "App disables ADB backup — data cannot be extracted via USB."
        )
    }

    /**
     * Test-only flag — apps flagged as test builds should not be on
     * a production device. Could indicate a modified or development APK.
     */
    private fun checkTestOnly(appInfo: ApplicationInfo): IntegrityCheck {
        val isTestOnly = (appInfo.flags and ApplicationInfo.FLAG_TEST_ONLY) != 0
        return IntegrityCheck(
            name = "Test Build",
            description = "App is marked as a test/development build",
            passed = !isTestOnly,
            severity = Severity.CRITICAL,
            detail = if (isTestOnly)
                "This app is flagged as a test-only build. It was not intended for production use and may contain debug backdoors or weakened security."
            else
                "App is a production build."
        )
    }

    /**
     * Install source — apps not installed from a trusted store (Play Store, etc.)
     * may have been sideloaded and could be modified versions of legitimate apps.
     */
    private fun checkInstallSource(context: Context, packageName: String): IntegrityCheck {
        val pm = context.packageManager
        val installer = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
        } catch (e: Exception) {
            null
        }

        val isTrusted = installer != null && installer in TRUSTED_INSTALLERS

        return IntegrityCheck(
            name = "Install Source",
            description = "App was not installed from a trusted store",
            passed = isTrusted,
            severity = Severity.WARNING,
            detail = if (isTrusted)
                "Installed from: ${getStoreName(installer!!)}."
            else if (installer != null)
                "Installed by: $installer — not a recognized app store. This app may be a modified version."
            else
                "Unknown install source — this app was likely sideloaded. It could be a modified or repackaged version of a legitimate app."
        )
    }

    /**
     * Signature count — most legitimate apps have a single signing certificate.
     * Multiple signatures or no signatures can indicate tampering.
     */
    private fun checkSignatureCount(packageInfo: PackageInfo): IntegrityCheck {
        val sigCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo
            if (signingInfo?.hasMultipleSigners() == true) {
                signingInfo.apkContentsSigners?.size ?: 0
            } else {
                signingInfo?.signingCertificateHistory?.size ?: 0
            }
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures?.size ?: 0
        }

        val isNormal = sigCount == 1

        return IntegrityCheck(
            name = "APK Signature",
            description = "App signing certificate verification",
            passed = isNormal,
            severity = if (sigCount == 0) Severity.CRITICAL else Severity.INFO,
            detail = when {
                sigCount == 0 -> "No signing certificate found. This APK may be corrupted or tampered with."
                sigCount == 1 -> "Single valid signing certificate — normal for production apps."
                else -> "Multiple signing certificates detected ($sigCount). This is unusual and could indicate the APK was re-signed."
            }
        )
    }

    private fun getStoreName(installer: String): String = when (installer) {
        "com.android.vending" -> "Google Play Store"
        "com.amazon.venezia" -> "Amazon Appstore"
        "com.huawei.appmarket" -> "Huawei AppGallery"
        "com.samsung.android.vending", "com.sec.android.app.samsungapps" -> "Samsung Galaxy Store"
        "com.xiaomi.market" -> "Xiaomi GetApps"
        "com.oppo.market", "com.heytap.market" -> "OPPO/OnePlus Store"
        else -> installer
    }
}
