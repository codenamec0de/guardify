package com.uow.guardify.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.uow.guardify.R
import com.uow.guardify.model.AppInfo
import com.uow.guardify.model.RiskLevel

/**
 * Data class combining app info with its monitoring state.
 */
data class MonitoredAppItem(
    val appInfo: AppInfo,
    val isMonitored: Boolean,
    val lastDataUsageBytes: Long = 0L
)

class MonitoredAppAdapter(
    private val onToggle: (AppInfo, Boolean) -> Unit,
    private val onAppClick: (AppInfo) -> Unit
) : ListAdapter<MonitoredAppItem, MonitoredAppAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monitored_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvRiskBadge: TextView = itemView.findViewById(R.id.tvRiskBadge)
        private val tvPermissionCount: TextView = itemView.findViewById(R.id.tvPermissionCount)
        private val tvDataUsage: TextView = itemView.findViewById(R.id.tvDataUsage)
        private val switchMonitor: SwitchMaterial = itemView.findViewById(R.id.switchMonitor)

        fun bind(item: MonitoredAppItem) {
            val app = item.appInfo

            tvAppName.text = app.appName

            app.icon?.let { ivAppIcon.setImageDrawable(it) }
                ?: ivAppIcon.setImageResource(R.drawable.ic_shield)

            // Permission count
            val sensitiveCount = app.sensitivePermissionCount
            tvPermissionCount.text = if (sensitiveCount > 0) {
                "$sensitiveCount sensitive permissions"
            } else {
                "${app.permissionCount} permissions"
            }

            // Risk badge
            when (app.riskLevel) {
                RiskLevel.HIGH -> {
                    tvRiskBadge.text = "HIGH"
                    tvRiskBadge.setBackgroundResource(R.drawable.bg_risk_high)
                    tvRiskBadge.setTextColor(itemView.context.getColor(R.color.risk_high))
                }
                RiskLevel.MEDIUM -> {
                    tvRiskBadge.text = "MED"
                    tvRiskBadge.setBackgroundResource(R.drawable.bg_risk_medium)
                    tvRiskBadge.setTextColor(itemView.context.getColor(R.color.risk_medium))
                }
                RiskLevel.LOW -> {
                    tvRiskBadge.text = "LOW"
                    tvRiskBadge.setBackgroundResource(R.drawable.bg_risk_low)
                    tvRiskBadge.setTextColor(itemView.context.getColor(R.color.risk_low))
                }
            }

            // Data usage info
            if (item.lastDataUsageBytes > 0) {
                tvDataUsage.visibility = View.VISIBLE
                tvDataUsage.text = "Last detected: ${formatBytes(item.lastDataUsageBytes)}"
            } else {
                tvDataUsage.visibility = View.GONE
            }

            // Monitor toggle — suppress listener while setting programmatic state
            switchMonitor.setOnCheckedChangeListener(null)
            switchMonitor.isChecked = item.isMonitored
            switchMonitor.setOnCheckedChangeListener { _, isChecked ->
                onToggle(app, isChecked)
            }

            itemView.setOnClickListener { onAppClick(app) }
        }

        private fun formatBytes(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MonitoredAppItem>() {
        override fun areItemsTheSame(old: MonitoredAppItem, new: MonitoredAppItem) =
            old.appInfo.packageName == new.appInfo.packageName

        override fun areContentsTheSame(old: MonitoredAppItem, new: MonitoredAppItem) =
            old.appInfo.packageName == new.appInfo.packageName &&
            old.isMonitored == new.isMonitored &&
            old.lastDataUsageBytes == new.lastDataUsageBytes
    }
}
