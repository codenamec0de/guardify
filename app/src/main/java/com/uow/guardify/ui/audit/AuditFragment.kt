package com.uow.guardify.ui.audit

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uow.guardify.AppDetailActivity
import com.uow.guardify.R
import com.uow.guardify.adapter.AppListAdapter
import com.uow.guardify.adapter.PermissionGroupAdapter
import com.uow.guardify.model.AppInfo
import com.uow.guardify.model.RiskLevel
import com.uow.guardify.util.AppScanner
import com.uow.guardify.util.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuditFragment : Fragment() {

    private lateinit var tvAppsScanned: TextView
    private lateinit var tvHighCount: TextView
    private lateinit var tvMediumCount: TextView
    private lateinit var tvLowCount: TextView
    private lateinit var chipAll: TextView
    private lateinit var chipHigh: TextView
    private lateinit var chipMedium: TextView
    private lateinit var chipLow: TextView
    private lateinit var etSearch: EditText
    private lateinit var rvApps: RecyclerView
    private lateinit var rvCategories: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout

    // View mode toggle
    private lateinit var toggleByApp: TextView
    private lateinit var toggleByCategory: TextView
    private lateinit var filterScrollView: HorizontalScrollView
    private lateinit var searchContainer: FrameLayout

    private lateinit var appAdapter: AppListAdapter
    private var allApps: List<AppInfo> = emptyList()
    private var currentFilter: RiskLevel? = null
    private var searchQuery: String = ""
    private var isCategoryMode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_audit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        setupListeners()
        loadApps()
    }

    private fun initViews(view: View) {
        tvAppsScanned = view.findViewById(R.id.tvAppsScanned)
        tvHighCount = view.findViewById(R.id.tvHighCount)
        tvMediumCount = view.findViewById(R.id.tvMediumCount)
        tvLowCount = view.findViewById(R.id.tvLowCount)
        chipAll = view.findViewById(R.id.chipAll)
        chipHigh = view.findViewById(R.id.chipHigh)
        chipMedium = view.findViewById(R.id.chipMedium)
        chipLow = view.findViewById(R.id.chipLow)
        etSearch = view.findViewById(R.id.etSearch)
        rvApps = view.findViewById(R.id.rvApps)
        rvCategories = view.findViewById(R.id.rvCategories)
        progressBar = view.findViewById(R.id.progressBar)
        emptyState = view.findViewById(R.id.emptyState)

        toggleByApp = view.findViewById(R.id.toggleByApp)
        toggleByCategory = view.findViewById(R.id.toggleByCategory)
        filterScrollView = view.findViewById(R.id.filterScrollView)
        searchContainer = view.findViewById(R.id.searchContainer)
    }

    private fun setupRecyclerView() {
        appAdapter = AppListAdapter { app -> openAppDetail(app) }
        rvApps.layoutManager = LinearLayoutManager(requireContext())
        rvApps.adapter = appAdapter

        rvCategories.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        chipAll.setOnClickListener { setFilter(null) }
        chipHigh.setOnClickListener { setFilter(RiskLevel.HIGH) }
        chipMedium.setOnClickListener { setFilter(RiskLevel.MEDIUM) }
        chipLow.setOnClickListener { setFilter(RiskLevel.LOW) }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString() ?: ""
                applyFilters()
            }
        })

        // View mode toggles
        toggleByApp.setOnClickListener { setViewMode(categoryMode = false) }
        toggleByCategory.setOnClickListener { setViewMode(categoryMode = true) }
    }

    private fun setViewMode(categoryMode: Boolean) {
        isCategoryMode = categoryMode

        // Update toggle styling
        if (categoryMode) {
            toggleByCategory.setBackgroundResource(R.drawable.bg_toggle_active)
            toggleByCategory.setTextColor(requireContext().getColor(R.color.text_primary_dark))
            toggleByCategory.setTypeface(null, android.graphics.Typeface.BOLD)
            toggleByApp.setBackgroundResource(android.R.color.transparent)
            toggleByApp.setTextColor(requireContext().getColor(R.color.text_secondary_dark))
            toggleByApp.setTypeface(null, android.graphics.Typeface.NORMAL)
        } else {
            toggleByApp.setBackgroundResource(R.drawable.bg_toggle_active)
            toggleByApp.setTextColor(requireContext().getColor(R.color.text_primary_dark))
            toggleByApp.setTypeface(null, android.graphics.Typeface.BOLD)
            toggleByCategory.setBackgroundResource(android.R.color.transparent)
            toggleByCategory.setTextColor(requireContext().getColor(R.color.text_secondary_dark))
            toggleByCategory.setTypeface(null, android.graphics.Typeface.NORMAL)
        }

        // Toggle visibility: By App shows filters/search/app list, By Category shows category list
        if (categoryMode) {
            filterScrollView.visibility = View.GONE
            searchContainer.visibility = View.GONE
            rvApps.visibility = View.GONE
            emptyState.visibility = View.GONE
            rvCategories.visibility = View.VISIBLE
            loadCategoryView()
        } else {
            filterScrollView.visibility = View.VISIBLE
            searchContainer.visibility = View.VISIBLE
            rvCategories.visibility = View.GONE
            applyFilters()
        }
    }

    private fun loadApps() {
        showLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            allApps = withContext(Dispatchers.IO) {
                AppScanner.scanInstalledApps(requireContext())
            }
            withContext(Dispatchers.Main) {
                showLoading(false)
                updateSummary()
                updateChipCounts()
                if (isCategoryMode) {
                    loadCategoryView()
                } else {
                    applyFilters()
                }
            }
        }
    }

    private fun loadCategoryView() {
        if (allApps.isEmpty()) return
        val groups = PermissionHelper.buildPermissionGroups(allApps)
        rvCategories.adapter = PermissionGroupAdapter(groups) { app -> openAppDetail(app) }
    }

    private fun updateSummary() {
        tvAppsScanned.text = getString(R.string.apps_scanned, allApps.size)
        val counts = AppScanner.countByRiskLevel(allApps)
        tvHighCount.text = (counts[RiskLevel.HIGH] ?: 0).toString()
        tvMediumCount.text = (counts[RiskLevel.MEDIUM] ?: 0).toString()
        tvLowCount.text = (counts[RiskLevel.LOW] ?: 0).toString()
    }

    private fun updateChipCounts() {
        val counts = AppScanner.countByRiskLevel(allApps)
        chipHigh.text = getString(R.string.high_count, counts[RiskLevel.HIGH] ?: 0)
        chipMedium.text = getString(R.string.medium_count, counts[RiskLevel.MEDIUM] ?: 0)
        chipLow.text = getString(R.string.low_count, counts[RiskLevel.LOW] ?: 0)
    }

    private fun setFilter(riskLevel: RiskLevel?) {
        currentFilter = riskLevel
        updateChipStyles()
        applyFilters()
    }

    private fun updateChipStyles() {
        listOf(chipAll, chipHigh, chipMedium, chipLow).forEach { chip ->
            chip.setBackgroundResource(R.drawable.bg_chip_unselected)
            chip.setTextColor(requireContext().getColor(R.color.text_secondary_dark))
        }
        val selectedChip = when (currentFilter) {
            null -> chipAll
            RiskLevel.HIGH -> chipHigh
            RiskLevel.MEDIUM -> chipMedium
            RiskLevel.LOW -> chipLow
        }
        selectedChip.setBackgroundResource(R.drawable.bg_chip_selected)
        selectedChip.setTextColor(requireContext().getColor(R.color.text_primary_dark))
    }

    private fun applyFilters() {
        var result = allApps
        currentFilter?.let { risk ->
            result = AppScanner.getAppsByRiskLevel(result, risk)
        }
        if (searchQuery.isNotBlank()) {
            result = AppScanner.searchApps(result, searchQuery)
        }
        appAdapter.submitList(result)

        if (result.isEmpty() && allApps.isNotEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvApps.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvApps.visibility = View.VISIBLE
        }
    }

    private fun openAppDetail(app: AppInfo) {
        val intent = Intent(requireContext(), AppDetailActivity::class.java).apply {
            putExtra(AppDetailActivity.EXTRA_PACKAGE_NAME, app.packageName)
        }
        startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            rvApps.visibility = View.GONE
            rvCategories.visibility = View.GONE
        }
    }
}
