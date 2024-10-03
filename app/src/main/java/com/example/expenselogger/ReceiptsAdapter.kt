// ReceiptsAdapter.kt
package com.example.expenselogger

import android.net.Uri
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
    fun onReceiptDelete(receipt: Receipt)
}

class ReceiptsAdapter(
    private val activitiesList: List<ActivityItem>, // List of activities for name lookup
    private val deleteListener: OnReceiptDeleteListener
) : RecyclerView.Adapter<ReceiptsAdapter.ReceiptViewHolder>() {

    // Internal list of receipts to display
    private val receipts: MutableList<Receipt> = mutableListOf()

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
        val context = holder.itemView.context
        val currencySymbol = context.getString(R.string.currency_symbol)
        val amountText = context.getString(R.string.amount_label, currencySymbol, receipt.amount)

        holder.tvAmount.text = amountText
        holder.tvTimestamp.text = receipt.timestamp

        // Load the image using Glide
        Glide.with(context)
            .load(Uri.parse(receipt.imageUri)) // Ensure imageUri is a valid URI
            .into(holder.ivReceipt)

        val activityName = getActivityNameById(receipt.activityId)
        holder.tvActivityName.text = activityName

        // Set onClickListener for delete button
        holder.btnDelete.setOnClickListener {
            deleteListener.onReceiptDelete(receipt)
        }
    }

    private fun getActivityNameById(activityId: Int): String {
        return activitiesList.find { it.id == activityId }?.name ?: "No Activity"
    }

    override fun getItemCount() = receipts.size

    // Method to update the adapter's data
    fun updateReceipts(newReceipts: List<Receipt>) {
        receipts.clear()
        receipts.addAll(newReceipts)
        notifyDataSetChanged()
    }

    // Optional: Method to clear filters and show all receipts
    fun clearFilter() {
        updateReceipts(emptyList()) // Implement as needed
    }
}