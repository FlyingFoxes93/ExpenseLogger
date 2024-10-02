package com.example.expenselogger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expenselogger.R
import com.example.expenselogger.models.ActivityItem

class ActivitiesAdapter(
    private var activities: List<ActivityItem>,
    private val listener: OnActivityClickListener
) : RecyclerView.Adapter<ActivitiesAdapter.ActivityViewHolder>() {

    interface OnActivityClickListener {
        fun onActivitySelected(activity: ActivityItem)
    }

    inner class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvActivityName: TextView = itemView.findViewById(R.id.tvActivityName)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onActivitySelected(activities[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = activities[position]
        holder.tvActivityName.text = activity.name
    }

    override fun getItemCount() = activities.size

    fun updateActivities(newActivities: List<ActivityItem>) {
        activities = newActivities
        notifyDataSetChanged()
    }
}
