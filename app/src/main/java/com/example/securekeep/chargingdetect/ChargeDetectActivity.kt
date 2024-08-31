package com.example.securekeep.chargingdetect

import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.os.BatteryManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.MainActivity
import com.example.securekeep.R
import com.example.securekeep.alarmsetup.EnterPinActivity
import com.example.securekeep.databinding.ActivityChargeDetectBinding
import com.example.securekeep.settings.SettingActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ChargeDetectActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityChargeDetectBinding.inflate(layoutInflater)
    }
    private lateinit var alertDialog: AlertDialog
    private var isAlarmActive = false
    private var isVibrate = false
    private var isFlash = false
    private var isChargingServiceRunning = false
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Retrieving selected attempts, alert status
        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        isAlarmActive = sharedPreferences.getBoolean("AlarmStatusCharge", false)
        isVibrate = sharedPreferences.getBoolean("VibrateStatusCharge", false)
        isFlash = sharedPreferences.getBoolean("FlashStatusCharge", false)

        isChargingServiceRunning = MainActivity.isServiceRunning(this@ChargeDetectActivity, ChargingDetectionService::class.java)

        updateUI()

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.settingBtn.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        alertDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Will Be Activated In 10 Seconds")
            .setMessage("00:10")
            .setBackground(ContextCompat.getDrawable(this, R.drawable.simple_round_boarder))
            .setCancelable(false)
            .create()

        if (isChargerConnected()) {
            if (isAlarmActive) {
                startChargingDetectionService()
            }
            updateUI()
        } else {
            Toast.makeText(this@ChargeDetectActivity, "Connect Charger First", Toast.LENGTH_SHORT).show()
            binding.powerBtn.setImageResource(R.drawable.charger)
            binding.powerBtn.setOnClickListener(null)
        }

        binding.powerBtn.setOnClickListener {
            if (isChargerConnected()) {
                if (!isAlarmActive && !isChargingServiceRunning) {
                    isAlarmActive = true

                    alertDialog.apply {
                        show()
                        // Set title text color
                        val titleView = findViewById<TextView>(androidx.appcompat.R.id.alertTitle)
                        titleView?.setTextColor(Color.BLACK)
                        // Set message text color
                        findViewById<TextView>(android.R.id.message)?.setTextColor(Color.BLACK)
                    }

                    object : CountDownTimer(10000, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            alertDialog.setMessage("00:${millisUntilFinished / 1000}")
                        }

                        override fun onFinish() {
                            if (alertDialog.isShowing) {
                                alertDialog.dismiss()
                            }
                            Toast.makeText(this@ChargeDetectActivity, "Charging Detection Mode Activated", Toast.LENGTH_SHORT).show()
                            updateUI()
                            startChargingDetectionService()
                            // Storing alarm status value in shared preferences
                            val editor = sharedPreferences.edit()
                            editor.putBoolean("AlarmStatusCharge", isAlarmActive)
                            editor.apply()
                        }
                    }.start()
                } else {
                    stopChargingDetection()
                }
            } else {
                Toast.makeText(this@ChargeDetectActivity, "Connect Charger First", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchBtnV.setOnClickListener {
            isVibrate = !isVibrate
            binding.switchBtnV.setImageResource(if (isVibrate) R.drawable.switch_on else R.drawable.switch_off)
            Toast.makeText(this, if (isVibrate) "Vibration Enabled" else "Vibration Disabled", Toast.LENGTH_SHORT).show()

            // Storing vibrate status value in shared preferences
            val editor = sharedPreferences.edit()
            editor.putBoolean("VibrateStatusCharge", isVibrate)
            editor.apply()
        }

        binding.switchBtnF.setOnClickListener {
            isFlash = !isFlash
            binding.switchBtnF.setImageResource(if (isFlash) R.drawable.switch_on else R.drawable.switch_off)
            Toast.makeText(this, if (isFlash) "Flash Turned on" else "Flash Turned off", Toast.LENGTH_SHORT).show()

            // Storing flash status value in shared preferences
            val editor = sharedPreferences.edit()
            editor.putBoolean("FlashStatusCharge", isFlash)
            editor.apply()
        }
    }

    private fun updateUI() {
        if (isChargerConnected()) {
            binding.powerBtn.setImageResource(if (isAlarmActive) R.drawable.power_off else R.drawable.power_on)
            binding.activateText.text = getString(if (isAlarmActive) R.string.tap_to_deactivate else R.string.tap_to_activate)
        } else {
            binding.powerBtn.setImageResource(R.drawable.charger)
            binding.activateText.text = getString(R.string.please_connect_charger)
        }

        binding.switchBtnV.setImageResource(if (isVibrate) R.drawable.switch_on else R.drawable.switch_off)
        binding.switchBtnF.setImageResource(if (isFlash) R.drawable.switch_on else R.drawable.switch_off)
    }

    private fun stopChargingDetection() {
        Toast.makeText(this@ChargeDetectActivity, "Wifi Detection Mode Deactivated", Toast.LENGTH_SHORT).show()
        binding.powerBtn.setImageResource(R.drawable.power_on)
        binding.activateText.text = getString(R.string.tap_to_activate)
        isAlarmActive = false

        isFlash = false
        binding.switchBtnF.setImageResource(R.drawable.switch_off)

        isVibrate = false
        binding.switchBtnV.setImageResource(R.drawable.switch_off)

        updateUI()

        // Storing alarm status value in shared preferences
        val editor = sharedPreferences.edit()
        editor.putBoolean("AlarmStatusCharge", isAlarmActive)
        editor.putBoolean("FlashStatusCharge", isFlash)
        editor.putBoolean("VibrateStatusCharge", isVibrate)
        editor.apply()

        stopChargingDetectionService()
    }

    override fun onResume() {
        super.onResume()
        val isAlarmServiceActive = sharedPreferences.getBoolean("AlarmServiceStatus", false)
        isAlarmActive = sharedPreferences.getBoolean("AlarmStatusCharge", false)
        isFlash = sharedPreferences.getBoolean("FlashStatusCharge", false)
        isVibrate = sharedPreferences.getBoolean("VibrateStatusCharge", false)

        if (isAlarmServiceActive) {
            // Create a new intent with the necessary extras
            val intent = Intent(this, EnterPinActivity::class.java).apply {
                putExtra("Alarm", isAlarmActive)
                putExtra("Flash", isFlash)
                putExtra("Vibrate", isVibrate)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            finish() // Finish this activity to prevent returning to it
        }
    }

    private fun isChargerConnected(): Boolean {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB
    }

    private fun startChargingDetectionService() {
        isChargingServiceRunning = true
        val serviceIntent = Intent(this, ChargingDetectionService::class.java)
        serviceIntent.putExtra("Alarm", isAlarmActive)
        serviceIntent.putExtra("Flash", isFlash)
        serviceIntent.putExtra("Vibrate", isVibrate)
        startForegroundService(serviceIntent)
    }

    private fun stopChargingDetectionService() {
        isChargingServiceRunning = false
        val serviceIntent = Intent(this, ChargingDetectionService::class.java)
        stopService(serviceIntent)
    }
}