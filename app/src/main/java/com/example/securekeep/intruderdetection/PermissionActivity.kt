package com.example.securekeep.intruderdetection

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        // Check CAMERA permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }

        // Check for storage permissions based on Android version Below Android 10
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            // Android 10 to Android 12
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else { // Android 13 and above
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }

        // Request the accumulated permissions if any are missing
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }

        // Check and request overlay permission separately
        requestOverlayPermission()
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission is required for full functionality.", Toast.LENGTH_SHORT).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            handleDeniedPermissions(permissions, grantResults)

            // Recheck and ensure all permissions are granted; if they are, proceed
            if (MainActivity.checkPermissionsForService(this)) {
                startActivity(Intent(this, IntruderActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Permissions are still required.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                Log.d("PermissionActivity", "Overlay permission granted")
            } else {
                Toast.makeText(this, "Overlay permission is required for full functionality.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleDeniedPermissions(permissions: Array<out String>, grantResults: IntArray) {
        val permissionMessages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mapOf(
                Manifest.permission.CAMERA to "Camera permission is required to capture selfies.",
                Manifest.permission.READ_EXTERNAL_STORAGE to "Read External Storage permission is required to access saved selfies.",
                Manifest.permission.READ_MEDIA_IMAGES to "Read Media Images permission is required to access image files."
            )
        } else {
            mapOf(
                Manifest.permission.CAMERA to "Camera permission is required to capture selfies.",
                Manifest.permission.WRITE_EXTERNAL_STORAGE to "Write External Storage permission is required to save selfies.",
                Manifest.permission.READ_EXTERNAL_STORAGE to "Read External Storage permission is required to access saved selfies."
            )
        }

        // Show toast messages for each denied permission
        var permissionDenied = false
        for (i in permissions.indices) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                permissionMessages[permissions[i]]?.let { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
                permissionDenied = true
            }
        }

        // Track denial count and open settings if denied twice
        if (permissionDenied) {
            denialCount++
            if (denialCount >= 2) {
                openSettings()
            }
        }
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 101
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 102
    }
}
