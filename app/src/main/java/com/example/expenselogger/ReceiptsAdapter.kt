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
import com.example.expenselogger.Receipt

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

    // Current filter
    private var currentFilterActivityId: Int? = null

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
            .load(Uri.parse(receipt.imageUri))
            .placeholder(R.drawable.ic_receipt_placeholder)
            .error(R.drawable.ic_receipt_placeholder)
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

    // Method to set/update the entire receipts list
    fun setReceipts(newReceipts: List<Receipt>) {
        allReceipts.clear()
        allReceipts.addAll(newReceipts)
        applyFilter()
    }

    // Method to filter receipts by activity
    fun filterByActivity(activityId: Int?) {
        currentFilterActivityId = activityId
        applyFilter()
    }

    private fun applyFilter() {
        filteredReceipts.clear()
        if (currentFilterActivityId == null) {
            // No filter applied, show all receipts
            filteredReceipts.addAll(allReceipts)
        } else {
            // Filter receipts matching the activityId
            filteredReceipts.addAll(allReceipts.filter { it.activityId == currentFilterActivityId })
        }
        notifyDataSetChanged()
    }

    // Getter to retrieve the currently filtered receipts
    fun getFilteredReceipts(): List<Receipt> {
        return filteredReceipts.toList()
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

    // Method to add a receipt
    fun addReceipt(receipt: Receipt) {
        allReceipts.add(receipt)
        if (currentFilterActivityId == null || receipt.activityId == currentFilterActivityId) {
            filteredReceipts.add(receipt)
            notifyItemInserted(filteredReceipts.size - 1)
        }
    }
}