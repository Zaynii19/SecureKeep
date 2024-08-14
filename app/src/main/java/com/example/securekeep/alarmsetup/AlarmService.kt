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
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.securekeep.R

class AlarmService : Service() {

    private lateinit var wakeLock: PowerManager.WakeLock
    private var isVibrate = false
    private var isFlash = false
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private lateinit var handler: Handler
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var powerManager: PowerManager
    private lateinit var audioManager: AudioManager
    private val flashRunnable = object : Runnable {
        override fun run() {
            toggleFlashlight()
            handler.postDelayed(this, 1000) // Flash every second
        }
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        isFlash = sharedPreferences.getBoolean("FlashStatus", false)
        isVibrate = sharedPreferences.getBoolean("VibrateStatus", false)
        val currentSoundLevel = sharedPreferences.getInt("SOUND_LEVEL", 70)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList.firstOrNull() ?: return // Check for null
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        handler = Handler()

        val isAlarmServiceActive = true
        val editor = sharedPreferences.edit()
        editor.putBoolean("AlarmServiceStatus", isAlarmServiceActive)
        editor.apply()

        mediaPlayer = MediaPlayer.create(this, sharedPreferences.getInt("alarm_tone",
            R.raw.alarm_tune_1
        ))
        setSystemSoundLevel(currentSoundLevel)
        startForegroundService()
        acquireWakeLock()
        startAlarm()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // No additional handling needed for notification clicks in this method
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        releaseWakeLock()
    }

    private fun startForegroundService() {
        // Intent to launch EnterPinActivity when the notification is clicked
        val intent = Intent(this, EnterPinActivity::class.java).apply {
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

    private fun startAlarm() {
        mediaPlayer?.apply {
            if (!isPlaying) {
                start()
            } else {
                seekTo(0)
            }
        }

        if (isVibrate) {
            triggerVibration()
        }

        if (isFlash) {
            flashRunnable.run() // Start toggling the flashlight
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
                prepare() // Prepare the MediaPlayer for future use
            }
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
}
