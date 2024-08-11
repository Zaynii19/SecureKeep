package com.example.securekeep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.databinding.ActivityOverChargeBinding

class OverChargeActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityOverChargeBinding.inflate(layoutInflater)
    }
    private lateinit var alertDialog: AlertDialog
    private var isAlarmActive = false
    private var isVibrate = false
    private var isFlash = false
    private var isReceiverRegistered = false
    private val ENTER_PIN_REQUEST_CODE = 1

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == Intent.ACTION_BATTERY_CHANGED) {
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val batteryPct = (level / scale.toFloat()) * 100

                // Check if battery is charging or full
                if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) {
                    if (batteryPct >= 100) {
                        triggerAlarm()
                    }
                }
            }
        }
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
                        Toast.makeText(this@OverChargeActivity, "Battery Detection Mode Activated", Toast.LENGTH_SHORT).show()
                        updateUI()
                        startBatteryDetection()
                    }
                }.start()
            } else {
                stopBatteryDetection()
                isAlarmActive = false

                // Storing alarmStatus value in shared preferences
                val editor = getPreferences(MODE_PRIVATE).edit()
                editor.putBoolean("AlarmStatus", isAlarmActive)
                editor.apply()
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
            startBatteryDetection()
        } else {
            binding.powerBtn.setImageResource(R.drawable.power_on)
            binding.activateText.text = getString(R.string.tap_to_activate)
        }

        binding.switchBtnV.setImageResource(if (isVibrate) R.drawable.switch_on else R.drawable.switch_off)
        binding.switchBtnF.setImageResource(if (isFlash) R.drawable.switch_on else R.drawable.switch_off)
    }

    private fun startBatteryDetection() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
        isReceiverRegistered = true
    }

    private fun triggerAlarm() {
        if (isAlarmActive) {

            isAlarmActive = false
            // Update the UI after deactivating the alarm
            updateUI()

            // Save the updated alarm status
            val editor = getPreferences(MODE_PRIVATE).edit()
            editor.putBoolean("AlarmStatus", isAlarmActive)
            editor.apply()

            Toast.makeText(this, "Battery is fully charged! Enter PIN", Toast.LENGTH_SHORT).show()
            val alarmIntent = Intent(this, EnterPinActivity::class.java)
            alarmIntent.putExtra("IS_VIBRATE", isVibrate)
            alarmIntent.putExtra("IS_FLASH", isFlash)
            startActivityForResult(alarmIntent, ENTER_PIN_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ENTER_PIN_REQUEST_CODE && resultCode == RESULT_OK) {
            stopBatteryDetection() // Stop battery detection when alarm is deactivated
        }
    }

    private fun stopBatteryDetection() {
        Toast.makeText(this@OverChargeActivity, "Battery Detection Mode Deactivated", Toast.LENGTH_SHORT).show()
        binding.powerBtn.setImageResource(R.drawable.power_on)
        binding.activateText.text = getString(R.string.tap_to_activate)
        isAlarmActive = false

        if (isReceiverRegistered) {
            unregisterReceiver(batteryReceiver)
            isReceiverRegistered = false
        }

        // Storing alarmStatus value in shared preferences
        val editor = getPreferences(MODE_PRIVATE).edit()
        editor.putBoolean("AlarmStatus", isAlarmActive)
        editor.apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            unregisterReceiver(batteryReceiver)
        }
    }
}
