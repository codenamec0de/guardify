package com.uow.guardify.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.uow.guardify.MainActivity
import com.uow.guardify.R
import com.uow.guardify.BreachCheckerActivity
import com.uow.guardify.ScanActivity
import com.uow.guardify.model.AppInfo
import com.uow.guardify.model.RiskLevel
import com.uow.guardify.util.AppScanner
import com.uow.guardify.util.DeviceSecurityChecker
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
    private lateinit var btnBreachChecker: LinearLayout

    // Device Security views
    private lateinit var tvSecurityScore: TextView
    private lateinit var tvSecurityGrade: TextView
    private lateinit var tvSecurityPassed: TextView
    private lateinit var securityChecklist: LinearLayout
    private lateinit var tvToggleChecklist: TextView

    private var scannedApps: List<AppInfo> = emptyList()
    private var checklistExpanded = false

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

        val lastScan = PreferencesManager.getLastScanTime(requireContext())
        if (lastScan > 0) {
            loadCachedSummary()
        } else {
            showReadyToScan()
        }

        runDeviceSecurityCheck()
    }

    override fun onResume() {
        super.onResume()
        val lastScan = PreferencesManager.getLastScanTime(requireContext())
        if (lastScan > 0) {
            loadCachedSummary()
        }
        runDeviceSecurityCheck()
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
        btnBreachChecker = view.findViewById(R.id.btnBreachChecker)
        progressBar = view.findViewById(R.id.progressBar)

        // Device Security
        tvSecurityScore = view.findViewById(R.id.tvSecurityScore)
        tvSecurityGrade = view.findViewById(R.id.tvSecurityGrade)
        tvSecurityPassed = view.findViewById(R.id.tvSecurityPassed)
        securityChecklist = view.findViewById(R.id.securityChecklist)
        tvToggleChecklist = view.findViewById(R.id.tvToggleChecklist)
    }

    private fun setupListeners() {
        btnScan.setOnClickListener {
            val intent = Intent(requireContext(), ScanActivity::class.java)
            startActivity(intent)
            @Suppress("DEPRECATION")
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        btnViewAllApps.setOnClickListener {
            (activity as? MainActivity)?.navigateToAudit()
        }
        btnBreachChecker.setOnClickListener {
            val intent = Intent(requireContext(), BreachCheckerActivity::class.java)
            startActivity(intent)
            @Suppress("DEPRECATION")
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        tvToggleChecklist.setOnClickListener {
            checklistExpanded = !checklistExpanded
            securityChecklist.visibility = if (checklistExpanded) View.VISIBLE else View.GONE
            tvToggleChecklist.text = if (checklistExpanded) "Hide details" else "Show details"
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

    private fun runDeviceSecurityCheck() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                DeviceSecurityChecker.checkAndSave(requireContext())
            }

            // Update score
            tvSecurityScore.text = result.score.toString()
            tvSecurityGrade.text = "${result.grade} \u2022 ${result.passedCount}/${result.totalCount} passed"
            tvSecurityPassed.text = "${result.passedCount} of ${result.totalCount} security checks passed"

            // Set score color
            val scoreColor = when (result.gradeColor) {
                DeviceSecurityChecker.GradeColor.GREEN -> R.color.risk_low
                DeviceSecurityChecker.GradeColor.BLUE -> R.color.info
                DeviceSecurityChecker.GradeColor.YELLOW -> R.color.risk_medium
                DeviceSecurityChecker.GradeColor.RED -> R.color.risk_high
            }
            tvSecurityScore.setTextColor(ContextCompat.getColor(requireContext(), scoreColor))

            // Build checklist
            securityChecklist.removeAllViews()
            for (item in result.items) {
                val itemView = layoutInflater.inflate(
                    R.layout.item_security_check, securityChecklist, false
                )

                val ivIcon = itemView.findViewById<ImageView>(R.id.ivCheckIcon)
                val tvName = itemView.findViewById<TextView>(R.id.tvCheckName)
                val tvDesc = itemView.findViewById<TextView>(R.id.tvCheckDesc)
                val tvPoints = itemView.findViewById<TextView>(R.id.tvCheckPoints)

                ivIcon.setImageResource(
                    if (item.passed) R.drawable.ic_check_circle else R.drawable.ic_warning_circle
                )
                tvName.text = item.name
                tvDesc.text = item.description
                tvPoints.text = if (item.passed) "+${item.points}" else "0/${item.points}"
                tvPoints.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (item.passed) R.color.risk_low else R.color.risk_high
                    )
                )

                securityChecklist.addView(itemView)
            }
        }
    }
}
