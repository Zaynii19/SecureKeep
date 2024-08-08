package com.example.securekeep

import android.Manifest
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.databinding.ActivityIntruderBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class IntruderActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityIntruderBinding.inflate(layoutInflater)
    }

    private var attemptThreshold = 1 // Default threshold value
    private var alertStatus = "NotActive"
    private var cameraServiceRunning = false
    private lateinit var alertDialog: AlertDialog
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private var currentFailedAttempts = 0

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

        // Request device admin permission if not already granted
        if (!devicePolicyManager.isAdminActive(compName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable device admin for additional security features.")
            startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
        }

        cameraServiceRunning = isCameraServiceRunning()

        // Retrieving selected attempts, alert status
        val editor = getPreferences(MODE_PRIVATE)
        binding.selectedAttempts.text = editor.getString("selectedAttempts", "2")
        val selectedValue = editor.getString("selectedValue", "2")
        attemptThreshold = selectedValue!!.toInt()
        alertStatus = editor.getString("AlertStatus", "NotActive")!!

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
            if (alertStatus == "Active") {
                stopBackgroundService()
                alertStatus = "NotActive"
                Toast.makeText(this@IntruderActivity, "Intruder Alert Mode Deactivated", Toast.LENGTH_SHORT).show()
                binding.powerBtn.setImageResource(R.drawable.power_on)
                binding.activateText.text = getString(R.string.tap_to_activate)
            } else {
                if (cameraServiceRunning) {
                    stopBackgroundService()
                } else {
                    alertDialog.show()
                    alertStatus = "Active"

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
                        }
                    }.start()
                }
            }
            saveAlertStatus()
        }
    }

    private fun updatePowerButton() {
        if (alertStatus == "Active") {
            binding.powerBtn.setImageResource(R.drawable.switch_off)
            binding.activateText.text = getString(R.string.tap_to_deactivate)
        } else {
            binding.powerBtn.setImageResource(R.drawable.power_on)
            binding.activateText.text = getString(R.string.tap_to_activate)
        }
    }

    private fun showNumberPickerDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_number_picker, null)
        builder.setView(dialogView)

        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroup)
        val dialog = builder.create()

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val selectedRadioButton = dialogView.findViewById<RadioButton>(checkedId)
            val selectedValue = selectedRadioButton.text.toString()
            binding.selectedAttempts.text = selectedValue
            attemptThreshold = selectedValue.toInt()

            // Storing attempt value in shared preferences
            val editor = getPreferences(MODE_PRIVATE).edit()
            editor.putString("selectedAttempts", selectedValue)
            editor.apply()

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun startBackgroundService() {
        if (!checkPermission()) {
            requestPermission()
        } else {
            Intent(this, CameraService::class.java).also {
                startService(it)
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
        val storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return cameraPermission == PackageManager.PERMISSION_GRANTED && storagePermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), 101)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startBackgroundService()
        }
    }

    private fun isCameraServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE).any { it.service.className == CameraService::class.java.name }
    }

    private fun onWrongPinAttempt() {
        currentFailedAttempts = getWrongAttempts()
        Log.d("IntruderActivity", "Wrong PIN attempt detected. Failed attempts: $currentFailedAttempts")
        if (currentFailedAttempts >= attemptThreshold) {
            Log.d("IntruderActivity", "Threshold met, starting CameraService.")
            startBackgroundService()
        }
    }

    private fun getWrongAttempts(): Int {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val failedAttempts = dpm.currentFailedPasswordAttempts
        Log.d("IntruderActivity", "Retrieved failed attempts: $failedAttempts")
        return failedAttempts
    }


    private fun saveAlertStatus() {
        val editor = getPreferences(MODE_PRIVATE).edit()
        editor.putString("AlertStatus", alertStatus)
        editor.apply()
    }

    companion object {
        private const val REQUEST_CODE_ENABLE_ADMIN = 1
    }
}
