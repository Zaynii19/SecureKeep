package com.example.securekeep.wifidetection

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.securekeep.R
import com.example.securekeep.alarmsetup.EnterPinActivity

class WifiDetectionService : Service() {

    private lateinit var sharedPreferences: SharedPreferences
    private var isAlarmTriggered = false
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
        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
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
            startActivity(Intent(this, EnterPinActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
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
}
