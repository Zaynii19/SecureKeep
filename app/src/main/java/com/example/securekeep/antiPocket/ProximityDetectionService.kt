package com.example.securekeep.antiPocket

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
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
import com.example.securekeep.alarmSetup.AlarmService
import com.example.securekeep.alarmSetup.EnterPinActivity

class ProximityDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var proximitySensor: Sensor
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var powerManager: PowerManager
    private lateinit var activityManager: ActivityManager
    private var isAlarmTriggered = false
    private var isVibrate = false
    private var isFlash = false
    private var isAlarmActive = false

    override fun onCreate() {
        super.onCreate()

        startForegroundService()

        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        powerManager = getSystemService(POWER_SERVICE) as PowerManager
        activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)!!
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_UI)

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
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] < proximitySensor.maximumRange && !isAlarmTriggered) {
                isAlarmTriggered = true
                val isScreenOn = powerManager.isInteractive
                val appInForeground = isAppInForeground()

                if (isScreenOn && appInForeground) {
                    // Start activity if the screen is on and app is in the foreground
                    startActivity(Intent(this, EnterPinActivity::class.java).apply {
                        putExtra("Vibrate", isVibrate)
                        putExtra("Flash", isFlash)
                        putExtra("Alarm", isAlarmActive)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } else {
                    // Start alarm service if the screen is off or app is closed
                    startAlarmService()
                }
                stopSelf()
            }
        }
    }

    private fun startAlarmService() {
        val intent = Intent(this, AlarmService::class.java).apply {
            putExtra("Vibrate", isVibrate)
            putExtra("Flash", isFlash)
            putExtra("Alarm", isAlarmActive)
        }
        startService(intent)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    private fun startForegroundService() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel (required for Android O and above)
        val channelId = "proximity_detection_channel"
        val channelName = "Proximity Detection Service"
        val importance = NotificationManager.IMPORTANCE_LOW
        val notificationChannel = NotificationChannel(channelId, channelName, importance)
        notificationManager.createNotificationChannel(notificationChannel)

        // Create the notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Proximity Detection Active")
            .setContentText("Detecting proximity...")
            .setSmallIcon(R.drawable.info)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val notification = notificationBuilder.build()

        // Start the service in the foreground
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    1,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            Log.e("ProximityService", "Failed to start foreground service", e)
            stopSelf()
        }
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