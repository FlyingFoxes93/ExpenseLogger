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
    private val activitiesList: List<ActivityItem>,
    private val deleteListener: OnReceiptDeleteListener
) : RecyclerView.Adapter<ReceiptsAdapter.ReceiptViewHolder>() {

    // Master list of all receipts
    private val allReceipts: MutableList<Receipt> = mutableListOf()

    // Filtered list to display
    private val filteredReceipts: MutableList<Receipt> = mutableListOf()

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
        val receipt = filteredReceipts[position]
        val context = holder.itemView.context
        val currencySymbol = context.getString(R.string.currency_symbol)
        val amountText = context.getString(R.string.amount_label, currencySymbol, receipt.amount)

        holder.tvAmount.text = amountText
        holder.tvTimestamp.text = receipt.timestamp

        // Load the image using Glide with a placeholder and error image
        Glide.with(context)
            .load(Uri.parse(receipt.imageUri)) // Ensure imageUri is a valid content URI string
            .placeholder(R.drawable.ic_receipt_placeholder) // Add a placeholder image in drawable
            .error(R.drawable.ic_receipt_placeholder) // Add an error image in drawable
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

    override fun getItemCount() = filteredReceipts.size

    // Method to add a receipt
    fun addReceipt(receipt: Receipt) {
        allReceipts.add(receipt)
        filteredReceipts.add(receipt)
        notifyItemInserted(filteredReceipts.size - 1)
    }

    // Method to remove a receipt
    fun removeReceipt(receipt: Receipt) {
        val position = filteredReceipts.indexOf(receipt)
        if (position != -1) {
            filteredReceipts.removeAt(position)
            notifyItemRemoved(position)
        }
        allReceipts.remove(receipt)
    }

    // Method to filter receipts by activity
    fun filterByActivity(activityId: Int?) {
        filteredReceipts.clear()
        if (activityId == null) {
            // No filter applied, show all receipts
            filteredReceipts.addAll(allReceipts)
        } else {
            // Filter receipts matching the activityId
            filteredReceipts.addAll(allReceipts.filter { it.activityId == activityId })
        }
        notifyDataSetChanged()
    }

    // Optional: Method to clear filters
    fun clearFilter() {
        filterByActivity(null)
    }

    // Method to update the entire receipts list
    fun updateReceipts(newReceipts: List<Receipt>) {
        allReceipts.clear()
        allReceipts.addAll(newReceipts)

        // Apply current filter
        if (currentFilterActivityId != null) {
            filterByActivity(currentFilterActivityId)
        } else {
            filterByActivity(null)
        }
    }

    private var currentFilterActivityId: Int? = null

    // Override filterByActivity to keep track of current filter
    fun filterByActivity(activityId: Int?, notify: Boolean = true) {
        currentFilterActivityId = activityId
        filteredReceipts.clear()
        if (activityId == null) {
            // No filter applied, show all receipts
            filteredReceipts.addAll(allReceipts)
        } else {
            // Filter receipts matching the activityId
            filteredReceipts.addAll(allReceipts.filter { it.activityId == activityId })
        }
        if (notify) notifyDataSetChanged()
    }
}