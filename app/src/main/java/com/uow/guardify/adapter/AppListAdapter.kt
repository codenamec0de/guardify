package com.uow.guardify.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uow.guardify.R
import com.uow.guardify.model.AppInfo
import com.uow.guardify.model.RiskLevel

class AppListAdapter(
    private val onAppClick: (AppInfo) -> Unit
) : ListAdapter<AppInfo, AppListAdapter.AppViewHolder>(AppDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvPermissionCount: TextView = itemView.findViewById(R.id.tvPermissionCount)
        private val tvRiskBadge: TextView = itemView.findViewById(R.id.tvRiskBadge)
        
        fun bind(app: AppInfo) {
            tvAppName.text = app.appName
            
            // Set app icon
            app.icon?.let {
                ivAppIcon.setImageDrawable(it)
            } ?: run {
                ivAppIcon.setImageResource(R.drawable.ic_shield)
            }
            
            // Set permission count
            val sensitiveCount = app.sensitivePermissionCount
            tvPermissionCount.text = if (sensitiveCount > 0) {
                "$sensitiveCount sensitive permissions"
            } else {
                "${app.permissionCount} permissions"
            }
            
            // Set risk badge
            when (app.riskLevel) {
                RiskLevel.HIGH -> {
                    tvRiskBadge.text = "HIGH RISK"
                    tvRiskBadge.setBackgroundResource(R.drawable.bg_risk_high)
                    tvRiskBadge.setTextColor(itemView.context.getColor(R.color.risk_high))
                }
                RiskLevel.MEDIUM -> {
                    tvRiskBadge.text = "MEDIUM RISK"
                    tvRiskBadge.setBackgroundResource(R.drawable.bg_risk_medium)
                    tvRiskBadge.setTextColor(itemView.context.getColor(R.color.risk_medium))
                }
                RiskLevel.LOW -> {
                    tvRiskBadge.text = "LOW RISK"
                    tvRiskBadge.setBackgroundResource(R.drawable.bg_risk_low)
                    tvRiskBadge.setTextColor(itemView.context.getColor(R.color.risk_low))
                }
            }
            
            itemView.setOnClickListener {
                onAppClick(app)
            }
        }
    }
    
    class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }
        
        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem == newItem
        }
    }
}
