package com.example.securekeep.intruderdetection

import android.Manifest
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.intruderdetection.CameraServices.MyDeviceAdminReceiver
import com.example.securekeep.R
import com.example.securekeep.databinding.ActivityIntruderBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class IntruderActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityIntruderBinding.inflate(layoutInflater)
    }
    //private var attemptThreshold = 2 // Default threshold value
    //private var alertStatus = false
    private var intruderServiceRunning = false
    private lateinit var alertDialog: AlertDialog
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private lateinit var sharedPreferences: SharedPreferences

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

        // Check if the app is already a device admin
        if (!devicePolicyManager.isAdminActive(compName)) {
            requestDeviceAdmin()
        }

        intruderServiceRunning = isIntruderServiceRunning()
        Toast.makeText(this, "IntruderService: $intruderServiceRunning", Toast.LENGTH_SHORT).show()
        saveAlertStatus()


        // Retrieving selected attempts and alert status
        //attemptThreshold = sharedPreferences.getInt("AttemptThreshold", 2)
        //binding.selectedAttempts.text = attemptThreshold.toString()
        //

        binding.pinAttemptSelector.setOnClickListener { showNumberPickerDialog() }

        binding.infoBtn.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Alert")
                .setMessage("This feature requires device admin permission, so before uninstalling this application, make sure to disable intruder alert.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .create().apply {
                    show()
                    getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GREEN)
                }

            alertDialog = AlertDialog.Builder(this)
                .setTitle("Will Be Activated In 10 Seconds")
                .setMessage("00:10")
                .setCancelable(false)
                .create()

            updatePowerButton()

            binding.powerBtn.setOnClickListener {
                if (intruderServiceRunning) {
                    stopBackgroundService()
                    //alertStatus = false
                    Toast.makeText(this, "Intruder Alert Mode Deactivated", Toast.LENGTH_SHORT).show()
                    binding.powerBtn.setImageResource(R.drawable.power_on)
                    binding.activateText.text = getString(R.string.tap_to_activate)
                } else {
                    alertDialog.show()
                    //alertStatus = true

                    object : CountDownTimer(10000, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            alertDialog.setMessage("00:${millisUntilFinished / 1000}")
                        }

                        override fun onFinish() {
                            alertDialog.dismiss()
                            Toast.makeText(this@IntruderActivity, "Intruder Alert Mode Activated", Toast.LENGTH_SHORT).show()
                            binding.powerBtn.setImageResource(R.drawable.power_off)
                            binding.activateText.text = getString(R.string.tap_to_deactivate)

                            startBackgroundService()
                            // Save the alert status in shared preferences
                            //saveAlertStatus()
                        }
                    }.start()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        intruderServiceRunning = sharedPreferences.getBoolean("AlertStatusIntruder", false)
        Toast.makeText(this, "IntruderService: $intruderServiceRunning", Toast.LENGTH_SHORT).show()
        updatePowerButton()

    }

    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
        intent.putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Enable device admin for additional security features."
        )
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
    }



    private fun updatePowerButton() {
        //if (alertStatus) {
        if (intruderServiceRunning) {
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

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedRadioButton = dialogView.findViewById<RadioButton>(checkedId)
            val selectedValue = selectedRadioButton.text.toString()
            binding.selectedAttempts.text = selectedValue
            //attemptThreshold = selectedValue.toInt()

            // Store attempt threshold in shared preferences
            //val editor = sharedPreferences.edit()
            //editor.putInt("AttemptThreshold", attemptThreshold)
            //editor.apply()

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun checkPermissionsForService(): Boolean {
        // Android 14 and Above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            val readStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
            val notiPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)

            return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                    readStoragePermission == PackageManager.PERMISSION_GRANTED &&
                    notiPermission == PackageManager.PERMISSION_GRANTED
        }
        // Android 10 and less
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            val readStoragePermission =    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            val writeStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

            return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                    writeStoragePermission == PackageManager.PERMISSION_GRANTED &&
                    readStoragePermission == PackageManager.PERMISSION_GRANTED
        }
        // Android 12 and less
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            val readStoragePermission =    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

            return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                    readStoragePermission == PackageManager.PERMISSION_GRANTED
        }
        // Android 13
        else{
            val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            val readStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)

            return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                    readStoragePermission == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startBackgroundService() {
        if (!checkPermissionsForService()) {
            Toast.makeText(this@IntruderActivity, "All Permissions are not Given", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("IntruderActivity", "startingBackgroundService ")
            /*Intent(this, MagicServiceClass::class.java).also {
                ContextCompat.startForegroundService(this, it)
                cameraServiceRunning = true
            }*/
            intruderServiceRunning = true
            saveAlertStatus()
            startService(Intent(this@IntruderActivity, IntruderTrackingService::class.java)
            )
        }
    }

    private fun stopBackgroundService() {
        /*Intent(this, MagicServiceClass::class.java).also {
            stopService(it)
            cameraServiceRunning = false
        }*/
        intruderServiceRunning = false
        saveAlertStatus()
        stopService(Intent(this@IntruderActivity, IntruderTrackingService::class.java))
    }



    private fun isIntruderServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == IntruderTrackingService::class.java.name }
    }

    private fun saveAlertStatus() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("intruderServiceStatus", intruderServiceRunning)
        editor.apply()
    }

    companion object {
        private const val REQUEST_CODE_ENABLE_ADMIN = 1
        //private const val PERMISSION_REQUEST_CODE = 101  // Unique code for permission requests
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
