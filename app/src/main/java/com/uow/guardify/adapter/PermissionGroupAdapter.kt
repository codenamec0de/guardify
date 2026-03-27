package com.uow.guardify.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.uow.guardify.R
import com.uow.guardify.model.AppInfo
import com.uow.guardify.model.PermissionGroup
import com.uow.guardify.model.RiskLevel
import com.uow.guardify.util.PermissionHelper

class PermissionGroupAdapter(
    private val groups: List<PermissionGroup>,
    private val onAppClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<PermissionGroupAdapter.GroupViewHolder>() {

    // Track which groups are expanded
    private val expandedGroups = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_permission_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount(): Int = groups.size

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivGroupIcon: ImageView = itemView.findViewById(R.id.ivGroupIcon)
        private val tvGroupName: TextView = itemView.findViewById(R.id.tvGroupName)
        private val tvGroupSubtitle: TextView = itemView.findViewById(R.id.tvGroupSubtitle)
        private val tvAppCount: TextView = itemView.findViewById(R.id.tvAppCount)
        private val ivExpand: ImageView = itemView.findViewById(R.id.ivExpand)
        private val groupHeader: View = itemView.findViewById(R.id.groupHeader)
        private val appListContainer: LinearLayout = itemView.findViewById(R.id.appListContainer)

        fun bind(group: PermissionGroup) {
            ivGroupIcon.setImageResource(group.iconRes)
            tvGroupName.text = group.displayName

            // Build subtitle from the permission names in this group
            val permNames = group.permissions.map { PermissionHelper.getPermissionName(it) }
            tvGroupSubtitle.text = permNames.joinToString(" \u2022 ")

            tvAppCount.text = "${group.appCount} app${if (group.appCount != 1) "s" else ""}"

            val isExpanded = group.id in expandedGroups
            ivExpand.rotation = if (isExpanded) 180f else 0f
            appListContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE

            if (isExpanded) {
                populateApps(group)
            }

            groupHeader.setOnClickListener {
                if (group.id in expandedGroups) {
                    expandedGroups.remove(group.id)
                } else {
                    expandedGroups.add(group.id)
                }
                notifyItemChanged(bindingAdapterPosition)
            }
        }

        private fun populateApps(group: PermissionGroup) {
            appListContainer.removeAllViews()
            val inflater = LayoutInflater.from(itemView.context)

            for (groupApp in group.apps) {
                val appView = inflater.inflate(
                    R.layout.item_permission_group_app, appListContainer, false
                )

                val ivAppIcon = appView.findViewById<ImageView>(R.id.ivAppIcon)
                val tvAppName = appView.findViewById<TextView>(R.id.tvAppName)
                val tvMatchedPerms = appView.findViewById<TextView>(R.id.tvMatchedPerms)
                val tvRiskBadge = appView.findViewById<TextView>(R.id.tvRiskBadge)

                // App icon
                groupApp.appInfo.icon?.let { ivAppIcon.setImageDrawable(it) }
                    ?: ivAppIcon.setImageResource(R.drawable.ic_shield)

                // App name
                tvAppName.text = groupApp.appInfo.appName

                // Which permissions from this group the app holds
                tvMatchedPerms.text = groupApp.matchedPermissions.joinToString(", ")

                // Risk badge
                val ctx = itemView.context
                when (groupApp.appInfo.riskLevel) {
                    RiskLevel.HIGH -> {
                        tvRiskBadge.text = "HIGH"
                        tvRiskBadge.setBackgroundResource(R.drawable.bg_risk_high)
                        tvRiskBadge.setTextColor(ctx.getColor(R.color.risk_high))
                    }
                    RiskLevel.MEDIUM -> {
                        tvRiskBadge.text = "MED"
                        tvRiskBadge.setBackgroundResource(R.drawable.bg_risk_medium)
                        tvRiskBadge.setTextColor(ctx.getColor(R.color.risk_medium))
                    }
                    RiskLevel.LOW -> {
                        tvRiskBadge.text = "LOW"
                        tvRiskBadge.setBackgroundResource(R.drawable.bg_risk_low)
                        tvRiskBadge.setTextColor(ctx.getColor(R.color.risk_low))
                    }
                }

                appView.setOnClickListener { onAppClick(groupApp.appInfo) }
                appListContainer.addView(appView)
            }
        }
    }
}
