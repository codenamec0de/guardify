package com.uow.guardify

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uow.guardify.adapter.PermissionAdapter
import com.uow.guardify.adapter.TrackerAdapter
import com.uow.guardify.model.AppInfo
import com.uow.guardify.model.RiskLevel
import com.uow.guardify.util.AppScanner
import com.uow.guardify.util.DataUsageHelper
import com.uow.guardify.util.PermissionHelper
import com.uow.guardify.util.TrackerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }

    private lateinit var btnBack: ImageButton
    private lateinit var ivAppIcon: ImageView
    private lateinit var tvAppName: TextView
    private lateinit var tvPackageName: TextView
    private lateinit var tvRiskBadge: TextView
    private lateinit var tvVersion: TextView
    private lateinit var tvPermissionCount: TextView
    private lateinit var tvDataUsage: TextView
    private lateinit var btnManagePermissions: Button
    private lateinit var btnUninstall: Button
    
    // Permission sections
    private lateinit var sectionPrivacyCritical: LinearLayout
    private lateinit var sectionStandardAccess: LinearLayout
    private lateinit var headerStandardAccess: LinearLayout
    private lateinit var tvPrivacyCriticalCount: TextView
    private lateinit var tvStandardAccessCount: TextView
    private lateinit var ivStandardAccessArrow: ImageView
    private lateinit var rvPrivacyCritical: RecyclerView
    private lateinit var rvStandardAccess: RecyclerView
    private lateinit var btnSensitiveInfo: ImageButton
    
    // Trackers
    private lateinit var tvTrackerStatus: TextView
    private lateinit var progressTrackers: ProgressBar
    private lateinit var rvTrackers: RecyclerView

    private var packageName: String = ""
    private var appUid: Int = -1
    private var isStandardAccessExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_detail)

        packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run {
            finish()
            return
        }

        initViews()
        setupListeners()
        loadAppDetails()
        loadDataUsage()
        loadTrackers()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        ivAppIcon = findViewById(R.id.ivAppIcon)
        tvAppName = findViewById(R.id.tvAppName)
        tvPackageName = findViewById(R.id.tvPackageName)
        tvRiskBadge = findViewById(R.id.tvRiskBadge)
        tvVersion = findViewById(R.id.tvVersion)
        tvPermissionCount = findViewById(R.id.tvPermissionCount)
        tvDataUsage = findViewById(R.id.tvDataUsage)
        btnManagePermissions = findViewById(R.id.btnManagePermissions)
        btnUninstall = findViewById(R.id.btnUninstall)
        
        sectionPrivacyCritical = findViewById(R.id.sectionPrivacyCritical)
        sectionStandardAccess = findViewById(R.id.sectionStandardAccess)
        headerStandardAccess = findViewById(R.id.headerStandardAccess)
        tvPrivacyCriticalCount = findViewById(R.id.tvPrivacyCriticalCount)
        tvStandardAccessCount = findViewById(R.id.tvStandardAccessCount)
        ivStandardAccessArrow = findViewById(R.id.ivStandardAccessArrow)
        rvPrivacyCritical = findViewById(R.id.rvPrivacyCritical)
        rvStandardAccess = findViewById(R.id.rvStandardAccess)
        btnSensitiveInfo = findViewById(R.id.btnSensitiveInfo)
        
        tvTrackerStatus = findViewById(R.id.tvTrackerStatus)
        progressTrackers = findViewById(R.id.progressTrackers)
        rvTrackers = findViewById(R.id.rvTrackers)

        rvPrivacyCritical.layoutManager = LinearLayoutManager(this)
        rvStandardAccess.layoutManager = LinearLayoutManager(this)
        rvTrackers.layoutManager = LinearLayoutManager(this)
        
        // Initially hide standard access list
        rvStandardAccess.visibility = View.GONE
        isStandardAccessExpanded = false
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { 
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        btnManagePermissions.setOnClickListener {
            PermissionHelper.openAppPermissionSettings(this, packageName)
        }

        btnUninstall.setOnClickListener {
            uninstallApp()
        }
        
        // Toggle Standard Access section
        headerStandardAccess.setOnClickListener {
            toggleStandardAccess()
        }
        
        // Show sensitive permissions info
        btnSensitiveInfo.setOnClickListener {
            showSensitivePermissionsInfo()
        }
    }
    
    private fun showSensitivePermissionsInfo() {
        val message = """
            |Privacy Critical permissions allow apps to access sensitive data about you:
            |
            |📍 Location — Your physical location
            |📷 Camera — Take photos and videos
            |🎤 Microphone — Record audio
            |📱 Phone — Call logs and phone state
            |💬 SMS — Read and send messages
            |📇 Contacts — Your contact list
            |📅 Calendar — Your schedule
            |📁 Storage — Files on your device
            |🏃 Body Sensors — Health data
            |📞 Call Log — Your call history
            |
            |Apps with many of these permissions can potentially:
            |• Track your movements
            |• Access private conversations
            |• Collect personal information
            |
            |Review apps carefully and revoke unnecessary permissions.
        """.trimMargin()
        
        AlertDialog.Builder(this)
            .setTitle("What are Sensitive Permissions?")
            .setMessage(message)
            .setPositiveButton("Got it", null)
            .show()
    }
    
    private fun toggleStandardAccess() {
        isStandardAccessExpanded = !isStandardAccessExpanded
        
        if (isStandardAccessExpanded) {
            rvStandardAccess.visibility = View.VISIBLE
            ivStandardAccessArrow.rotation = 180f
        } else {
            rvStandardAccess.visibility = View.GONE
            ivStandardAccessArrow.rotation = 0f
        }
    }

    private fun uninstallApp() {
        try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:$packageName")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to uninstall this app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAppDetails() {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }

            val applicationInfo = packageInfo.applicationInfo

            // Store UID for data usage
            applicationInfo?.let {
                appUid = it.uid
                ivAppIcon.setImageDrawable(packageManager.getApplicationIcon(it))
                tvAppName.text = packageManager.getApplicationLabel(it)
            }

            // Set package name
            tvPackageName.text = packageName

            // Set version
            tvVersion.text = packageInfo.versionName ?: "Unknown"

            // Get only GRANTED permissions
            val grantedPermissions = getGrantedPermissions(packageInfo)
            tvPermissionCount.text = grantedPermissions.size.toString()

            // Categorize permissions
            val privacyCritical = grantedPermissions.filter { PermissionHelper.isSensitivePermission(it) }
            val standardAccess = grantedPermissions.filter { !PermissionHelper.isSensitivePermission(it) }

            // Setup Privacy Critical section - only show if there are permissions
            if (privacyCritical.isNotEmpty()) {
                sectionPrivacyCritical.visibility = View.VISIBLE
                tvPrivacyCriticalCount.text = privacyCritical.size.toString()
                rvPrivacyCritical.adapter = PermissionAdapter(privacyCritical)
            } else {
                sectionPrivacyCritical.visibility = View.GONE
            }

            // Setup Standard Access section - only show if there are permissions
            if (standardAccess.isNotEmpty()) {
                sectionStandardAccess.visibility = View.VISIBLE
                tvStandardAccessCount.text = standardAccess.size.toString()
                rvStandardAccess.adapter = PermissionAdapter(standardAccess)
                // Keep it collapsed initially
                rvStandardAccess.visibility = View.GONE
                isStandardAccessExpanded = false
            } else {
                sectionStandardAccess.visibility = View.GONE
            }

            // Calculate and show risk level
            lifecycleScope.launch {
                val apps = withContext(Dispatchers.IO) {
                    AppScanner.scanInstalledApps(this@AppDetailActivity)
                }
                val app = apps.find { it.packageName == packageName }
                app?.let { setRiskBadge(it.riskLevel) }
            }

        } catch (e: PackageManager.NameNotFoundException) {
            Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    /**
     * Returns only dangerous permissions that the user has explicitly granted.
     * Normal permissions (INTERNET, WAKE_LOCK, etc.) are excluded — they are
     * auto-granted at install time and do not reflect any user decision.
     */
    private fun getGrantedPermissions(packageInfo: PackageInfo): List<String> {
        val permissions = packageInfo.requestedPermissions ?: return emptyList()
        val flags = packageInfo.requestedPermissionsFlags ?: return emptyList()

        return permissions.filterIndexed { index, permission ->
            val isGranted = (flags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            if (!isGranted) return@filterIndexed false
            try {
                val permInfo = packageManager.getPermissionInfo(permission, 0)
                (permInfo.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE) == PermissionInfo.PROTECTION_DANGEROUS
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }.distinct()
    }

    private fun setRiskBadge(riskLevel: RiskLevel) {
        when (riskLevel) {
            RiskLevel.HIGH -> {
                tvRiskBadge.text = "HIGH RISK"
                tvRiskBadge.setBackgroundResource(R.drawable.bg_risk_high)
                tvRiskBadge.setTextColor(getColor(R.color.risk_high))
            }
            RiskLevel.MEDIUM -> {
                tvRiskBadge.text = "MEDIUM RISK"
                tvRiskBadge.setBackgroundResource(R.drawable.bg_risk_medium)
                tvRiskBadge.setTextColor(getColor(R.color.risk_medium))
            }
            RiskLevel.LOW -> {
                tvRiskBadge.text = "LOW RISK"
                tvRiskBadge.setBackgroundResource(R.drawable.bg_risk_low)
                tvRiskBadge.setTextColor(getColor(R.color.risk_low))
            }
        }
    }

    private fun loadDataUsage() {
        if (appUid == -1) {
            tvDataUsage.text = "--"
            return
        }

        if (!DataUsageHelper.hasUsageStatsPermission(this)) {
            tvDataUsage.text = "N/A"
            return
        }

        lifecycleScope.launch {
            val dataUsage = withContext(Dispatchers.IO) {
                DataUsageHelper.getAppDataUsage(this@AppDetailActivity, appUid)
            }

            withContext(Dispatchers.Main) {
                dataUsage?.let {
                    tvDataUsage.text = it.getFormattedTotal()
                } ?: run {
                    tvDataUsage.text = "--"
                }
            }
        }
    }

    private fun loadTrackers() {
        tvTrackerStatus.text = getString(R.string.loading_trackers)
        progressTrackers.visibility = View.VISIBLE
        rvTrackers.visibility = View.GONE

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                TrackerRepository.getTrackersForApp(packageName)
            }

            withContext(Dispatchers.Main) {
                progressTrackers.visibility = View.GONE

                result.fold(
                    onSuccess = { trackers ->
                        if (trackers.isEmpty()) {
                            tvTrackerStatus.text = getString(R.string.no_trackers)
                            tvTrackerStatus.setTextColor(getColor(R.color.risk_low))
                            rvTrackers.visibility = View.GONE
                        } else {
                            tvTrackerStatus.text = "${trackers.size} trackers detected"
                            tvTrackerStatus.setTextColor(getColor(R.color.risk_high))
                            rvTrackers.visibility = View.VISIBLE
                            rvTrackers.adapter = TrackerAdapter(trackers)
                        }
                    },
                    onFailure = { error ->
                        tvTrackerStatus.text = getString(R.string.tracker_error)
                        rvTrackers.visibility = View.GONE
                    }
                )
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
