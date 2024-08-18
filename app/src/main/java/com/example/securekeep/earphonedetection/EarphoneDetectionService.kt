package com.example.securekeep.earphonedetection

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.securekeep.R
import com.example.securekeep.alarmsetup.AlarmService
import com.example.securekeep.alarmsetup.EnterPinActivity

class EarphoneDetectionService : Service() {

    private var isAlarmTriggered = false
    private lateinit var activityManager: ActivityManager
    private lateinit var powerManager: PowerManager
    private var isVibrate = false
    private var isFlash = false
    private var isAlarmActive = false

    private val audioReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
            if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                triggerAlarm()
                stopSelf()
            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
                triggerAlarm()
                stopSelf()
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
        registerReceivers()

        intent?.let {
            isVibrate = it.getBooleanExtra("Vibrate", false)
            isFlash = it.getBooleanExtra("Flash", false)
            isAlarmActive = it.getBooleanExtra("Alarm", false)
        }

        return START_STICKY
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "earphone_detection_channel"
            val channelName = "Earphone Detection Service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationChannel = NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val notificationBuilder = NotificationCompat.Builder(this, "earphone_detection_channel")
            .setContentTitle("Earphone Detection Active")
            .setContentText("Monitoring Earphone Connectivity...")
            .setSmallIcon(R.drawable.info) // Replace with your own icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
        startForeground(1, notificationBuilder.build())
    }

    private fun registerReceivers() {
        if (isWiredHeadsetConnected()) {
            val filter = IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            registerReceiver(audioReceiver, filter)
        } else if (isBluetoothHeadsetConnected()) {
            val bluetoothFilter = IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            registerReceiver(bluetoothReceiver, bluetoothFilter)
        }
    }

    private fun unregisterReceivers() {
        if (isWiredHeadsetConnected())
            unregisterReceiver(audioReceiver)
        else if(isBluetoothHeadsetConnected())
            unregisterReceiver(bluetoothReceiver)
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
        intent.putExtra("Vibrate", isVibrate)
        intent.putExtra("Flash", isFlash)
        intent.putExtra("Alarm", isAlarmActive)
        startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceivers()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
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
}
