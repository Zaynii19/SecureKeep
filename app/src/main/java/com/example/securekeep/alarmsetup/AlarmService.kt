
package com.example.securekeep.alarmsetup

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.securekeep.R
import java.io.IOException

class AlarmService : Service() {

    private lateinit var wakeLock: PowerManager.WakeLock
    private var isVibrate = false
    private var isFlash = false
    private var isAlarmActive = false
    private var fromPin = false
    private var currentSoundLevel = 0
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private lateinit var handler: Handler
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var pinAlarmPreferences: SharedPreferences
    private lateinit var powerManager: PowerManager
    private lateinit var audioManager: AudioManager
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

    override fun onCreate() {
        super.onCreate()
        pinAlarmPreferences = getSharedPreferences("PinAlarmPreferences", MODE_PRIVATE)
        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        currentSoundLevel = sharedPreferences.getInt("SOUND_LEVEL", 70)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList.firstOrNull() ?: return // Check for null
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        handler = Handler()
        handler.post(volumeCheckRunnable)  // Start volume check task

        val isAlarmServiceActive = true
        val editor = sharedPreferences.edit()
        editor.putBoolean("AlarmServiceStatus", isAlarmServiceActive)
        editor.apply()

        // Retrieve the saved tone
        val toneId = sharedPreferences.getInt("alarm_tone", R.raw.alarm_tune_1)
        val toneUriString = sharedPreferences.getString("alarm_tone_uri", null)
        val systemToneUri = toneUriString?.let { Uri.parse(it) } // Convert to Uri if exists

        // Create MediaPlayer based on tone URI or ID
        mediaPlayer = if (systemToneUri != null) {
            try {
                Log.d("AlarmService", "systemToneUri: $systemToneUri")
                MediaPlayer().apply {
                    setDataSource(this@AlarmService, systemToneUri)
                    prepare()
                    isLooping = true
                }
            } catch (e: IOException) {
                Log.e("AlarmService", "Error setting data source", e)
                null // Handle the null case. You might want to set a default sound here.
            }
        } else {
            MediaPlayer.create(this, toneId).apply {
                isLooping = true
            }
        }

        setSystemSoundLevel(currentSoundLevel)
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent.let {
            fromPin = it!!.getBooleanExtra("FromPin", false)  // Retrieve fromPin
        }
        if (fromPin){
            isAlarmActive = pinAlarmPreferences.getBoolean("AlarmStatus", false)
            isFlash = pinAlarmPreferences.getBoolean("FlashStatus", false)
            isVibrate = pinAlarmPreferences.getBoolean("VibrateStatus", false)

            Log.d("AlarmService", "onStartCommand: FromPin: $fromPin Alarm: $isAlarmActive Flash: $isFlash Vibrate: $isVibrate")
        }else intent?.let {
            isVibrate = it.getBooleanExtra("Vibrate", false)
            isFlash = it.getBooleanExtra("Flash", false)
            isAlarmActive = it.getBooleanExtra("Alarm", false)

            Log.d("AlarmService", "onStartCommand2: Alarm: $isAlarmActive Flash: $isFlash Vibrate: $isVibrate")
        }

        startForegroundService()
        triggerAlarm()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()

        // Update SharedPreferences to set AlarmServiceStatus to false
        val editor = sharedPreferences.edit()
        editor.putBoolean("AlarmServiceStatus", false)
        editor.apply()
    }


    private fun startForegroundService() {
        // Intent to launch EnterPinActivity when the notification is clicked
        val intent = Intent(this, EnterPinActivity::class.java).apply {
            putExtra("Vibrate", isVibrate)
            putExtra("Flash", isFlash)
            putExtra("Alarm", isAlarmActive)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // Create notification
        val notification = NotificationCompat.Builder(this, ApplicationClass.CHANNEL_ID)
            .setContentIntent(pendingIntent) // Perform action when clicked
            .setContentTitle("Alarm Activated")
            .setContentText("Click to Enter Pin")
            .setSmallIcon(R.drawable.info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true) // Automatically remove notification when clicked
            .build()

        startForeground(1, notification)
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
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
                prepare() // Prepare the MediaPlayer for future use
            }
            isLooping = false // Set looping to false
        }

        // Cancel vibration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.cancel()
        } else {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.cancel()
        }

        // Stop flashlight
        if (isFlash) {
            toggleFlashlight() // Turn off the flashlight
        }

        handler.removeCallbacks(flashRunnable)
        // Stop volume check when service is destroyed
        handler.removeCallbacks(volumeCheckRunnable)

        releaseWakeLock()
    }

    private fun triggerVibration() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
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

    private fun acquireWakeLock() {
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SecureKeep::WakeLock")
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun setSystemSoundLevel(level: Int) {
        val clampedLevel = level.coerceIn(0, 100)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = (clampedLevel * maxVolume) / 100
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0) // Remove FLAG_SHOW_UI
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
}
