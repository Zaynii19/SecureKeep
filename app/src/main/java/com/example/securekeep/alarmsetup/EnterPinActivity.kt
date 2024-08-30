
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
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
    private var isAlarmActive = false
    private var isAlarmServiceActive = false
    var fromPin = false
    private var currentSoundLevel = 0
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private lateinit var handler: Handler
    private lateinit var audioManager: AudioManager
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var pinAlarmPreferences: SharedPreferences
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
        preferences = MyPreferences(this@EnterPinActivity)
        handler.post(volumeCheckRunnable)  // Start volume check task

        // Initialize SharedPreferences
        pinAlarmPreferences = getSharedPreferences("PinAlarmPreferences", MODE_PRIVATE)
        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        currentPin = sharedPreferences.getString("USER_PIN", "")!!
        isAlarmServiceActive = sharedPreferences.getBoolean("AlarmServiceStatus",false)
        // Retrieve sound level from shared preferences
        currentSoundLevel = sharedPreferences.getInt("SOUND_LEVEL", 70)
        setSystemSoundLevel(currentSoundLevel)

        // Retrieve the saved tone
        val toneId = sharedPreferences.getInt("alarm_tone", R.raw.alarm_tune_1)
        val toneUriString = sharedPreferences.getString("alarm_tone_uri", null)
        val systemToneUri = toneUriString?.let { Uri.parse(it) } // Convert to Uri if exists

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
        isAlarmActive = intent.getBooleanExtra("Alarm", false)
        isVibrate = intent.getBooleanExtra("Vibrate", false)
        isFlash = intent.getBooleanExtra("Flash", false)

        Log.d("PinActivity", "onCreate: Alarm: $isAlarmActive Flash: $isFlash Vibrate: $isVibrate")


        pinDots = arrayOf(
            binding.pinDot1, binding.pinDot2, binding.pinDot3, binding.pinDot4
        )

        if (isAlarmServiceActive) {
            stopAlarmService()
        }

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
                Toast.makeText(this, "Wrong Pin", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun triggerAlarm() {
        mediaPlayer?.apply {
            if (!isPlaying) {
                start()
            }
        }

        if (isVibrate) {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 (API level 31) and above
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.cancel()
        } else {
            // For Android versions below 12
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.cancel()
        }

        handler.removeCallbacks(flashRunnable)
        if (isFlash) {
            toggleFlashlight()
        }

        // Stop volume check when service is destroyed
        handler.removeCallbacks(volumeCheckRunnable)
    }

    private fun triggerVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 (API level 31) and above
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            // For Android versions below 12
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
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

        val editor = pinAlarmPreferences.edit()
        editor.putBoolean("AlarmStatus", isAlarmActive)
        editor.putBoolean("VibrateStatus", isVibrate)
        editor.putBoolean("FlashStatus", isFlash)
        editor.apply()

        Log.d("PinActivity", "onPause: Alarm: $isAlarmActive Flash: $isFlash Vibrate: $isVibrate")

        stopAlarm()
        startAlarmService()
    }

    override fun onResume() {
        super.onResume()
        val intent = intent
        isAlarmActive = intent.getBooleanExtra("Alarm", false)
        isVibrate = intent.getBooleanExtra("Vibrate", false)
        isFlash = intent.getBooleanExtra("Flash", false)
        stopAlarmService()
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
            putExtra("Alarm", isAlarmActive)
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
        outState.putBoolean("Alarm", isAlarmActive)
        outState.putBoolean("Vibrate", isVibrate)
        outState.putBoolean("Flash", isFlash)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isAlarmActive = savedInstanceState.getBoolean("Alarm", false)
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
