package com.example.securekeep

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.PowerManager
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.databinding.ActivityAntiPocketBinding

class AntiPocketActivity : AppCompatActivity(), SensorEventListener {

    private val binding by lazy {
        ActivityAntiPocketBinding.inflate(layoutInflater)
    }

    private lateinit var alertDialog: AlertDialog
    private var isAlarmActive = false
    private var isVibrate = false
    private var isFlash = false

    private lateinit var sensorManager: SensorManager
    private lateinit var proximitySensor: Sensor
    private lateinit var wakeLock: PowerManager.WakeLock
    private var isWakeLockAcquired = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize SensorManager and Proximity Sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)!!

        // Initialize WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::AntiPocketActivityWakelock")

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
                        Toast.makeText(this@AntiPocketActivity, "Anti Pocket Mode Activated", Toast.LENGTH_SHORT).show()
                        updateUI()
                        startProximityDetection()
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

    private fun startProximityDetection() {
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
        isWakeLockAcquired = true
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_UI)
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

        // Storing alarmStatus value in shared preferences
        val editor = getPreferences(MODE_PRIVATE).edit()
        editor.putBoolean("AlarmStatus", isAlarmActive)
        editor.apply()

        sensorManager.unregisterListener(this)
        if (isWakeLockAcquired) {
            wakeLock.release()
            isWakeLockAcquired = false
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PROXIMITY && isAlarmActive) {
            if (event.values[0] < proximitySensor.maximumRange) {
                triggerAlarm()
            }
        }
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

            val alarmIntent = Intent(this, EnterPinActivity::class.java)
            alarmIntent.putExtra("IS_VIBRATE", isVibrate)
            alarmIntent.putExtra("IS_FLASH", isFlash)
            startActivity(alarmIntent)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do nothing
    }
}
