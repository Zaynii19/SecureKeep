package com.example.securekeep.intruderdetection

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securekeep.R
import com.example.securekeep.databinding.ActivityIntruderSelfieBinding
import com.example.securekeep.intruderdetection.IntruderAdapter.SelfieAdapter
import com.example.securekeep.intruderdetection.IntruderAdapter.SelfieModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IntruderSelfieActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityIntruderSelfieBinding.inflate(layoutInflater)
    }
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var selfieAdapter: SelfieAdapter
    private lateinit var selfieList: MutableList<SelfieModel>

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Handle back button press here
            startActivity(Intent(this@IntruderSelfieActivity, IntruderActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("IntruderPrefs", MODE_PRIVATE)

        selfieList = mutableListOf()
        binding.rcv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        selfieAdapter = SelfieAdapter(this, selfieList, ::updateActionButtonVisibility)
        binding.rcv.adapter = selfieAdapter
        binding.rcv.setHasFixedSize(true)
        binding.rcv.setItemViewCacheSize(13)


        binding.backBtn.setOnClickListener {
            startActivity(Intent(this@IntruderSelfieActivity, IntruderActivity::class.java))
            finish()
        }

        // Initially hide action buttons
        updateActionButtonVisibility()

        loadSelfiesFromStorage()

        binding.selectAllBtn.setOnClickListener {
            if (selfieAdapter.getSelectedCount() == selfieList.size) {
                selfieAdapter.clearSelection()
            } else {
                selfieAdapter.selectAll()
            }
            updateActionButtonVisibility()
        }

        binding.delBtn.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun loadSelfiesFromStorage() {
        // Clear the existing list to avoid duplicates
        selfieList.clear()

        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        if (storageDir != null && storageDir.exists()) {
            val imageFiles = storageDir.listFiles { file ->
                file.isFile && (file.extension == "jpg" || file.extension == "jpeg" || file.extension == "png")
            }

            val dateFormatter = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault())

            imageFiles?.forEach { file ->
                val imageUri = Uri.fromFile(file)
                val dateTaken = dateFormatter.format(Date(file.lastModified()))
                selfieList.add(SelfieModel(imageUri, dateTaken))
            }

            // Sort the selfieList by dateTaken
            selfieList.sortByDescending { selfie ->
                dateFormatter.parse(selfie.dateTime) // Parse the date for accurate sorting
            }

        } else {
            Log.e("SelfieActivity", "Storage directory not found or is empty.")
        }

        selfieAdapter.notifyDataSetChanged()
        Log.d("SelfieActivity", "loadSelfiesFromStorage: Loaded ${selfieList.size} images")
    }


    private fun showDeleteConfirmationDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Delete")
            .setMessage("Do you want to delete the selected intruder pictures?")
            .setBackground(ContextCompat.getDrawable(this, R.drawable.simple_round_boarder))
            .setPositiveButton("Yes") { _, _ ->
                deleteSelectedImages()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create().apply {
                show()
                // Set title text color
                val titleView = findViewById<TextView>(androidx.appcompat.R.id.alertTitle)
                titleView?.setTextColor(Color.BLACK)
                // Set message text color
                findViewById<TextView>(android.R.id.message)?.setTextColor(Color.BLACK)
                // Set button color
                getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GREEN)
                getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GREEN)
            }
    }

    private fun deleteSelectedImages() {
        selfieAdapter.deleteSelectedItems { deletedCount ->
            Toast.makeText(this, "Deleted $deletedCount items", Toast.LENGTH_SHORT).show()
            updateActionButtonVisibility()
        }
    }

    private fun updateActionButtonVisibility() {
        if (selfieAdapter.isSelectionMode) {
            binding.selectAllBtn.visibility = View.VISIBLE
            binding.delBtn.visibility = View.VISIBLE
        } else {
            binding.selectAllBtn.visibility = View.GONE
            binding.delBtn.visibility = View.GONE
        }
    }
}
