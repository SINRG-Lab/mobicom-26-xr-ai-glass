package com.sdk.glassessdksample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecentActivityAdapter(
    private val activities: MutableList<RecentActivityItem>
) : RecyclerView.Adapter<RecentActivityAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.activity_icon)
        val title: TextView = view.findViewById(R.id.activity_title)
        val subtitle: TextView = view.findViewById(R.id.activity_subtitle)
        val badge: TextView = view.findViewById(R.id.activity_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_activity, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = activities[position]
        holder.icon.setImageResource(activity.iconRes)
        holder.title.text = activity.title
        holder.subtitle.text = activity.subtitle
        holder.badge.text = activity.badgeText
    }

    override fun getItemCount() = activities.size

    fun addActivity(item: RecentActivityItem) {
        if (activities.size >= 3) {
            activities.removeAt(0) // remove oldest
        }
        activities.add(item)       // add newest
        notifyDataSetChanged()
    }

    fun updateActivity(position: Int) {
        activities[position].badgeText = "Synced"
        notifyDataSetChanged()
    }
}
