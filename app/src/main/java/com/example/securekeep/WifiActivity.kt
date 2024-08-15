package com.example.securekeep

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.databinding.ActivityWifiBinding
import com.example.securekeep.settings.SettingActivity
import com.example.securekeep.wifidetection.WifiDetectionService

class WifiActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityWifiBinding.inflate(layoutInflater)
    }
    private lateinit var alertDialog: AlertDialog
    private var isAlarmActive = false
    private var isVibrate = false
    private var isFlash = false
    private val ENTER_PIN_REQUEST_CODE = 1

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
        val preferences = getPreferences(MODE_PRIVATE)
        isAlarmActive = preferences.getBoolean("AlarmStatus", false)
        isVibrate = preferences.getBoolean("VibrateStatus", false)
        isFlash = preferences.getBoolean("FlashStatus", false)

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

                // Storing alarmStatus value in shared preferences
                val editor = getPreferences(MODE_PRIVATE).edit()
                editor.putBoolean("AlarmStatus", isAlarmActive)
                editor.apply()

                alertDialog.show()

                object : CountDownTimer(10000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        alertDialog.setMessage("00:${millisUntilFinished / 1000}")
                    }

                    override fun onFinish() {
                        if (alertDialog.isShowing) {
                            alertDialog.dismiss()
                        }
                        Toast.makeText(this@WifiActivity, "Wi-Fi Detection Mode Activated", Toast.LENGTH_SHORT).show()
                        updateUI()
                        startWifiDetectionService()
                    }
                }.start()
            } else {
                stopWifiDetectionService()
            }
        }

        binding.switchBtnV.setOnClickListener {
            isVibrate = !isVibrate
            binding.switchBtnV.setImageResource(if (isVibrate) R.drawable.switch_on else R.drawable.switch_off)
            Toast.makeText(this, if (isVibrate) "Vibration Enabled" else "Vibration Disabled", Toast.LENGTH_SHORT).show()

            // Storing vibrate status value in shared preferences
            val editor = getPreferences(MODE_PRIVATE).edit()
            editor.putBoolean("VibrateStatus", isVibrate)
            editor.apply()
        }

        binding.switchBtnF.setOnClickListener {
            isFlash = !isFlash
            binding.switchBtnF.setImageResource(if (isFlash) R.drawable.switch_on else R.drawable.switch_off)
            Toast.makeText(this, if (isFlash) "Flash Turned on" else "Flash Turned off", Toast.LENGTH_SHORT).show()

            // Storing flash status value in shared preferences
            val editor = getPreferences(MODE_PRIVATE).edit()
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

    private fun startWifiDetectionService() {
        val serviceIntent = Intent(this, WifiDetectionService::class.java)
        startForegroundService(serviceIntent)
    }

    private fun stopWifiDetectionService() {
        val serviceIntent = Intent(this, WifiDetectionService::class.java)
        stopService(serviceIntent)
        // Reset alarm state in shared preferences
        val editor = getPreferences(MODE_PRIVATE).edit()
        editor.putBoolean("AlarmStatus", false)
        editor.apply()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ENTER_PIN_REQUEST_CODE && resultCode == RESULT_OK) {
            stopWifiDetectionService() // Stop Wi-Fi detection when alarm is deactivated
        }
    }
}
