package com.uow.guardify.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.uow.guardify.R
import com.uow.guardify.util.PermissionHelper

class PermissionAdapter(
    private val permissions: List<String>
) : RecyclerView.Adapter<PermissionAdapter.PermissionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PermissionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_permission, parent, false)
        return PermissionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PermissionViewHolder, position: Int) {
        holder.bind(permissions[position])
    }

    override fun getItemCount(): Int = permissions.size

    inner class PermissionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPermissionIcon: ImageView = itemView.findViewById(R.id.ivPermissionIcon)
        private val tvPermissionName: TextView = itemView.findViewById(R.id.tvPermissionName)
        private val tvPermissionDesc: TextView = itemView.findViewById(R.id.tvPermissionDesc)
        private val indicatorSensitive: View = itemView.findViewById(R.id.indicatorSensitive)

        fun bind(permission: String) {
            tvPermissionName.text = PermissionHelper.getPermissionName(permission)
            tvPermissionDesc.text = PermissionHelper.getPermissionDescription(permission)
            
            // Show warning indicator for sensitive permissions
            if (PermissionHelper.isSensitivePermission(permission)) {
                indicatorSensitive.visibility = View.VISIBLE
            } else {
                indicatorSensitive.visibility = View.GONE
            }
        }
    }
}
