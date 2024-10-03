// MainActivity.kt
package com.example.expenselogger

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.app.AlertDialog
import android.text.InputType
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.ActionBarDrawerToggle
import com.example.expenselogger.models.ActivityItem
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), OnReceiptDeleteListener, ActivitiesAdapter.OnActivityClickListener {

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
    }

    private lateinit var btnTakePhoto: Button
    private lateinit var etAmount: EditText
    private lateinit var tvTotalAmount: TextView
    private lateinit var rvReceipts: RecyclerView
    private val receipts: MutableList<Receipt> = mutableListOf()
    private val filteredReceipts: MutableList<Receipt> = mutableListOf()
    private lateinit var receiptsAdapter: ReceiptsAdapter
    private var totalAmount = 0.0
    private lateinit var btnShareExpenses: Button
    private lateinit var tvEmptyMessage: TextView

    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle

    private lateinit var currentPhotoPath: String
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>

    private var selectedActivity: ActivityItem? = null

    private lateinit var rvActivities: RecyclerView
    private lateinit var activitiesAdapter: ActivitiesAdapter
    private val activitiesList: MutableList<ActivityItem> = mutableListOf()

    private lateinit var tvSelectedActivity: TextView

    private var receiptIdCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Views
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        etAmount = findViewById(R.id.etAmount)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        rvReceipts = findViewById(R.id.rvReceipts)
        btnShareExpenses = findViewById(R.id.btnShareExpenses)
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage)

        // Initialize Toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.fuchsia))

        // Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        // Initialize ActionBarDrawerToggle
        drawerToggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        // Set the DrawerArrowDrawable color to fuchsia
        drawerToggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.fuchsia)

        // Initialize NavigationView Header Components
        val headerView = navigationView.getHeaderView(0)
        val searchView = headerView.findViewById<SearchView>(R.id.searchActivities)
        val btnAddActivity = headerView.findViewById<Button>(R.id.btnAddActivity)
        rvActivities = headerView.findViewById(R.id.rvActivities)

        // Initialize ActivitiesAdapter
        activitiesAdapter = ActivitiesAdapter(activitiesList, this)
        rvActivities.layoutManager = LinearLayoutManager(this)
        rvActivities.adapter = activitiesAdapter

        // Initialize ReceiptsAdapter with corrected constructor
        receiptsAdapter = ReceiptsAdapter(activitiesList, this)
        rvReceipts.layoutManager = LinearLayoutManager(this)
        rvReceipts.adapter = receiptsAdapter

        tvSelectedActivity = findViewById(R.id.tvSelectedActivity)

        // Load Activities and Receipts
        loadActivities()
        loadReceipts()

        // Initialize the ActivityResultLauncher
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                handleImageCapture()
            } else {
                Toast.makeText(this, getString(R.string.photo_capture_failed), Toast.LENGTH_SHORT).show()
            }
            updateEmptyView()
        }

        // Set Click Listener for Adding Activities
        btnAddActivity.setOnClickListener {
            showAddActivityDialog()
        }

        // Set Query Text Listener for SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterActivities(newText)
                return true
            }
        })

        // Check and request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION)
        }

        // Set Click Listener for Take Photo Button
        btnTakePhoto.setOnClickListener {
            if (selectedActivity == null) {
                Toast.makeText(this, "Please select an activity before taking a photo.", Toast.LENGTH_SHORT).show()
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent()
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CAMERA),
                        REQUEST_CAMERA_PERMISSION
                    )
                }
            }
        }

        // Set Click Listener for Share Expenses Button
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
        if (filteredReceipts.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_expenses_to_share), Toast.LENGTH_SHORT).show()
            return
        }

        val expenseData = formatExpenseData()

        val imageUris = ArrayList<Uri>()
        filteredReceipts.forEach { receipt ->
            val uri = Uri.parse(receipt.imageUri)
            imageUris.add(uri)
        }

        val emailIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.expense_report_subject))
            putExtra(Intent.EXTRA_TEXT, expenseData)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

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

        val currencySymbol = getString(R.string.currency_symbol)

        var totalAmount = 0.0

        filteredReceipts.forEach { receipt ->
            builder.append(getString(R.string.amount_label, currencySymbol, receipt.amount))
            builder.append("\n")
            builder.append("${getString(R.string.timestamp)}: ${receipt.timestamp}\n")
            builder.append("\n")
            totalAmount += receipt.amount
        }

        builder.append(getString(R.string.total_amount_label, currencySymbol, totalAmount))

        return builder.toString()
    }

    private fun handleImageCapture() {
        val amountText = etAmount.text.toString()
        val amount = amountText.toDoubleOrNull() ?: 0.0
        val currencySymbol = getString(R.string.currency_symbol)

        val timeStamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.UK).format(Date())

        val activityId = selectedActivity?.id ?: 0

        // Generate content URI using FileProvider
        val photoFile = File(currentPhotoPath)
        val receiptUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile).toString()

        val receipt = Receipt(
            id = ++receiptIdCounter,
            imageUri = receiptUri, // Store as content URI string
            amount = amount,
            timestamp = timeStamp,
            activityId = activityId
        )
        receipts.add(receipt)

        if (selectedActivity != null && receipt.activityId == selectedActivity!!.id) {
            filteredReceipts.add(receipt)
            receiptsAdapter.addReceipt(receipt)

            // Update total amount
            totalAmount += amount
            tvTotalAmount.text = getString(R.string.total_amount_label, currencySymbol, totalAmount)
        }
        // Clear the amount input
        etAmount.text.clear()

        updateEmptyView()
    }

    private fun updateEmptyView() {
        if (filteredReceipts.isEmpty()) {
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

    private fun loadActivities() {
        // Add some dummy activities or load from a database
        activitiesList.add(ActivityItem(1, "Work"))
        activitiesList.add(ActivityItem(2, "Personal"))
        activitiesAdapter.notifyDataSetChanged()
    }

    private fun showAddActivityDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New Activity")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Add") { _, _ ->
            val activityName = input.text.toString()
            if (activityName.isNotBlank()) {
                addNewActivity(activityName)
            } else {
                Toast.makeText(this, "Activity name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)

        builder.show()
    }

    private fun addNewActivity(name: String) {
        val newId = (activitiesList.maxByOrNull { it.id }?.id ?: 0) + 1
        val newActivity = ActivityItem(newId, name)
        activitiesList.add(newActivity)
        activitiesAdapter.notifyDataSetChanged()
    }

    private fun loadReceipts() {
        // Initially, display all receipts
        filteredReceipts.clear()
        filteredReceipts.addAll(receipts)
        receiptsAdapter.filterByActivity(null)

        // Update total amount
        updateTotalAmount()
        updateEmptyView()
    }

    private fun updateTotalAmount() {
        val currencySymbol = getString(R.string.currency_symbol)
        totalAmount = filteredReceipts.sumOf { it.amount }
        tvTotalAmount.text = getString(R.string.total_amount_label, currencySymbol, totalAmount)
    }

    private fun filterReceiptsByActivity(activityId: Int) {
        filteredReceipts.clear()
        filteredReceipts.addAll(receipts.filter { it.activityId == activityId })
        receiptsAdapter.filterByActivity(activityId)

        // Update total amount
        updateTotalAmount()

        // Update the empty view
        updateEmptyView()
    }

    private fun filterActivities(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            activitiesList
        } else {
            activitiesList.filter { it.name.contains(query, ignoreCase = true) }
        }
        activitiesAdapter.updateActivities(filteredList)
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

    override fun onReceiptDelete(receipt: Receipt) {
        val currencySymbol = getString(R.string.currency_symbol)

        // Remove the receipt from the master list
        receipts.remove(receipt)

        // Remove the receipt from the filtered list if present
        filteredReceipts.remove(receipt)

        // Remove from adapter
        receiptsAdapter.removeReceipt(receipt)

        // Update total amount
        totalAmount -= receipt.amount
        tvTotalAmount.text = getString(R.string.total_amount_label, currencySymbol, totalAmount)

        // Update the empty view
        updateEmptyView()
    }

    override fun onActivitySelected(activity: ActivityItem) {
        // Handle the activity selection
        selectedActivity = activity
        drawerLayout.closeDrawers()
        tvSelectedActivity.text = "Activity: ${activity.name}"
        Toast.makeText(this, "Selected: ${activity.name}", Toast.LENGTH_SHORT).show()
        filterReceiptsByActivity(activity.id)
    }
}