package com.example.securekeep.earphonedetection

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.MainActivity
import com.example.securekeep.R
import com.example.securekeep.alarmsetup.EnterPinActivity
import com.example.securekeep.databinding.ActivityEarphonesBinding
import com.example.securekeep.settings.SettingActivity

class EarphonesActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityEarphonesBinding.inflate(layoutInflater)
    }
    private lateinit var alertDialog: AlertDialog
    private var isAlarmActive = false
    private var isVibrate = false
    private var isFlash = false
    private var isEarphoneServiceRunning = false
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
        isAlarmActive = sharedPreferences.getBoolean("AlarmStatusEarphone", false)
        isVibrate = sharedPreferences.getBoolean("VibrateStatusEarphone", false)
        isFlash = sharedPreferences.getBoolean("FlashStatusEarphone", false)

        isEarphoneServiceRunning = MainActivity.isServiceRunning(this@EarphonesActivity, EarphoneDetectionService::class.java)


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

        if (isEarphonesConnected()) {
            if (isAlarmActive) {
                startEarphoneDetectionService()
            }
            updateUI()
        } else {
            Toast.makeText(this@EarphonesActivity, "Connect Earphones First", Toast.LENGTH_SHORT).show()
            binding.powerBtn.setImageResource(R.drawable.charger)
            binding.powerBtn.setOnClickListener(null)
        }

        binding.powerBtn.setOnClickListener {
            if (isEarphonesConnected()) {
                if (isAlarmActive && isEarphoneServiceRunning) {
                    stopEarphoneDetection()
                } else {
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
                            Toast.makeText(this@EarphonesActivity, "Earphones Detection Mode Activated", Toast.LENGTH_SHORT).show()
                            updateUI()
                            startEarphoneDetectionService()
                            // Storing alarm status value in shared preferences
                            val editor = sharedPreferences.edit()
                            editor.putBoolean("AlarmStatusEarphone", isAlarmActive)
                            editor.apply()
                        }
                    }.start()
                }
            } else {
                Toast.makeText(this@EarphonesActivity, "Connect Earphones First", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchBtnV.setOnClickListener {
            isVibrate = !isVibrate
            binding.switchBtnV.setImageResource(if (isVibrate) R.drawable.switch_on else R.drawable.switch_off)
            Toast.makeText(this, if (isVibrate) "Vibration Enabled" else "Vibration Disabled", Toast.LENGTH_SHORT).show()

            // Storing vibrate status value in shared preferences
            val editor = sharedPreferences.edit()
            editor.putBoolean("VibrateStatusEarphone", isVibrate)
            editor.apply()
        }

        binding.switchBtnF.setOnClickListener {
            isFlash = !isFlash
            binding.switchBtnF.setImageResource(if (isFlash) R.drawable.switch_on else R.drawable.switch_off)
            Toast.makeText(this, if (isFlash) "Flash Turned on" else "Flash Turned off", Toast.LENGTH_SHORT).show()

            // Storing flash status value in shared preferences
            val editor = sharedPreferences.edit()
            editor.putBoolean("FlashStatusEarphone", isFlash)
            editor.apply()
        }
    }

    override fun onResume() {
        super.onResume()
        val isAlarmServiceActive = sharedPreferences.getBoolean("AlarmServiceStatus", false)
        isAlarmActive = sharedPreferences.getBoolean("AlarmStatusEarphone", false)
        isFlash = sharedPreferences.getBoolean("FlashStatusEarphone", false)
        isVibrate = sharedPreferences.getBoolean("VibrateStatusEarphone", false)

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

    private fun stopEarphoneDetection() {
        if (isEarphonesConnected()) {
            isAlarmActive = false

            isFlash = false
            binding.switchBtnF.setImageResource(R.drawable.switch_off)

            isVibrate = false
            binding.switchBtnV.setImageResource(R.drawable.switch_off)

            updateUI()

            // Storing alarm status value in shared preferences
            val editor = sharedPreferences.edit()
            editor.putBoolean("AlarmStatusEarphone", isAlarmActive)
            editor.putBoolean("FlashStatusEarphone", isFlash)
            editor.putBoolean("VibrateStatusEarphone", isVibrate)
            editor.apply()

            stopEarphoneDetectionService()

        }
    }

    private fun updateUI() {
        if (isEarphonesConnected()) {
            binding.powerBtn.setImageResource(if (isAlarmActive) R.drawable.power_off else R.drawable.power_on)
            binding.activateText.text = getString(if (isAlarmActive) R.string.tap_to_deactivate else R.string.tap_to_activate)
        } else {
            binding.powerBtn.setImageResource(R.drawable.earbuds)
            binding.activateText.text = getString(R.string.please_connect_earphones)
        }

        binding.switchBtnV.setImageResource(if (isVibrate) R.drawable.switch_on else R.drawable.switch_off)
        binding.switchBtnF.setImageResource(if (isFlash) R.drawable.switch_on else R.drawable.switch_off)
    }

    private fun isEarphonesConnected(): Boolean {
        return isWiredHeadsetConnected() || isBluetoothHeadsetConnected()
    }

    private fun isWiredHeadsetConnected(): Boolean {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.isWiredHeadsetOn
    }

    private fun isBluetoothHeadsetConnected(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            false
        } else {
            bluetoothAdapter?.bondedDevices?.any {
                it.type == BluetoothDevice.DEVICE_TYPE_LE || it.type == BluetoothDevice.DEVICE_TYPE_CLASSIC
            } ?: false
        }
    }

    private fun startEarphoneDetectionService() {
        isEarphoneServiceRunning = true
        val intent = Intent(this, EarphoneDetectionService::class.java)
        intent.putExtra("Alarm", isAlarmActive)
        intent.putExtra("Flash", isFlash)
        intent.putExtra("Vibrate", isVibrate)
        startService(intent)
    }

    private fun stopEarphoneDetectionService() {
        isEarphoneServiceRunning = false
        val intent = Intent(this, EarphoneDetectionService::class.java)
        stopService(intent)
    }
}
