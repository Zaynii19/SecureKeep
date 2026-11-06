package com.example.securekeep.chargingDetect

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.securekeep.R
import com.example.securekeep.alarmSetup.AlarmService
import com.example.securekeep.alarmSetup.EnterPinActivity

class ChargingDetectionService : Service() {

    private var isAlarmTriggered = false
    private lateinit var powerManager: PowerManager
    private lateinit var activityManager: ActivityManager
    private var isVibrate = false
    private var isFlash = false
    private var isAlarmActive = false
    private val chargingReceiver = object : BroadcastReceiver() {
        private var wasPlugged = false
        override fun onReceive(context: Context, intent: Intent) {
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

            if (isChargerConnected()) {
                if (!wasPlugged) {
                    wasPlugged = true
                    // Charger connected
                }
            } else if (plugged == 0 && wasPlugged) {
                wasPlugged = false
                // Charger disconnected
                triggerAlarm()
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        startForegroundService()

        powerManager = getSystemService(POWER_SERVICE) as PowerManager
        activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        registerReceiver(chargingReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        intent?.let {
            isVibrate = it.getBooleanExtra("Vibrate", false)
            isFlash = it.getBooleanExtra("Flash", false)
            isAlarmActive = it.getBooleanExtra("Alarm", false)
        }

        return START_STICKY
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel (required for Android O and above)
        val channelId = "charging_detection_channel"
        val channelName = "Charging Detection Service"
        val importance = NotificationManager.IMPORTANCE_LOW
        val notificationChannel = NotificationChannel(channelId, channelName, importance)
        notificationManager.createNotificationChannel(notificationChannel)

        // Create the notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Charging Detection Active")
            .setContentText("Monitoring Charging Connectivity...")
            .setSmallIcon(R.drawable.info)  
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val notification = notificationBuilder.build()

        // Start the service in the foreground
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    1,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            Log.e("ChargingService", "Failed to start foreground service", e)
            stopSelf()
        }
    }

    private fun triggerAlarm() {
        if (!isAlarmTriggered) {
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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(chargingReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun isChargerConnected(): Boolean {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB
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

    private fun startAlarmService() {
        val intent = Intent(this, AlarmService::class.java).apply {
            putExtra("Vibrate", isVibrate)
            putExtra("Flash", isFlash)
            putExtra("Alarm", isAlarmActive)
        }
        startService(intent)
    }
}