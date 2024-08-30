package com.example.securekeep.intruderdetection

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.securekeep.R
import com.example.securekeep.databinding.ActivityIntruderSelfieBinding
import com.example.securekeep.intruderdetection.IntruderAdapter.SelfieAdapter
import com.example.securekeep.intruderdetection.IntruderAdapter.SelfieModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IntruderSelfieActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityIntruderSelfieBinding.inflate(layoutInflater)
    }
    private lateinit var selfieAdapter: SelfieAdapter
    private lateinit var selfieList: MutableList<SelfieModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.backBtn.setOnClickListener {
            startActivity(Intent(this@IntruderSelfieActivity, IntruderActivity::class.java))
            finish()
        }

        selfieList = mutableListOf() // Initialize the list
        binding.rcv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        selfieAdapter = SelfieAdapter(this, selfieList)
        binding.rcv.adapter = selfieAdapter
        binding.rcv.setHasFixedSize(true)
        binding.rcv.setItemViewCacheSize(13)

        loadSelfiesFromStorage()
    }

    private fun loadSelfiesFromStorage() {
        // Define your app-specific directory for images
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        if (storageDir != null && storageDir.exists()) {
            // List all files in the directory
            val imageFiles = storageDir.listFiles { file ->
                file.isFile && (file.extension == "jpg" || file.extension == "jpeg" || file.extension == "png")
            }

            // Create a date formatter for the desired format
            val dateFormatter = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault())

            // Add image files to the selfie list
            imageFiles?.forEach { file ->
                // The absolute path of the image file
                val imageUri = Uri.fromFile(file)

                // Format the last modified date
                val dateTaken = dateFormatter.format(Date(file.lastModified()))

                // Add to the list
                selfieList.add(SelfieModel(imageUri, dateTaken))
            }
        } else {
            Log.e("SelfieActivity", "Storage directory not found or is empty.")
        }

        // Notify adapter about data changes
        selfieAdapter.notifyDataSetChanged()

        Log.d("SelfieActivity", "loadSelfiesFromStorage: Loaded ${selfieList.size} images")
    }
}
