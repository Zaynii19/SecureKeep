package com.example.securekeep

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.databinding.ActivityEarphonesBinding

class EarphonesActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityEarphonesBinding.inflate(layoutInflater)
    }
    private lateinit var alertDialog: AlertDialog
    private var isAlarmActive = false
    private var isReceiverRegistered = false
    private var isVibrate = false
    private var isFlash = false
    private val ENTER_PIN_REQUEST_CODE = 1
    private val REQUEST_CODE_BLUETOOTH_PERMISSION = 1

    private val audioReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
            if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                triggerAlarm()
            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
                triggerAlarm()
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

        // Retrieve saved states
        val preferences = getPreferences(MODE_PRIVATE)
        isAlarmActive = preferences.getBoolean("AlarmStatus", false)
        isVibrate = preferences.getBoolean("VibrateStatus", false)
        isFlash = preferences.getBoolean("FlashStatus", false)
        updateUI()

        checkBluetoothPermissions()

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
            if (isEarphonesConnected()) {
                if (isAlarmActive) {
                    isAlarmActive = false
                    // Deactivate the alarm
                    stopEarphoneDetection()
                    Toast.makeText(this, "Earphones Detection Mode Deactivated", Toast.LENGTH_SHORT).show()
                } else {
                    // Activate the alarm
                    isAlarmActive = true
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
                            Toast.makeText(this@EarphonesActivity, "Earphones Detection Mode Activated", Toast.LENGTH_SHORT).show()
                            updateUI()
                            startEarphoneDetection()
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

            val editor = getPreferences(MODE_PRIVATE).edit()
            editor.putBoolean("VibrateStatus", isVibrate)
            editor.apply()
        }

        binding.switchBtnF.setOnClickListener {
            isFlash = !isFlash
            binding.switchBtnF.setImageResource(if (isFlash) R.drawable.switch_on else R.drawable.switch_off)
            Toast.makeText(this, if (isFlash) "Flash Turned on" else "Flash Turned off", Toast.LENGTH_SHORT).show()

            val editor = getPreferences(MODE_PRIVATE).edit()
            editor.putBoolean("FlashStatus", isFlash)
            editor.apply()
        }
    }


    private fun checkBluetoothPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_CODE_BLUETOOTH_PERMISSION)
            }
        } else {
            setupUI()
        }
    }

    private fun setupUI() {
        if (isEarphonesConnected()) {
            if (isAlarmActive) {
                startEarphoneDetection()
            }
        } else {
            Toast.makeText(this@EarphonesActivity, "Connect Earphones First", Toast.LENGTH_SHORT).show()
            binding.powerBtn.setImageResource(R.drawable.earbuds)
            binding.powerBtn.setOnClickListener(null)
        }
    }

    private fun updateUI() {
        if (isEarphonesConnected()) {
            binding.powerBtn.setImageResource(if (isAlarmActive) R.drawable.power_off else R.drawable.power_on)
            binding.activateText.text = getString(if (isAlarmActive) R.string.tap_to_deactivate else R.string.tap_to_activate)
        } else {
            binding.powerBtn.setImageResource(R.drawable.charger)
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

    private fun triggerAlarm() {
        if (isAlarmActive) {
            isAlarmActive = false

            // Update the UI after deactivating the alarm
            updateUI()

            // Save the updated alarm status
            val editor = getPreferences(MODE_PRIVATE).edit()
            editor.putBoolean("AlarmStatus", isAlarmActive)
            editor.apply()

            Toast.makeText(this, "Earphones disconnected! Enter PIN", Toast.LENGTH_SHORT).show()
            val alarmIntent = Intent(this, EnterPinActivity::class.java)
            alarmIntent.putExtra("IS_VIBRATE", isVibrate)
            alarmIntent.putExtra("IS_FLASH", isFlash)
            startActivityForResult(alarmIntent, ENTER_PIN_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ENTER_PIN_REQUEST_CODE && resultCode == RESULT_OK) {
            stopEarphoneDetection()
        }
    }

    private fun startEarphoneDetection() {
        if (isWiredHeadsetConnected()) {
            val filter = IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            registerReceiver(audioReceiver, filter)
            isReceiverRegistered = true
        } else if (isBluetoothHeadsetConnected()) {
            val bluetoothFilter = IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            registerReceiver(bluetoothReceiver, bluetoothFilter)
            isReceiverRegistered = true
        }
    }

    private fun stopEarphoneDetection() {
        if (isWiredHeadsetConnected()) {
            isAlarmActive = false
            if (isReceiverRegistered) {
                unregisterReceiver(audioReceiver)
                isFlash = false
                binding.switchBtnF.setImageResource(R.drawable.switch_off)

                isVibrate = false
                binding.switchBtnV.setImageResource(R.drawable.switch_off)
                updateUI()
                // Save the updated alarm status
                val editor = getPreferences(MODE_PRIVATE).edit()
                editor.putBoolean("AlarmStatus", isAlarmActive)
                editor.apply()

            }
        } else if (isBluetoothHeadsetConnected()){
            if (isReceiverRegistered) {
                unregisterReceiver(bluetoothReceiver)
                isFlash = false
                binding.switchBtnF.setImageResource(R.drawable.switch_off)

                isVibrate = false
                binding.switchBtnV.setImageResource(R.drawable.switch_off)
                isReceiverRegistered = false
                updateUI()
                // Save the updated alarm status
                val editor = getPreferences(MODE_PRIVATE).edit()
                editor.putBoolean("AlarmStatus", isAlarmActive)
                editor.apply()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isWiredHeadsetConnected()) {
            isAlarmActive = false
            if (isReceiverRegistered) {
                unregisterReceiver(audioReceiver)

            } else if (isBluetoothHeadsetConnected()){
                unregisterReceiver(bluetoothReceiver)
                isReceiverRegistered = false
            }
        }
    }
}
