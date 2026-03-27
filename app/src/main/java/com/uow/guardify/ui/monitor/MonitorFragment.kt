package com.uow.guardify.ui.monitor

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uow.guardify.AppDetailActivity
import com.uow.guardify.R
import com.uow.guardify.adapter.MonitoredAppAdapter
import com.uow.guardify.adapter.MonitoredAppItem
import com.uow.guardify.data.GuardifyDatabase
import com.uow.guardify.data.entity.MonitoredAppEntity
import com.uow.guardify.model.AppInfo
import com.uow.guardify.model.RiskLevel
import com.uow.guardify.service.GuardifyMonitorService
import com.uow.guardify.util.AppScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MonitorFragment : Fragment() {

    private lateinit var rvApps: RecyclerView
    private lateinit var tvSummary: TextView
    private lateinit var tvServiceStatus: TextView
    private lateinit var statusDot: View
    private lateinit var btnEnableAll: TextView
    private lateinit var btnDisableAll: TextView
    private lateinit var etSearch: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout

    private lateinit var adapter: MonitoredAppAdapter
    private var allItems: List<MonitoredAppItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_monitor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvApps = view.findViewById(R.id.rvApps)
        tvSummary = view.findViewById(R.id.tvSummary)
        tvServiceStatus = view.findViewById(R.id.tvServiceStatus)
        statusDot = view.findViewById(R.id.statusDot)
        btnEnableAll = view.findViewById(R.id.btnEnableAll)
        btnDisableAll = view.findViewById(R.id.btnDisableAll)
        etSearch = view.findViewById(R.id.etSearch)
        progressBar = view.findViewById(R.id.progressBar)
        emptyState = view.findViewById(R.id.emptyState)

        adapter = MonitoredAppAdapter(
            onToggle = { app, enabled -> onToggleMonitoring(app, enabled) },
            onAppClick = { app -> onAppClicked(app) }
        )
        rvApps.layoutManager = LinearLayoutManager(requireContext())
        rvApps.adapter = adapter

        btnEnableAll.setOnClickListener { setAllMonitored(true) }
        btnDisableAll.setOnClickListener { setAllMonitored(false) }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { filterApps(s?.toString() ?: "") }
        })

        loadApps()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun updateServiceStatus() {
        val running = GuardifyMonitorService.isRunning(requireContext())
        if (running) {
            tvServiceStatus.text = "Protection active"
            statusDot.setBackgroundResource(R.drawable.bg_dot_success)
        } else {
            tvServiceStatus.text = "Protection inactive"
            statusDot.setBackgroundResource(R.drawable.bg_dot_warning)
        }
    }

    private fun loadApps() {
        progressBar.visibility = View.VISIBLE
        rvApps.visibility = View.GONE
        emptyState.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                AppScanner.scanInstalledApps(requireContext())
            }

            if (apps.isEmpty()) {
                progressBar.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
                return@launch
            }

            // Sort: HIGH → MEDIUM → LOW
            val sorted = apps.sortedWith(compareBy {
                when (it.riskLevel) {
                    RiskLevel.HIGH -> 0
                    RiskLevel.MEDIUM -> 1
                    RiskLevel.LOW -> 2
                }
            })

            val db = GuardifyDatabase.getInstance(requireContext())
            val monitorDao = db.monitoredAppDao()

            // Build monitored items — merge with existing DB state
            val items = withContext(Dispatchers.IO) {
                sorted.map { app ->
                    val existing = monitorDao.getByPackage(app.packageName)
                    if (existing == null) {
                        // First time seeing this app — auto-enable if high/medium risk
                        val autoEnable = app.riskLevel != RiskLevel.LOW
                        val entity = MonitoredAppEntity(
                            packageName = app.packageName,
                            appName = app.appName,
                            riskLevel = app.riskLevel.name,
                            isMonitored = autoEnable
                        )
                        monitorDao.insert(entity)
                        MonitoredAppItem(app, autoEnable, 0L)
                    } else {
                        MonitoredAppItem(app, existing.isMonitored, existing.lastDataUsageBytes)
                    }
                }
            }

            allItems = items
            progressBar.visibility = View.GONE
            rvApps.visibility = View.VISIBLE
            adapter.submitList(items)
            updateSummary(items)
        }
    }

    private fun onToggleMonitoring(app: AppInfo, enabled: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            GuardifyDatabase.getInstance(requireContext())
                .monitoredAppDao()
                .setMonitored(app.packageName, enabled)
        }

        // Update local list
        allItems = allItems.map {
            if (it.appInfo.packageName == app.packageName) it.copy(isMonitored = enabled) else it
        }
        val query = etSearch.text?.toString() ?: ""
        if (query.isEmpty()) {
            adapter.submitList(allItems)
        } else {
            filterApps(query)
        }
        updateSummary(allItems)
    }

    private fun setAllMonitored(enabled: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            GuardifyDatabase.getInstance(requireContext())
                .monitoredAppDao()
                .setAllMonitored(enabled)
        }

        allItems = allItems.map { it.copy(isMonitored = enabled) }
        val query = etSearch.text?.toString() ?: ""
        if (query.isEmpty()) {
            adapter.submitList(allItems.toList()) // new list ref to trigger diff
        } else {
            filterApps(query)
        }
        updateSummary(allItems)
    }

    private fun filterApps(query: String) {
        if (query.isEmpty()) {
            adapter.submitList(allItems)
        } else {
            val filtered = allItems.filter {
                it.appInfo.appName.contains(query, ignoreCase = true) ||
                it.appInfo.packageName.contains(query, ignoreCase = true)
            }
            adapter.submitList(filtered)
        }
    }

    private fun updateSummary(items: List<MonitoredAppItem>) {
        val monitored = items.count { it.isMonitored }
        val total = items.size
        tvSummary.text = "Monitoring $monitored of $total apps"
    }

    private fun onAppClicked(app: AppInfo) {
        val intent = Intent(requireContext(), AppDetailActivity::class.java).apply {
            putExtra(AppDetailActivity.EXTRA_PACKAGE_NAME, app.packageName)
        }
        startActivity(intent)
    }
}
