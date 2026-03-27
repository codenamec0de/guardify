package com.uow.guardify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.uow.guardify.util.DataUsageHelper
import com.uow.guardify.util.PreferencesManager

class OnboardingActivity : AppCompatActivity() {

    private lateinit var cbAppsPermission: CheckBox
    private lateinit var cbSmsPermission: CheckBox
    private lateinit var cbUsagePermission: CheckBox
    private lateinit var btnGetStarted: Button

    private var pendingPermissionType: String? = null

    // Permission request launcher for SMS
    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "SMS permissions granted", Toast.LENGTH_SHORT).show()
            cbSmsPermission.isChecked = true
        } else {
            Toast.makeText(this, "SMS permissions denied", Toast.LENGTH_SHORT).show()
            cbSmsPermission.isChecked = false
        }
        checkNextPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        initViews()
        setupListeners()
        checkExistingPermissions()
    }

    override fun onResume() {
        super.onResume()
        // Check if usage permission was granted while in settings
        if (pendingPermissionType == "usage") {
            cbUsagePermission.isChecked = DataUsageHelper.hasUsageStatsPermission(this)
            pendingPermissionType = null
        }
    }

    private fun initViews() {
        cbAppsPermission = findViewById(R.id.cbAppsPermission)
        cbSmsPermission = findViewById(R.id.cbSmsPermission)
        cbUsagePermission = findViewById(R.id.cbUsagePermission)
        btnGetStarted = findViewById(R.id.btnGetStarted)
    }

    private fun setupListeners() {
        btnGetStarted.setOnClickListener {
            requestPermissions()
        }

        // Make the permission items clickable
        findViewById<android.view.View>(R.id.permissionAppsItem).setOnClickListener {
            cbAppsPermission.isChecked = !cbAppsPermission.isChecked
        }

        findViewById<android.view.View>(R.id.permissionSmsItem).setOnClickListener {
            cbSmsPermission.isChecked = !cbSmsPermission.isChecked
        }

        findViewById<android.view.View>(R.id.permissionUsageItem).setOnClickListener {
            cbUsagePermission.isChecked = !cbUsagePermission.isChecked
        }
    }

    private fun checkExistingPermissions() {
        // Check if app query permission is available (always granted on API 30+)
        cbAppsPermission.isChecked = true

        // Check SMS permissions
        val smsGranted = checkSmsPermissions()
        cbSmsPermission.isChecked = smsGranted

        // Check Usage Stats permission
        val usageGranted = DataUsageHelper.hasUsageStatsPermission(this)
        cbUsagePermission.isChecked = usageGranted
    }

    private fun checkSmsPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        // Start the permission request chain
        requestSmsPermissionIfNeeded()
    }

    private fun requestSmsPermissionIfNeeded() {
        if (cbSmsPermission.isChecked && !checkSmsPermissions()) {
            showSmsPermissionExplanation()
        } else {
            requestUsagePermissionIfNeeded()
        }
    }

    private fun requestUsagePermissionIfNeeded() {
        if (cbUsagePermission.isChecked && !DataUsageHelper.hasUsageStatsPermission(this)) {
            showUsagePermissionExplanation()
        } else {
            proceedToMain()
        }
    }

    private fun checkNextPermission() {
        requestUsagePermissionIfNeeded()
    }

    private fun showSmsPermissionExplanation() {
        AlertDialog.Builder(this)
            .setTitle("SMS Permission Required")
            .setMessage("Guardify needs SMS access to scan your messages for potential scam links and phishing attempts.\n\nThis helps protect you from fraudulent messages.")
            .setPositiveButton("Grant") { _, _ ->
                smsPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS
                    )
                )
            }
            .setNegativeButton("Skip") { _, _ ->
                cbSmsPermission.isChecked = false
                checkNextPermission()
            }
            .setCancelable(false)
            .show()
    }

    private fun showUsagePermissionExplanation() {
        AlertDialog.Builder(this)
            .setTitle("Usage Access Required")
            .setMessage("Guardify needs Usage Access to show you how much data each app uses.\n\nYou'll be taken to Settings where you need to find and enable Guardify.\n\nThis is a special permission that cannot be granted automatically.")
            .setPositiveButton("Open Settings") { _, _ ->
                pendingPermissionType = "usage"
                DataUsageHelper.requestUsageStatsPermission(this)
            }
            .setNegativeButton("Skip") { _, _ ->
                cbUsagePermission.isChecked = false
                proceedToMain()
            }
            .setCancelable(false)
            .show()
    }

    private fun proceedToMain() {
        // Save onboarding completion status
        PreferencesManager.setOnboardingComplete(this, true)
        PreferencesManager.setPermissionsGranted(this, cbAppsPermission.isChecked)

        // Navigate to MainActivity with animation
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        finish()
    }
}
