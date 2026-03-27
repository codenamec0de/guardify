package com.uow.guardify.adapter

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uow.guardify.R
import com.uow.guardify.model.PermissionAlert

class AlertAdapter(
    private val onAlertClick: (PermissionAlert) -> Unit
) : ListAdapter<PermissionAlert, AlertAdapter.AlertViewHolder>(AlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAlertAppIcon)
        private val tvAlertTitle: TextView = itemView.findViewById(R.id.tvAlertTitle)
        private val tvAlertMessage: TextView = itemView.findViewById(R.id.tvAlertMessage)
        private val tvAlertTime: TextView = itemView.findViewById(R.id.tvAlertTime)
        private val tvAlertDataBadge: TextView = itemView.findViewById(R.id.tvAlertDataBadge)
        private val indicatorUnread: View = itemView.findViewById(R.id.indicatorUnread)

        fun bind(alert: PermissionAlert) {
            // App icon
            try {
                val pm = itemView.context.packageManager
                val icon = pm.getApplicationIcon(alert.packageName)
                ivAppIcon.setImageDrawable(icon)
            } catch (e: Exception) {
                ivAppIcon.setImageResource(R.drawable.ic_shield)
            }

            // Title: app name
            tvAlertTitle.text = alert.appName

            // Build descriptive message
            val permList = alert.permissions.joinToString(", ")
            tvAlertMessage.text = "${alert.appName} accessed $permList " +
                    "while running in the background for ${alert.formattedDuration}. " +
                    "During this time, the app used ${alert.formattedDataUsed} of data."

            // Relative timestamp
            tvAlertTime.text = DateUtils.getRelativeTimeSpanString(
                alert.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )

            // Data badge
            tvAlertDataBadge.text = alert.formattedDataUsed

            // Unread indicator
            indicatorUnread.visibility = if (alert.isRead) View.GONE else View.VISIBLE

            itemView.setOnClickListener { onAlertClick(alert) }
        }
    }

    class AlertDiffCallback : DiffUtil.ItemCallback<PermissionAlert>() {
        override fun areItemsTheSame(old: PermissionAlert, new: PermissionAlert) =
            old.id == new.id

        override fun areContentsTheSame(old: PermissionAlert, new: PermissionAlert) =
            old == new
    }
}
