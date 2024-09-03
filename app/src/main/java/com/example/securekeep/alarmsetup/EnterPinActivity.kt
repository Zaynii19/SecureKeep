
package com.example.securekeep.alarmsetup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.MainActivity
import com.example.securekeep.MyPreferences
import com.example.securekeep.R
import com.example.securekeep.databinding.ActivityEnterPinBinding
import java.io.IOException

class EnterPinActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityEnterPinBinding.inflate(layoutInflater)
    }
    private lateinit var pinDots: Array<View>
    private var enteredPin = ""
    private var currentPin = ""
    private var isVibrate = false
    private var isFlash = false
    private var isAlarmServiceActive = false
    private val vibrationPattern = longArrayOf(0, 500, 500) // Wait, Vibrate for 1 sec, Wait for 1 sec
    private lateinit var vibrator: Vibrator
    var fromPin = false
    private var currentSoundLevel = 0
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private lateinit var handler: Handler
    private lateinit var audioManager: AudioManager
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var alarmPreferences: SharedPreferences
    private lateinit var preferences: MyPreferences

    private val flashRunnable = object : Runnable {
        override fun run() {
            toggleFlashlight()
            handler.postDelayed(this, 1000) // Flash every second
        }
    }

    private val volumeCheckRunnable = object : Runnable {
        override fun run() {
            resetVolumeIfChanged()
            handler.postDelayed(this, 1000) // Check every second
        }
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList[0] // Assuming back camera
        handler = Handler(Looper.getMainLooper())
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        preferences = MyPreferences(this@EnterPinActivity)
        handler.post(volumeCheckRunnable)  // Start volume check task

        // Initialize SharedPreferences
        alarmPreferences = getSharedPreferences("PinAndService", MODE_PRIVATE)
        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        currentPin = sharedPreferences.getString("USER_PIN", "")!!
        isAlarmServiceActive = sharedPreferences.getBoolean("AlarmServiceStatus",false)
        // Retrieve sound level from shared preferences
        currentSoundLevel = sharedPreferences.getInt("SOUND_LEVEL", 70)
        // Retrieve the saved tone
        val toneId = sharedPreferences.getInt("alarm_tone", R.raw.alarm_tune_1)
        val toneUriString = sharedPreferences.getString("alarm_tone_uri", null)
        val systemToneUri = toneUriString?.let { Uri.parse(it) } // Convert to Uri if exists

        // set sound and Create MediaPlayer based on tone URI or ID
        setSystemSoundLevel(currentSoundLevel)

        // Create MediaPlayer based on tone URI or ID
        mediaPlayer = if (systemToneUri != null) {
            try {
                Log.d("EnterPinActivity", "systemToneUri: $systemToneUri")
                MediaPlayer().apply {
                    setDataSource(this@EnterPinActivity, systemToneUri)
                    prepare()
                    isLooping = true
                }
            } catch (e: IOException) {
                Log.e("EnterPinActivity", "Error setting data source", e)
                null // Handle the null case. You might want to set a default sound here.
            }
        } else {
            MediaPlayer.create(this, toneId).apply {
                isLooping = true
            }
        }

        val intent = intent
        isVibrate = intent.getBooleanExtra("Vibrate", false)
        isFlash = intent.getBooleanExtra("Flash", false)


        pinDots = arrayOf(
            binding.pinDot1, binding.pinDot2, binding.pinDot3, binding.pinDot4
        )

        stopAlarmService()


        fromPin = false

        // Activate alarm
        triggerAlarm()

        setupPinButtons()
    }

    private fun setupPinButtons() {
        val buttonIds = listOf(
            binding.btn0 to "0",
            binding.btn1 to "1",
            binding.btn2 to "2",
            binding.btn3 to "3",
            binding.btn4 to "4",
            binding.btn5 to "5",
            binding.btn6 to "6",
            binding.btn7 to "7",
            binding.btn8 to "8",
            binding.btn9 to "9",
            binding.clearBtn to "CLEAR"
        )

        buttonIds.forEach { (buttons, value) ->
            buttons.setOnClickListener {
                if (value == "CLEAR") {
                    onClearClick()
                } else {
                    onDigitClick(value)
                    pinCheck()
                }
            }
        }
    }

    private fun onDigitClick(digit: String) {
        if (enteredPin.length < 4) {
            enteredPin += digit
            updatePinDots()
        }
    }

    private fun updatePinDots() {
        pinDots.forEachIndexed { index, view ->
            if (index < enteredPin.length) {
                view.setBackgroundColor(Color.GREEN)
            } else {
                view.setBackgroundColor(Color.WHITE)
            }
        }
    }

    private fun onClearClick() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.substring(0, enteredPin.length - 1)
            updatePinDots()
        }
    }

    private fun pinCheck() {
        if (enteredPin.length == 4) {
            if (enteredPin == currentPin) {

                stopAlarm()
                stopAlarmService()

                isAlarmServiceActive = false
                val editor = sharedPreferences.edit()
                editor.putBoolean("AlarmServiceStatus", isAlarmServiceActive)
                editor.apply()

                val resultIntent = Intent()
                setResult(Activity.RESULT_OK, resultIntent)
                startActivity(Intent(this@EnterPinActivity, MainActivity::class.java))
                finish()
            } else {
                clearAllPinDots()
                Toast.makeText(this, "Wrong Pin", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearAllPinDots() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = ""
            updatePinDots()
        }
    }

    private fun triggerAlarm() {
        mediaPlayer?.apply {
            if (!isPlaying) {
                start()
            }
        }

        if (isVibrate) {
            // Cancel any existing vibration before starting a new one
            vibrator.cancel()
            triggerVibration()
        }

        if (isFlash) {
            handler.post(flashRunnable)
        }
    }

    private fun stopAlarm() {
        preferences.storePreferences()

        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
                prepare() // Prepare the MediaPlayer for future use
            }
            isLooping = false // Set looping to false
        }

        vibrator.cancel() // Stop vibration

        handler.removeCallbacks(flashRunnable)
        if (isFlash) {
            toggleFlashlight()
        }

        // Stop volume check when service is destroyed
        handler.removeCallbacks(volumeCheckRunnable)
    }

    private fun triggerVibration() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // For API level 34 and above
                val vibrationEffect = VibrationEffect.createWaveform(
                    vibrationPattern,
                    0 // Repeat indefinitely
                )
                vibrator.vibrate(vibrationEffect, VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_ALARM)
                    .build())
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                // For API levels 26 to 33
                val vibrationEffect = VibrationEffect.createWaveform(
                    vibrationPattern,
                    0 // Repeat indefinitely
                )
                vibrator.vibrate(vibrationEffect)
            }
            else -> {
                // For API levels below 26
                vibrator.vibrate(vibrationPattern, 0) // Repeat indefinitely
            }
        }
    }

    private fun toggleFlashlight() {
        try {
            cameraManager.setTorchMode(cameraId, !isFlash)
            isFlash = !isFlash
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            Toast.makeText(this, "Error toggling flashlight", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        fromPin = true

        val editor = alarmPreferences.edit()
        editor.putBoolean("VibrateStatus", isVibrate)
        editor.putBoolean("FlashStatus", isFlash)
        editor.apply()

        Log.d("PinActivity", "onPause: Flash: $isFlash Vibrate: $isVibrate")

        stopAlarm()
        startAlarmService()
    }

    override fun onResume() {
        super.onResume()
        val intent = intent
        isVibrate = intent.getBooleanExtra("Vibrate", false)
        isFlash = intent.getBooleanExtra("Flash", false)


        stopAlarmService()
        // Re-initialize the Vibrator service
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        triggerAlarm()
    }

    private fun setSystemSoundLevel(level: Int) {
        val clampedLevel = level.coerceIn(0, 100)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = (clampedLevel * maxVolume) / 100
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0) // Remove FLAG_SHOW_UI
    }

    private fun startAlarmService() {
        val serviceIntent = Intent(this, AlarmService::class.java).apply {
            putExtra("Vibrate", isVibrate)
            putExtra("Flash", isFlash)
            putExtra("FromPin", fromPin)  // Pass fromPin
        }

        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopAlarmService() {
        val serviceIntent = Intent(this, AlarmService::class.java)
        stopService(serviceIntent)
    }

    private fun resetVolumeIfChanged() {
        currentSoundLevel = sharedPreferences.getInt("SOUND_LEVEL", 70)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val preferredVolume = (currentSoundLevel * maxVolume) / 100
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        if (currentVolume != preferredVolume) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, preferredVolume, 0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("Vibrate", isVibrate)
        outState.putBoolean("Flash", isFlash)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isVibrate = savedInstanceState.getBoolean("Vibrate", false)
        isFlash = savedInstanceState.getBoolean("Flash", false)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        if (enteredPin == currentPin){
            stopAlarmService()
        }else{
            startAlarmService()
        }
    }

    override fun onBackPressed() {
        if (enteredPin == currentPin) {
            super.onBackPressed()
            // Do nothing here to prevent default back button behavior
        } else {
            Toast.makeText(this, "PIN required to exit!", Toast.LENGTH_SHORT).show()
            // Handle the case where the condition is not met
        }
    }

}
