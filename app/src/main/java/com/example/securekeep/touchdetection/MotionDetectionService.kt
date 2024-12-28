package com.example.securekeep.touchdetection

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.securekeep.R
import com.example.securekeep.alarmsetup.AlarmService
import com.example.securekeep.alarmsetup.EnterPinActivity
import kotlin.math.sqrt

class MotionDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private var mAccel: Float = 0.0f
    private var mAccelCurrent: Float = 0.0f
    private var mAccelLast: Float = 0.0f
    private var motionSensitivity: Float = 5.0f
    private var isAlarmTriggered = false
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var powerManager: PowerManager
    private lateinit var activityManager: ActivityManager
    private var isVibrate = false
    private var isFlash = false
    private var isAlarmActive = false

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!

        // Retrieve the saved motion sensitivity
        motionSensitivity = sharedPreferences.getFloat("motionSensitivity", 15.0f)

        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)

        intent?.let {
            isVibrate = it.getBooleanExtra("Vibrate", false)
            isFlash = it.getBooleanExtra("Flash", false)
            isAlarmActive = it.getBooleanExtra("Alarm", false)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val values = event.values
            val x = values[0]
            val y = values[1]
            val z = values[2]
            mAccelLast = mAccelCurrent
            mAccelCurrent = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta = mAccelCurrent - mAccelLast
            mAccel = mAccel * 0.9f + delta

            Log.d("MotionDetectionService", "mAccelCurrent: $mAccelCurrent, motionSensitivity: $motionSensitivity")

            if (mAccelCurrent > motionSensitivity && !isAlarmTriggered) {
                isAlarmTriggered = true

                val isScreenOn = powerManager.isInteractive
                val appInForeground = isAppInForeground()

                if (isScreenOn && appInForeground) {
                    // Start activity if the screen is on and app is in the foreground
                    val intent = Intent(this, EnterPinActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("Vibrate", isVibrate)
                        putExtra("Flash", isFlash)
                        putExtra("Alarm", isAlarmActive)
                    }
                    startActivity(intent)
                } else {
                    // Start alarm service if the screen is off or app is closed
                    startAlarmService()
                }
                stopSelf()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do nothing
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel (required for Android O and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "motion_detection_channel"
            val channelName = "Motion Detection Service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationChannel = NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // Create the notification
        val notificationBuilder = NotificationCompat.Builder(this, "motion_detection_channel")
            .setContentTitle("Motion Detection Active")
            .setContentText("Detecting motion...")
            .setSmallIcon(R.drawable.info)  // Replace with your own icon
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val notification = notificationBuilder.build()

        // Start the service in the foreground
        startForeground(1, notification)
    }

    private fun startAlarmService() {
        val intent = Intent(this, AlarmService::class.java).apply {
            putExtra("Vibrate", isVibrate)
            putExtra("Flash", isFlash)
            putExtra("Alarm", isAlarmActive)
        }
        Log.d("MotionService", "onSensorChanged: Alarm: $isAlarmActive Flash: $isFlash Vibrate: $isVibrate")
        startService(intent)
    }

    private fun isAppInForeground(): Boolean {
        val appProcesses = activityManager.runningAppProcesses
        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return appProcess.processName == packageName
            }
        }
        return false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
