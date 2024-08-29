package com.example.securekeep.intruderdetection

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.MainActivity
import com.example.securekeep.R
import com.example.securekeep.databinding.ActivityPermissionBinding

class PermissionActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPermissionBinding.inflate(layoutInflater)
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

        // Request camera, storage, and other runtime permissions
        requestPermissions()

        binding.startBtn.setOnClickListener {
            if (MainActivity.checkPermissionsForService(this)){
                startActivity(Intent(this, IntruderActivity::class.java))
                finish()
            } else {
                requestPermissions()
            }
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
            // Check if all requested permissions are granted
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("IntruderActivity", "All permissions granted successfully")
            } else {
                handleDeniedPermissions(permissions, grantResults)
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
        // Define messages for denied permissions
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

        // Iterate through denied permissions and show corresponding toast messages
        for ((index, permission) in permissions.withIndex()) {
            if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                permissionMessages[permission]?.let { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 101  // Unique code for permission requests
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 102  // Unique code for overlay permission request
    }
}
