// ActivitiesAdapter.kt
package com.example.expenselogger

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.expenselogger.R
import com.example.expenselogger.models.ActivityItem

class ActivitiesAdapter(
    private var activities: MutableList<ActivityItem>,
    private val listener: OnActivityClickListener,
    private val activityDeleteListener: OnActivityDeleteListener
) : RecyclerView.Adapter<ActivitiesAdapter.ActivityViewHolder>() {

    interface OnActivityClickListener {
        fun onActivitySelected(activity: ActivityItem)
    }

    interface OnActivityDeleteListener {
        fun onActivityDelete(activity: ActivityItem)
    }

    inner class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvActivityName: TextView = itemView.findViewById(R.id.tvActivityName)
        val btnDeleteActivity: ImageButton = itemView.findViewById(R.id.btnDeleteActivity)

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

        holder.btnDeleteActivity.setOnClickListener {
            // Prevent deletion of default activities
            if (activity.id == MainActivity.DEFAULT_ACTIVITY_ID) {
                Toast.makeText(holder.itemView.context, "Cannot delete default activity.", Toast.LENGTH_SHORT).show()
            } else {
                // Show confirmation dialog
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Delete Activity")
                    .setMessage("Are you sure you want to delete this activity?")
                    .setPositiveButton("Yes") { _, _ ->
                        activityDeleteListener.onActivityDelete(activity)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }
    }

    override fun getItemCount() = activities.size

    fun updateActivities(newActivities: List<ActivityItem>) {
        activities.clear()
        activities.addAll(newActivities)
        notifyDataSetChanged()
    }

    fun addActivity(activity: ActivityItem) {
        activities.add(activity)
        notifyItemInserted(activities.size - 1)
    }

    fun removeActivity(activity: ActivityItem) {
        val position = activities.indexOf(activity)
        if (position != -1) {
            activities.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}