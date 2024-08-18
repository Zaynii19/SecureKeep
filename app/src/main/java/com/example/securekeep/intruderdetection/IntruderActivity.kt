
package com.example.securekeep.intruderdetection

import android.Manifest
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.MainActivity
import com.example.securekeep.R
import com.example.securekeep.databinding.ActivityIntruderBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class IntruderActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityIntruderBinding.inflate(layoutInflater)
    }

    private var attemptThreshold = 1 // Default threshold value
    private var alertStatus = false
    private var cameraServiceRunning = false
    private lateinit var alertDialog: AlertDialog
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private var currentFailedAttempts = 0
    private lateinit var sharedPreferences: SharedPreferences
    private val passwordAttemptReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "PASSWORD_ATTEMPT_FAILED") {
                onWrongPinAttempt()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, MyDeviceAdminReceiver::class.java)

        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)

        // Register the BroadcastReceiver
        val filter = IntentFilter("PASSWORD_ATTEMPT_FAILED")
        registerReceiver(passwordAttemptReceiver, filter, RECEIVER_EXPORTED)

        // Check if the app is already a device admin
        if (!devicePolicyManager.isAdminActive(compName)) {
            // Call your method to request for device admin
            requestDeviceAdmin()
        }

        // Request camera and storage permissions
        requestPermissions()

        cameraServiceRunning = isCameraServiceRunning()

        // Retrieving selected attempts, alert status
        attemptThreshold = sharedPreferences.getInt("AttemptThreshold", 2)
        binding.selectedAttempts.text = sharedPreferences.getInt("AttemptThreshold", 2).toString()
        alertStatus = sharedPreferences.getBoolean("AlertStatusIntruder", false)

        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.pinAttemptSelector.setOnClickListener {
            showNumberPickerDialog()
        }

        binding.infoBtn.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(this)
            builder.setTitle("Alert")
                .setMessage("This feature requires device admin permission, so before uninstalling this application, make sure to disable intruder alert.")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
            val infoDialog = builder.create()
            infoDialog.show()
            infoDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GREEN)
        }

        alertDialog = AlertDialog.Builder(this)
            .setTitle("Will Be Activated In 10 Seconds")
            .setMessage("00:10")
            .setCancelable(false)
            .create()

        updatePowerButton()

        binding.powerBtn.setOnClickListener {
            if (alertStatus) {
                stopBackgroundService()
                alertStatus = false
                Toast.makeText(this@IntruderActivity, "Intruder Alert Mode Deactivated", Toast.LENGTH_SHORT).show()
                binding.powerBtn.setImageResource(R.drawable.power_on)
                binding.activateText.text = getString(R.string.tap_to_activate)
            } else {
                if (cameraServiceRunning) {
                    stopBackgroundService()
                } else {
                    alertDialog.show()
                    alertStatus = true

                    object : CountDownTimer(10000, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            alertDialog.setMessage("00:${millisUntilFinished / 1000}")
                        }

                        override fun onFinish() {
                            if (alertDialog.isShowing) {
                                alertDialog.dismiss()
                            }
                            Toast.makeText(this@IntruderActivity, "Intruder Alert Mode Activated", Toast.LENGTH_SHORT).show()
                            binding.powerBtn.setImageResource(R.drawable.power_off)
                            binding.activateText.text = getString(R.string.tap_to_deactivate)

                            // Capture Intruder Image
                            onWrongPinAttempt()
                            // Storing alarm status value in shared preferences
                            val editor = sharedPreferences.edit()
                            editor.putBoolean("AlertStatusIntruder", alertStatus)
                            editor.apply()
                        }
                    }.start()
                }
            }
            saveAlertStatus()
        }
    }

    private fun requestDeviceAdmin() {
        // Create an intent to request device admin
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable device admin for additional security features.")
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == RESULT_OK) {
                // Device admin enabled
                Toast.makeText(this, "Device admin enabled.", Toast.LENGTH_SHORT).show()
            } else {
                // Device admin not enabled
                Toast.makeText(this, "Device admin not enabled. Please enable it to use this feature.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePowerButton() {
        if (alertStatus) {
            binding.powerBtn.setImageResource(R.drawable.power_off)
            binding.activateText.text = getString(R.string.tap_to_deactivate)
        } else {
            binding.powerBtn.setImageResource(R.drawable.power_on)
            binding.activateText.text = getString(R.string.tap_to_activate)
        }
    }

    private fun showNumberPickerDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_attempt_picker, null)
        builder.setView(dialogView)

        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroup)
        val dialog = builder.create()

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedRadioButton = dialogView.findViewById<RadioButton>(checkedId)
            val selectedValue = selectedRadioButton.text.toString()
            binding.selectedAttempts.text = selectedValue
            attemptThreshold = selectedValue.toInt()

            // Storing attempt threshold value in shared preferences
            val editor = sharedPreferences.edit()
            editor.putInt("AttemptThreshold", attemptThreshold)
            editor.apply()

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun startBackgroundService() {
        if (!checkPermission()) {
            requestPermissions()
        } else {
            Intent(this, CameraService::class.java).also {
                ContextCompat.startForegroundService(this, it)
                cameraServiceRunning = true
            }
        }
    }

    private fun stopBackgroundService() {
        Intent(this, CameraService::class.java).also {
            stopService(it)
            cameraServiceRunning = false
        }
    }

    private fun checkPermission(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)

        val writeStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        val readStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val foregroundPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE)
        } else {
            PackageManager.PERMISSION_GRANTED
        }

        return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                writeStoragePermission == PackageManager.PERMISSION_GRANTED &&
                readStoragePermission == PackageManager.PERMISSION_GRANTED &&
                foregroundPermission == PackageManager.PERMISSION_GRANTED
    }




    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
             permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.FOREGROUND_SERVICE)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
        }
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101) {
            // Check if all requested permissions are granted
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("IntruderActivity", "onRequestPermissionsResult: All permissions Granted Successfully")
            } else {
                // Determine which permissions were not granted
                val deniedPermissions = permissions.filterIndexed { index, _ -> grantResults[index] != PackageManager.PERMISSION_GRANTED }

                // Provide user feedback based on which permissions are denied
                /*if (deniedPermissions.contains(Manifest.permission.CAMERA) ||
                    deniedPermissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    deniedPermissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    deniedPermissions.contains(Manifest.permission.READ_MEDIA_IMAGES) ||
                    ) {

                    Toast.makeText(this, "Camera and Storage permissions are required to capture selfies.", Toast.LENGTH_SHORT).show()
                }*/

                // Define a map for the denied permissions and their corresponding messages
                val permissionMessages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    mapOf(
                        Manifest.permission.CAMERA to "Camera permission is required to capture selfies.",
                        Manifest.permission.READ_EXTERNAL_STORAGE to "Read External Storage permission is required to access saved selfies.",
                        Manifest.permission.READ_MEDIA_IMAGES to "Read Media Images permission is required to access image files.")
                } else {
                    mapOf(
                    Manifest.permission.CAMERA to "Camera permission is required to capture selfies.",
                    Manifest.permission.WRITE_EXTERNAL_STORAGE to "Write External Storage permission is required to save selfies.",
                    Manifest.permission.READ_EXTERNAL_STORAGE to "Read External Storage permission is required to access saved selfies.")
                }

                // Iterate through denied permissions and show the corresponding toast messages
                for (permission in deniedPermissions) {
                    permissionMessages[permission]?.let { message ->
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }



    private fun isCameraServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE).any { it.service.className == CameraService::class.java.name }
    }

    private fun onWrongPinAttempt() {
        currentFailedAttempts++
        Log.d("IntruderActivity", "Wrong PIN attempt detected. Failed attempts: $currentFailedAttempts")
        if (currentFailedAttempts >= attemptThreshold) {
            Log.d("IntruderActivity", "Threshold met, starting CameraService.")
            startBackgroundService()
        }
    }


    private fun saveAlertStatus() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("AlertStatusIntruder", alertStatus)
        editor.apply()
    }

    companion object {
        private const val REQUEST_CODE_ENABLE_ADMIN = 1
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(passwordAttemptReceiver)
    }
}
