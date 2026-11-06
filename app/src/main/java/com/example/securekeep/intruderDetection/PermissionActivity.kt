package com.example.securekeep.intruderDetection

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.MainActivity
import com.example.securekeep.R
import com.example.securekeep.databinding.ActivityPermissionBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PermissionActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPermissionBinding.inflate(layoutInflater)
    }

    // Track how many times the user has denied permissions
    private var denialCount = 0
    private var isYes = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            proceedIfReady()
        } else {
            Toast.makeText(this, "Camera permission is required to capture selfies.", Toast.LENGTH_SHORT).show()
            denialCount++
            if (denialCount >= 2) {
                openSettings()
            } else {
                Toast.makeText(this, "Permissions are still required.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            Log.d("PermissionActivity", "Overlay permission granted")
            proceedIfReady()
        } else {
            Toast.makeText(this, "Overlay permission is required for full functionality.", Toast.LENGTH_SHORT).show()
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

        // Check if overlay permission is granted
        if (!Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog()
        }

        binding.startBtn.setOnClickListener {
            // Always check and request permissions before starting the next activity
            if (MainActivity.checkPermissionsForService(this)) {
                startActivity(Intent(this, IntruderActivity::class.java))
                finish()
            } else {
                requestPermissions()
            }
        }
    }

    private fun showOverlayPermissionDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Request Overlay Permission")
            .setMessage("Display Overlay permission is needed for intruder feature.")
            .setBackground(ContextCompat.getDrawable(this, R.drawable.simple_round_boarder))
            .setPositiveButton("Yes") { _, _ ->
                isYes = true
                requestOverlayPermission()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .create().apply {
                show()
                // Set title text color
                val titleView = findViewById<TextView>(androidx.appcompat.R.id.alertTitle)
                titleView?.setTextColor(Color.BLACK)
                // Set message text color
                findViewById<TextView>(android.R.id.message)?.setTextColor(Color.BLACK)
                // Set button color
                getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.green))
                getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, R.color.green))
            }
    }

    override fun onResume() {
        super.onResume()
        if (isYes && !Settings.canDrawOverlays(this)){
            finish()
        }
    }

    private fun proceedIfReady() {
        if (MainActivity.checkPermissionsForService(this)) {
            startActivity(Intent(this, IntruderActivity::class.java))
            finish()
        } else if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
        }
    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
        } else {
            proceedIfReady()
        }
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission is required for full functionality.", Toast.LENGTH_SHORT).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }
}