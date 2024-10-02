package com.example.expenselogger

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), OnReceiptDeleteListener {

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
    }

    private lateinit var btnTakePhoto: Button
    private lateinit var etAmount: EditText
    private lateinit var tvTotalAmount: TextView
    private lateinit var rvReceipts: RecyclerView
    private val receipts = mutableListOf<Receipt>()
    private lateinit var receiptsAdapter: ReceiptsAdapter
    private var totalAmount = 0.0
    private lateinit var btnShareExpenses: Button
    private lateinit var tvEmptyMessage: TextView

    private lateinit var currentPhotoPath: String
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        etAmount = findViewById(R.id.etAmount)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        rvReceipts = findViewById(R.id.rvReceipts)
        btnShareExpenses = findViewById(R.id.btnShareExpenses)
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage)

        receiptsAdapter = ReceiptsAdapter(receipts, this)
        rvReceipts.layoutManager = LinearLayoutManager(this)
        rvReceipts.adapter = receiptsAdapter

        // Initialize the ActivityResultLauncher
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                handleImageCapture()
            } else {
                Toast.makeText(this, getString(R.string.photo_capture_failed), Toast.LENGTH_SHORT).show()
            }
            updateEmptyView()
        }

        // Check and request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION)
        }

        btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION)
            }
        }
        btnShareExpenses.setOnClickListener {
            shareExpenses()
        }
    }

    private fun dispatchTakePictureIntent() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            ex.printStackTrace()
            null
        }
        photoFile?.also {
            val photoURI = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                it
            )
            takePictureLauncher.launch(photoURI)
        }
    }

    private fun shareExpenses() {
        if (receipts.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_expenses_to_share), Toast.LENGTH_SHORT).show()
            return
        }

        // Format the expense data
        val expenseData = formatExpenseData()

        // Collect image URIs
        val imageUris = ArrayList<Uri>()
        receipts.forEach { receipt ->
            val file = File(receipt.imageUri)
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            imageUris.add(uri)
        }

        // Create an email intent
        val emailIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.expense_report_subject))
            putExtra(Intent.EXTRA_TEXT, expenseData)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Launch the email intent
        try {
            startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email)))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.no_email_clients), Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatExpenseData(): String {
        val builder = StringBuilder()
        builder.append(getString(R.string.expense_report_header))
        builder.append("\n\n")

        var totalAmount = 0.0

        receipts.forEach { receipt ->
            builder.append("${getString(R.string.amount)}: $${String.format("%.2f", receipt.amount)}\n")
            builder.append("${getString(R.string.timestamp)}: ${receipt.timestamp}\n")
            builder.append("\n")
            totalAmount += receipt.amount
        }

        builder.append("${getString(R.string.total_amount)}: $${String.format("%.2f", totalAmount)}\n")

        return builder.toString()
    }

    private fun handleImageCapture() {
        val amountText = etAmount.text.toString()
        val amount = amountText.toDoubleOrNull() ?: 0.0

        val timeStamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        val receipt = Receipt(
            imageUri = currentPhotoPath,
            amount = amount,
            timestamp = timeStamp
        )
        receipts.add(receipt)
        receiptsAdapter.notifyItemInserted(receipts.size - 1)

        // Update total amount
        totalAmount += amount
        tvTotalAmount.text = getString(R.string.total_amount_label, totalAmount)

        // Clear the amount input
        etAmount.text.clear()

        updateEmptyView()
    }

    private fun updateEmptyView() {
        if (receipts.isEmpty()) {
            rvReceipts.visibility = View.GONE
            tvEmptyMessage.visibility = View.VISIBLE
        } else {
            rvReceipts.visibility = View.VISIBLE
            tvEmptyMessage.visibility = View.GONE
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    // Handle permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    dispatchTakePictureIntent()
                } else {
                    Toast.makeText(this, getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    override fun onReceiptDelete(receipt: Receipt, position: Int) {
        // Remove the receipt from the list
        receipts.removeAt(position)

        // Notify the adapter
        receiptsAdapter.notifyItemRemoved(position)

        // Update the total amount
        totalAmount -= receipt.amount
        tvTotalAmount.text = getString(R.string.total_amount_label, totalAmount)

        // Update the empty view
        updateEmptyView()
    }
}