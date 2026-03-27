package com.uow.guardify.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.uow.guardify.R
import com.uow.guardify.model.TrackerInfo

class TrackerAdapter(
    private val trackers: List<TrackerInfo>
) : RecyclerView.Adapter<TrackerAdapter.TrackerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tracker, parent, false)
        return TrackerViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackerViewHolder, position: Int) {
        holder.bind(trackers[position])
    }

    override fun getItemCount(): Int = trackers.size

    inner class TrackerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTrackerName: TextView = itemView.findViewById(R.id.tvTrackerName)
        private val tvTrackerCategories: TextView = itemView.findViewById(R.id.tvTrackerCategories)

        fun bind(tracker: TrackerInfo) {
            tvTrackerName.text = tracker.name
            
            val categories = tracker.categories?.joinToString(", ") ?: "Analytics"
            tvTrackerCategories.text = categories
        }
    }
}
