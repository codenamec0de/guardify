package com.uow.guardify.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.uow.guardify.MainActivity
import com.uow.guardify.R
import com.uow.guardify.ScanActivity
import com.uow.guardify.model.AppInfo
import com.uow.guardify.model.RiskLevel
import com.uow.guardify.util.AppScanner
import com.uow.guardify.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var cardScanStatus: CardView
    private lateinit var tvScanTitle: TextView
    private lateinit var tvAppsScanned: TextView
    private lateinit var tvHighCount: TextView
    private lateinit var tvMediumCount: TextView
    private lateinit var tvLowCount: TextView
    private lateinit var btnScan: Button
    private lateinit var btnViewAllApps: LinearLayout
    private lateinit var progressBar: ProgressBar

    private var scannedApps: List<AppInfo> = emptyList()
    private var hasScanned = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupListeners()
        
        // Check if we've scanned before (from preferences)
        val lastScan = PreferencesManager.getLastScanTime(requireContext())
        if (lastScan > 0) {
            // Load cached data without showing loading state
            loadCachedSummary()
        } else {
            // Show ready to scan state
            showReadyToScan()
        }
    }

    override fun onResume() {
        super.onResume()
        // Only reload if we've previously scanned
        val lastScan = PreferencesManager.getLastScanTime(requireContext())
        if (lastScan > 0) {
            loadCachedSummary()
        }
    }

    private fun initViews(view: View) {
        cardScanStatus = view.findViewById(R.id.cardScanStatus)
        tvScanTitle = view.findViewById(R.id.tvScanTitle)
        tvAppsScanned = view.findViewById(R.id.tvAppsScanned)
        tvHighCount = view.findViewById(R.id.tvHighCount)
        tvMediumCount = view.findViewById(R.id.tvMediumCount)
        tvLowCount = view.findViewById(R.id.tvLowCount)
        btnScan = view.findViewById(R.id.btnScan)
        btnViewAllApps = view.findViewById(R.id.btnViewAllApps)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        btnScan.setOnClickListener { 
            // Launch ScanActivity with animation
            val intent = Intent(requireContext(), ScanActivity::class.java)
            startActivity(intent)
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        btnViewAllApps.setOnClickListener {
            (activity as? MainActivity)?.navigateToAudit()
        }
    }
    
    private fun showReadyToScan() {
        progressBar.visibility = View.GONE
        tvScanTitle.text = "Ready to Scan"
        tvAppsScanned.text = "Tap the button below to scan your apps"
        tvHighCount.text = "-"
        tvMediumCount.text = "-"
        tvLowCount.text = "-"
    }

    private fun loadCachedSummary() {
        progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            scannedApps = withContext(Dispatchers.IO) {
                AppScanner.scanInstalledApps(requireContext())
            }
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                updateUI()
            }
        }
    }

    private fun updateUI() {
        tvScanTitle.text = getString(R.string.scan_complete)
        tvAppsScanned.text = getString(R.string.apps_scanned, scannedApps.size)
        val riskCounts = AppScanner.countByRiskLevel(scannedApps)
        tvHighCount.text = (riskCounts[RiskLevel.HIGH] ?: 0).toString()
        tvMediumCount.text = (riskCounts[RiskLevel.MEDIUM] ?: 0).toString()
        tvLowCount.text = (riskCounts[RiskLevel.LOW] ?: 0).toString()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}
