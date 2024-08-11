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
import com.example.securekeep.databinding.ActivityChargeDetectBinding

class ChargeDetectActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityChargeDetectBinding.inflate(layoutInflater)
    }
    private lateinit var alertDialog: AlertDialog
    private var isAlarmActive = false
    private var isVibrate = false
    private var isFlash = false
    private var isReceiverRegistered = false

    // Handle Charging Detection
    private val chargingReceiver = object : BroadcastReceiver() {
        private var wasPlugged = false

        override fun onReceive(context: Context, intent: Intent) {
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

            if (plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB) {
                if (!wasPlugged) {
                    wasPlugged = true
                }
            } else if (plugged == 0 && wasPlugged) {
                wasPlugged = false
                isAlarmActive = false

                // Update the UI after activating the alarm
                updatePowerButtonUI()

                // Save the updated alarm status
                val editor = getPreferences(MODE_PRIVATE).edit()
                editor.putBoolean("AlarmStatus", isAlarmActive)
                editor.apply()

                Toast.makeText(context, "Charger Disconnected", Toast.LENGTH_SHORT).show()
                val alarmIntent = Intent(context, EnterPinActivity::class.java)
                alarmIntent.putExtra("IS_VIBRATE", isVibrate)
                alarmIntent.putExtra("IS_FLASH", isFlash)
                context.startActivity(alarmIntent)
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

        // Retrieving saved states
        val preferences = getPreferences(MODE_PRIVATE)
        isAlarmActive = preferences.getBoolean("AlarmStatus", false)
        isVibrate = preferences.getBoolean("VibrateStatus", false)
        isFlash = preferences.getBoolean("FlashStatus", false)

        updatePowerButtonUI()

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

        // Check if charger is connected on activity creation
        if (isChargerConnected()) {
            if (isAlarmActive) {
                startChargingDetection()
            }
            updatePowerButtonUI()
        } else {
            Toast.makeText(this@ChargeDetectActivity, "Connect Charger First", Toast.LENGTH_SHORT).show()
            binding.powerBtn.setImageResource(R.drawable.charger)
            binding.powerBtn.setOnClickListener(null)
        }

        binding.powerBtn.setOnClickListener {
            if (isChargerConnected()) {
                if (!isAlarmActive) {
                    isAlarmActive = true

                    // Storing alarm status value in shared preferences
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
                            Toast.makeText(this@ChargeDetectActivity, "Charging Detection Mode Activated", Toast.LENGTH_SHORT).show()
                            updatePowerButtonUI()
                            startChargingDetection()
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

    private fun updatePowerButtonUI() {
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

    private fun isChargerConnected(): Boolean {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB
    }

    private fun startChargingDetection() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(chargingReceiver, filter)
        isReceiverRegistered = true
    }

    private fun stopChargingDetection() {
        Toast.makeText(this@ChargeDetectActivity, "Charging Detection Mode Deactivated", Toast.LENGTH_SHORT).show()
        isAlarmActive = false

        isFlash = false
        binding.switchBtnF.setImageResource(R.drawable.switch_off)

        isVibrate = false
        binding.switchBtnV.setImageResource(R.drawable.switch_off)

        // Storing alarm status value in shared preferences
        val editor = getPreferences(MODE_PRIVATE).edit()
        editor.putBoolean("AlarmStatus", isAlarmActive)
        editor.apply()

        updatePowerButtonUI()

        if (isReceiverRegistered) {
            unregisterReceiver(chargingReceiver)
            isReceiverRegistered = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the ChargingReceiver if it's still registered
        if (isReceiverRegistered) {
            unregisterReceiver(chargingReceiver)
        }
    }
}
