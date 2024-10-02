package com.example.expenselogger

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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

    private lateinit var currentPhotoPath: String
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        etAmount = findViewById(R.id.etAmount)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        rvReceipts = findViewById(R.id.rvReceipts)

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

        // Check if the list is empty
        if (receipts.isEmpty()) {
            // Optionally, you can reset the adapter or update the UI
            // For example, you might want to display a message indicating the list is empty
        }

        // Update the total amount
        totalAmount -= receipt.amount
        tvTotalAmount.text = getString(R.string.total_amount_label, totalAmount)
    }
}