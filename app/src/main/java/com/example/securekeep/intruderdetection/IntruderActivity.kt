package com.example.securekeep.intruderdetection

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.MainActivity
import com.example.securekeep.R
import com.example.securekeep.databinding.ActivityIntruderBinding
import com.example.securekeep.intruderdetection.IntruderServices.IntruderTrackingService
import com.example.securekeep.intruderdetection.IntruderServices.MyDeviceAdminReceiver
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class IntruderActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityIntruderBinding.inflate(layoutInflater)
    }
    private var attemptThreshold = 2 // Default threshold value
    private var alertStatus = false
    private var isIntruderServiceRunning = false
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
        sharedPreferences = getSharedPreferences("IntruderPrefs", MODE_PRIVATE)

        // Check if the app is already a device admin
        if (!devicePolicyManager.isAdminActive(compName)) {
            requestDeviceAdmin()
        }

        binding.backBtn.setOnClickListener {
            startActivity(Intent(this@IntruderActivity, MainActivity::class.java))
            finish()
        }

        isIntruderServiceRunning = MainActivity.isServiceRunning(this@IntruderActivity, IntruderTrackingService::class.java)

        // Retrieving selected attempts and alert status
        alertStatus = sharedPreferences.getBoolean("AlertStatus", false)
        attemptThreshold = sharedPreferences.getInt("AttemptThreshold", 2)
        binding.selectedAttempts.text = attemptThreshold.toString()


        updatePowerButton()

        binding.infoBtn.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Alert")
                .setMessage("This feature requires device admin permission, so before uninstalling this application, make sure to disable intruder alert.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .create().apply {
                    show()
                    getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GREEN)
                }
        }

        binding.pinAttemptSelector.setOnClickListener { showNumberPickerDialog() }

        alertDialog = AlertDialog.Builder(this)
            .setTitle("Will Be Activated In 10 Seconds")
            .setMessage("00:10")
            .setCancelable(false)
            .create()

        binding.powerBtn.setOnClickListener {
            if (alertStatus && isIntruderServiceRunning) {
                stopBackgroundService()
                alertStatus = false
                isIntruderServiceRunning = false
                Toast.makeText(this, "Intruder Alert Mode Deactivated", Toast.LENGTH_SHORT).show()
                binding.powerBtn.setImageResource(R.drawable.power_on)
                binding.activateText.text = getString(R.string.tap_to_activate)
                //Save the alert status in shared preferences
                val editor = sharedPreferences.edit()
                editor.putBoolean("AlertStatus", alertStatus)
                editor.apply()
            } else {
                alertDialog.show()
                alertStatus = true
                isIntruderServiceRunning = true

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
                        //Save the alert status in shared preferences
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("AlertStatus", alertStatus)
                        editor.apply()
                    }
                }.start()
            }
        }

        binding.viewIntruderPic.setOnClickListener {
            startActivity(Intent(this@IntruderActivity, IntruderSelfieActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        alertStatus = sharedPreferences.getBoolean("AlertStatus", false)
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
        if (alertStatus && isIntruderServiceRunning) {
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
            attemptThreshold = selectedValue.toInt()

            // Store attempt threshold in shared preferences
            val editor = sharedPreferences.edit()
            editor.putInt("AttemptThreshold", attemptThreshold)
            editor.apply()

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun startBackgroundService() {
            Log.d("IntruderActivity", "startingBackgroundService ")
            isIntruderServiceRunning = true
            startService(Intent(this@IntruderActivity, IntruderTrackingService::class.java))
    }

    private fun stopBackgroundService() {
        isIntruderServiceRunning = false
        stopService(Intent(this@IntruderActivity, IntruderTrackingService::class.java))
    }

    companion object {
        private const val REQUEST_CODE_ENABLE_ADMIN = 1
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
