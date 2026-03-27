package com.uow.guardify

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uow.guardify.service.GuardifyMonitorService
import com.uow.guardify.ui.alerts.AlertsFragment
import com.uow.guardify.ui.audit.AuditFragment
import com.uow.guardify.ui.home.HomeFragment
import com.uow.guardify.ui.monitor.MonitorFragment
import com.uow.guardify.ui.settings.SettingsFragment
import com.uow.guardify.util.BatteryOptimizationHelper
import com.uow.guardify.worker.PermissionMonitorWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        setupNavigation()
        schedulePermissionMonitor()
        startMonitorService()

        // Load default fragment or navigate to alerts if coming from notification
        if (savedInstanceState == null) {
            val navigateTo = intent?.getStringExtra("navigate_to")
            if (navigateTo == "alerts") {
                loadFragment(AlertsFragment())
                bottomNavigation.selectedItemId = R.id.navigation_alerts
            } else {
                loadFragment(HomeFragment())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        promptBatteryOptimization()
    }

    private fun setupNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.navigation_audit -> {
                    loadFragment(AuditFragment())
                    true
                }
                R.id.navigation_monitor -> {
                    loadFragment(MonitorFragment())
                    true
                }
                R.id.navigation_alerts -> {
                    loadFragment(AlertsFragment())
                    true
                }
                R.id.navigation_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun navigateToAudit() {
        bottomNavigation.selectedItemId = R.id.navigation_audit
    }

    /**
     * Start the always-on foreground monitoring service.
     */
    private fun startMonitorService() {
        if (!GuardifyMonitorService.isRunning(this)) {
            GuardifyMonitorService.start(this)
        }
    }

    /**
     * Schedule periodic background monitoring every 15 minutes.
     * Acts as a fallback if the foreground service is killed.
     */
    private fun schedulePermissionMonitor() {
        val workRequest = PeriodicWorkRequestBuilder<PermissionMonitorWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PermissionMonitorWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Prompt user to exempt Guardify from battery optimization.
     * Only asks once — if already exempt or already dismissed, does nothing.
     */
    private fun promptBatteryOptimization() {
        if (BatteryOptimizationHelper.isExempt(this)) return

        val prefs = getSharedPreferences("guardify_prefs", MODE_PRIVATE)
        val prompted = prefs.getBoolean("battery_opt_prompted", false)
        if (prompted) return

        prefs.edit().putBoolean("battery_opt_prompted", true).apply()

        AlertDialog.Builder(this)
            .setTitle("Keep Guardify Running")
            .setMessage(
                "For continuous protection, Guardify needs to be exempt from battery optimization. " +
                "This ensures background monitoring keeps running even when the app is closed.\n\n" +
                "Tap \"Allow\" on the next screen."
            )
            .setPositiveButton("Enable") { _, _ ->
                BatteryOptimizationHelper.requestExemption(this)
            }
            .setNegativeButton("Later", null)
            .show()
    }
}
