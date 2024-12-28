package com.example.securekeep.earphonedetection

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.MainActivity
import com.example.securekeep.R
import com.example.securekeep.alarmsetup.EnterPinActivity
import com.example.securekeep.databinding.ActivityEarphonesBinding
import com.example.securekeep.settings.SettingActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
    private lateinit var alarmPreferences: SharedPreferences

    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSION = 100
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
        alarmPreferences = getSharedPreferences("PinAndService", MODE_PRIVATE)
        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        isAlarmActive = sharedPreferences.getBoolean("AlarmStatusEarphone", false)
        isVibrate = sharedPreferences.getBoolean("VibrateStatusEarphone", false)
        isFlash = sharedPreferences.getBoolean("FlashStatusEarphone", false)

        isEarphoneServiceRunning = MainActivity.isServiceRunning(this@EarphonesActivity, EarphoneDetectionService::class.java)

        setupEarphonesDetection()

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.settingBtn.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }
    }

    private fun setupEarphonesDetection() {
        if (isEarphonesConnected()) {
            if (isAlarmActive) {
                startEarphoneDetectionService()
            }
            updateUI()
        } else {
            Toast.makeText(this@EarphonesActivity, "Connect Earphones First", Toast.LENGTH_SHORT).show()
            binding.powerBtn.setImageResource(R.drawable.earbuds)
            binding.powerBtn.setOnClickListener(null)
        }

        alertDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Will Be Activated In 10 Seconds")
            .setMessage("00:10")
            .setBackground(ContextCompat.getDrawable(this, R.drawable.simple_round_boarder))
            .setCancelable(false)
            .create()

        binding.powerBtn.setOnClickListener {
            // Check for Bluetooth permission before proceeding
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                // Proceed with earphones detection
                if (isEarphonesConnected()) {
                    if (isAlarmActive && isEarphoneServiceRunning) {
                        stopEarphoneDetection()
                    } else {
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
            } else {
                // Request Bluetooth permission
                requestBluetoothPermission()
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

    @SuppressLint("InlinedApi")
    private fun requestBluetoothPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
            REQUEST_BLUETOOTH_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_BLUETOOTH_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupEarphonesDetection()
                } else {
                    // If permission is denied, show a Toast and open system settings
                    Toast.makeText(this, "Bluetooth permission is required", Toast.LENGTH_SHORT).show()
                    openAppSettings()
                }
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        val isAlarmServiceActive = sharedPreferences.getBoolean("AlarmServiceStatus", false)
        isAlarmActive = sharedPreferences.getBoolean("AlarmStatusEarphone", false)
        isFlash = alarmPreferences.getBoolean("FlashStatus", false)
        isVibrate = alarmPreferences.getBoolean("VibrateStatus", false)

        if (isAlarmServiceActive) {
            // Create a new intent with the necessary extras
            val intent = Intent(this, EnterPinActivity::class.java).apply {
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
            binding.activateText.text =
                getString(if (isAlarmActive) R.string.tap_to_deactivate else R.string.tap_to_activate)
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
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        for (device in devices) {
            if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                return true
            }
        }
        return false
    }

    private fun isBluetoothHeadsetConnected(): Boolean {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

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
