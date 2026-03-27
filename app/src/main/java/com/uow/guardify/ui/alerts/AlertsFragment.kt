package com.uow.guardify.ui.alerts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.uow.guardify.AppDetailActivity
import com.uow.guardify.R
import com.uow.guardify.adapter.AlertAdapter
import com.uow.guardify.model.PermissionAlert
import com.uow.guardify.service.TestDataUsageService
import com.uow.guardify.util.AlertStorage
import com.uow.guardify.util.DataUsageHelper

class AlertsFragment : Fragment() {

    private lateinit var rvAlerts: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var tvAlertCount: TextView
    private lateinit var btnClearAlerts: TextView
    private lateinit var btnTestUsage: ExtendedFloatingActionButton
    private lateinit var alertAdapter: AlertAdapter

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startTestService()
        } else {
            Toast.makeText(requireContext(), "Notification permission needed for the test service", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_alerts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvAlerts = view.findViewById(R.id.rvAlerts)
        emptyState = view.findViewById(R.id.emptyState)
        tvAlertCount = view.findViewById(R.id.tvAlertCount)
        btnClearAlerts = view.findViewById(R.id.btnClearAlerts)
        btnTestUsage = view.findViewById(R.id.btnTestUsage)

        alertAdapter = AlertAdapter { alert -> onAlertClicked(alert) }
        rvAlerts.layoutManager = LinearLayoutManager(requireContext())
        rvAlerts.adapter = alertAdapter

        btnClearAlerts.setOnClickListener { confirmClearAlerts() }
        btnTestUsage.setOnClickListener { onTestButtonClicked() }

        loadAlerts()
    }

    override fun onResume() {
        super.onResume()
        loadAlerts()
    }

    private fun loadAlerts() {
        val alerts = AlertStorage.getAlerts(requireContext())

        if (alerts.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvAlerts.visibility = View.GONE
            tvAlertCount.visibility = View.GONE
            btnClearAlerts.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvAlerts.visibility = View.VISIBLE
            tvAlertCount.visibility = View.VISIBLE
            btnClearAlerts.visibility = View.VISIBLE

            val unread = alerts.count { !it.isRead }
            tvAlertCount.text = if (unread > 0) {
                "$unread new alert${if (unread > 1) "s" else ""}"
            } else {
                "${alerts.size} alert${if (alerts.size > 1) "s" else ""}"
            }

            alertAdapter.submitList(alerts)
        }
    }

    private fun onAlertClicked(alert: PermissionAlert) {
        AlertStorage.markAsRead(requireContext(), alert.id)

        val intent = Intent(requireContext(), AppDetailActivity::class.java).apply {
            putExtra(AppDetailActivity.EXTRA_PACKAGE_NAME, alert.packageName)
        }
        startActivity(intent)

        loadAlerts()
    }

    private fun confirmClearAlerts() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear all alerts?")
            .setMessage("This will remove all background usage alerts.")
            .setPositiveButton("Clear") { _, _ ->
                AlertStorage.clearAlerts(requireContext())
                loadAlerts()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // -------------------------------------------------------------------------
    // Test flow
    // -------------------------------------------------------------------------

    private fun onTestButtonClicked() {
        // Check Usage Stats permission first
        if (!DataUsageHelper.hasUsageStatsPermission(requireContext())) {
            AlertDialog.Builder(requireContext())
                .setTitle("Permission Required")
                .setMessage("Usage Access permission is needed to detect data usage. Grant it in the next screen.")
                .setPositiveButton("Open Settings") { _, _ ->
                    DataUsageHelper.requestUsageStatsPermission(requireContext())
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Test Background Detection")
            .setMessage(
                "This will start a background service that downloads 1-10 MB of data over ~20 seconds.\n\n" +
                "After tapping Start:\n" +
                "1. Minimize the app (press Home)\n" +
                "2. Wait for the notification to say \"Test done!\"\n" +
                "3. Come back to see the alert\n\n" +
                "This proves the data usage detection pipeline works."
            )
            .setPositiveButton("Start Test") { _, _ -> requestNotificationAndStart() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestNotificationAndStart() {
        // On Android 13+ need POST_NOTIFICATIONS permission for the foreground service notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        startTestService()
    }

    private fun startTestService() {
        val intent = Intent(requireContext(), TestDataUsageService::class.java).apply {
            action = TestDataUsageService.ACTION_START
        }
        ContextCompat.startForegroundService(requireContext(), intent)
        Toast.makeText(requireContext(), "Test started! Minimize the app now.", Toast.LENGTH_LONG).show()
    }
}
