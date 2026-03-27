package com.uow.guardify

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uow.guardify.ui.alerts.AlertsFragment
import com.uow.guardify.ui.audit.AuditFragment
import com.uow.guardify.ui.home.HomeFragment
import com.uow.guardify.ui.settings.SettingsFragment
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

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
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
     * Schedule periodic background monitoring every 15 minutes.
     * Uses KEEP policy so it doesn't reset if already enqueued.
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
}
