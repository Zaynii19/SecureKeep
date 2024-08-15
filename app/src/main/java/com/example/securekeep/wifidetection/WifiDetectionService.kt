package com.example.securekeep.wifidetection

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.securekeep.R
import com.example.securekeep.alarmsetup.AlarmService
import com.example.securekeep.alarmsetup.EnterPinActivity

class WifiDetectionService : Service() {

    private var isAlarmTriggered = false
    private lateinit var powerManager: PowerManager
    private lateinit var activityManager: ActivityManager
    private var lastWifiState: Boolean? = null
    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val currentWifiState = isWifiConnected()
            if (lastWifiState == null) {
                lastWifiState = currentWifiState
                return
            }
            if (lastWifiState != currentWifiState) {
                lastWifiState = currentWifiState
                triggerAlarm()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(wifiReceiver, filter)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wifiReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun isWifiConnected(): Boolean {
        val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connManager.activeNetworkInfo
        return networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
    }

    private fun triggerAlarm() {
        if (!isAlarmTriggered) {
            isAlarmTriggered = true

            val isScreenOn = powerManager.isInteractive
            val appInForeground = isAppInForeground()

            if (isScreenOn && appInForeground) {
                // Start activity if the screen is on and app is in the foreground
                startActivity(Intent(this, EnterPinActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } else {
                // Start alarm service if the screen is off or app is closed
                startAlarmService()
            }
            stopSelf()
        }
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "wifi_detection_channel"
            val channelName = "Wi-Fi Detection Service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationChannel = NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, "wifi_detection_channel")
            .setContentTitle("Wi-Fi Detection Active")
            .setContentText("Monitoring Wi-Fi connectivity...")
            .setSmallIcon(R.drawable.info) // Replace with your own icon
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val notification = notificationBuilder.build()

        startForeground(1, notification)
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
        val intent = Intent(this, AlarmService::class.java)
        startService(intent)
    }
}
