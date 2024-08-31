package com.example.securekeep.antipocket

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
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
import com.example.securekeep.databinding.ActivityAntiPocketBinding
import com.example.securekeep.settings.SettingActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AntiPocketActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityAntiPocketBinding.inflate(layoutInflater)
    }
    private lateinit var alertDialog: AlertDialog
    private var isAlarmActive = false
    private var isVibrate = false
    private var isFlash = false
    private var isPocketServiceRunning = false
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
        isAlarmActive = sharedPreferences.getBoolean("AlarmStatusPocket", false)
        isVibrate = sharedPreferences.getBoolean("VibrateStatusPocket", false)
        isFlash = sharedPreferences.getBoolean("FlashStatusPocket", false)

        isPocketServiceRunning = MainActivity.isServiceRunning(this@AntiPocketActivity, ProximityDetectionService::class.java)

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

        binding.powerBtn.setOnClickListener {
            if (!isAlarmActive && !isPocketServiceRunning) {
                isAlarmActive = true

                alertDialog.apply {
                    show()
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
                        Toast.makeText(this@AntiPocketActivity, "Anti Pocket Mode Activated", Toast.LENGTH_SHORT).show()
                        updateUI()
                        startProximityDetectionService()
                        // Storing alarm status value in shared preferences
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("AlarmStatusPocket", isAlarmActive)
                        editor.apply()
                    }
                }.start()
            } else {
                stopProximityDetection()
            }
        }

        binding.switchBtnV.setOnClickListener {
            isVibrate = !isVibrate
            binding.switchBtnV.setImageResource(if (isVibrate) R.drawable.switch_on else R.drawable.switch_off)
            Toast.makeText(this, if (isVibrate) "Vibration Enabled" else "Vibration Disabled", Toast.LENGTH_SHORT).show()

            // Storing vibrate status value in shared preferences
            val editor = sharedPreferences.edit()
            editor.putBoolean("VibrateStatusPocket", isVibrate)
            editor.apply()
        }

        binding.switchBtnF.setOnClickListener {
            isFlash = !isFlash
            binding.switchBtnF.setImageResource(if (isFlash) R.drawable.switch_on else R.drawable.switch_off)
            Toast.makeText(this, if (isFlash) "Flash Turned on" else "Flash Turned off", Toast.LENGTH_SHORT).show()

            // Storing flash status value in shared preferences
            val editor = sharedPreferences.edit()
            editor.putBoolean("FlashStatusPocket", isFlash)
            editor.apply()
        }
    }

    private fun updateUI() {
        if (isAlarmActive) {
            binding.powerBtn.setImageResource(R.drawable.power_off)
            binding.activateText.text = getString(R.string.tap_to_deactivate)
        } else {
            binding.powerBtn.setImageResource(R.drawable.power_on)
            binding.activateText.text = getString(R.string.tap_to_activate)
        }

        binding.switchBtnV.setImageResource(if (isVibrate) R.drawable.switch_on else R.drawable.switch_off)
        binding.switchBtnF.setImageResource(if (isFlash) R.drawable.switch_on else R.drawable.switch_off)
    }

    override fun onResume() {
        super.onResume()
        val isAlarmServiceActive = sharedPreferences.getBoolean("AlarmServiceStatus", false)
        isAlarmActive = sharedPreferences.getBoolean("AlarmStatusPocket", false)
        isFlash = sharedPreferences.getBoolean("FlashStatusPocket", false)
        isVibrate = sharedPreferences.getBoolean("VibrateStatusPocket", false)

        Log.d("PocketActivity", "onResume: Alarm: $isAlarmActive, Flash: $isFlash, Vibrate: $isVibrate")

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

    private fun stopProximityDetection() {
        Toast.makeText(this@AntiPocketActivity, "Anti Pocket Mode Deactivated", Toast.LENGTH_SHORT).show()
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
        editor.putBoolean("AlarmStatusPocket", isAlarmActive)
        editor.putBoolean("FlashStatusPocket", isFlash)
        editor.putBoolean("VibrateStatusPocket", isVibrate)
        editor.apply()

        stopProximityDetectionService()
    }


    private fun startProximityDetectionService() {
        isPocketServiceRunning = true
        val serviceIntent = Intent(this, ProximityDetectionService::class.java)
        serviceIntent.putExtra("Alarm", isAlarmActive)
        serviceIntent.putExtra("Flash", isFlash)
        serviceIntent.putExtra("Vibrate", isVibrate)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopProximityDetectionService() {
        isPocketServiceRunning = false
        val serviceIntent = Intent(this, ProximityDetectionService::class.java)
        stopService(serviceIntent)
    }
}
