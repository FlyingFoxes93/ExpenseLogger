package com.example.expenselogger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.expenselogger.models.ActivityItem

interface OnReceiptDeleteListener {
    fun onReceiptDelete(receipt: Receipt, position: Int)
}

class ReceiptsAdapter(
    private val receipts: MutableList<Receipt>,
    private val activitiesList: List<ActivityItem>,
    private val deleteListener: OnReceiptDeleteListener
) : RecyclerView.Adapter<ReceiptsAdapter.ReceiptViewHolder>() {

    class ReceiptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivReceipt: ImageView = itemView.findViewById(R.id.ivReceipt)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val tvActivityName: TextView = itemView.findViewById(R.id.tvActivityName)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receipt, parent, false)
        return ReceiptViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        val receipt = receipts[position]
        holder.tvAmount.text = holder.itemView.context.getString(R.string.amount_label, receipt.amount)
        holder.tvTimestamp.text = receipt.timestamp

        // Load the image
        Glide.with(holder.itemView.context)
            .load(receipt.imageUri)
            .into(holder.ivReceipt)

        val activityName = getActivityNameById(receipt.activityId)
        holder.tvActivityName.text = activityName

        // Set onClickListener for delete button
        holder.btnDelete.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                val currentReceipt = receipts[currentPosition]
                deleteListener.onReceiptDelete(currentReceipt, currentPosition)
            }
        }
    }

    private fun getActivityNameById(activityId: Int): String {
        return activitiesList.find { it.id == activityId }?.name ?: "No Activity"
    }

    override fun getItemCount() = receipts.size
}