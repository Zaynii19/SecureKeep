package com.example.securekeep.antipocket

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.R
import com.example.securekeep.alarmsetup.AlarmService
import com.example.securekeep.alarmsetup.EnterPinActivity
import com.example.securekeep.databinding.ActivityAntiPocketBinding
import com.example.securekeep.settings.SettingActivity

class AntiPocketActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityAntiPocketBinding.inflate(layoutInflater)
    }
    private lateinit var alertDialog: AlertDialog
    private var isAlarmActive = false
    private var isVibrate = false
    private var isFlash = false
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
        isAlarmActive = sharedPreferences.getBoolean("AlarmStatus", false)
        isVibrate = sharedPreferences.getBoolean("VibrateStatus", false)
        isFlash = sharedPreferences.getBoolean("FlashStatus", false)

        updateUI()

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.settingBtn.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        alertDialog = AlertDialog.Builder(this)
            .setTitle("Will Be Activated In 10 Seconds")
            .setMessage("00:10")
            .setCancelable(false)
            .create()

        binding.powerBtn.setOnClickListener {
            if (!isAlarmActive) {
                isAlarmActive = true
                alertDialog.show()

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
            val editor = getSharedPreferences("AlarmPrefs", MODE_PRIVATE).edit()
            editor.putBoolean("VibrateStatus", isVibrate)
            editor.apply()
        }

        binding.switchBtnF.setOnClickListener {
            isFlash = !isFlash
            binding.switchBtnF.setImageResource(if (isFlash) R.drawable.switch_on else R.drawable.switch_off)
            Toast.makeText(this, if (isFlash) "Flash Turned on" else "Flash Turned off", Toast.LENGTH_SHORT).show()

            // Storing flash status value in shared preferences
            val editor = getSharedPreferences("AlarmPrefs", MODE_PRIVATE).edit()
            editor.putBoolean("FlashStatus", isFlash)
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
        // Check if the alarm service is active when the activity resumes
        val isAlarmServiceActive = sharedPreferences.getBoolean("AlarmServiceStatus",false)

        if (isAlarmServiceActive) {
            // Start EnterPinActivity if the alarm is active
            startActivity(Intent(this, EnterPinActivity::class.java))
            finish() // Optionally finish this activity if you want to prevent the user from returning to it
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
        editor.putBoolean("AlarmStatus", isAlarmActive)
        editor.putBoolean("FlashStatus", isFlash)
        editor.putBoolean("VibrateStatus", isVibrate)
        editor.apply()

        stopProximityDetectionService()
    }


    private fun startProximityDetectionService() {
        val serviceIntent = Intent(this, ProximityDetectionService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopProximityDetectionService() {
        val serviceIntent = Intent(this, ProximityDetectionService::class.java)
        stopService(serviceIntent)
    }
}
