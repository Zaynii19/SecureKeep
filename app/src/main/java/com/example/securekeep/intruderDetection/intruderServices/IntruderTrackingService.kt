package com.example.securekeep.intruderDetection.intruderServices

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.securekeep.R

class IntruderTrackingService : Service() {
    private var currentFailedAttempts = 0
    private var attemptThreshold = 0
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var keyguardManager: KeyguardManager
    private var handler = Handler(Looper.getMainLooper())
    private var isMagicServiceRunning = false
    private val lock = Any() // Lock object for synchronization


    private val passwordAttemptReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "PASSWORD_ATTEMPT_FAILED") {
                onWrongPinAttempt()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        startForegroundService()

        handler = Handler(Looper.getMainLooper())
        sharedPreferences = getSharedPreferences("IntruderPrefs", MODE_PRIVATE)
        attemptThreshold = sharedPreferences.getInt("AttemptThreshold", 2)

        keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager

        // Register the BroadcastReceiver
        ContextCompat.registerReceiver(
            this,
            passwordAttemptReceiver,
            IntentFilter("PASSWORD_ATTEMPT_FAILED"),
            ContextCompat.RECEIVER_EXPORTED
        )

        // Start checking for unlocks
        checkUnlockStatus()
    }

    private fun checkUnlockStatus() {
        handler.postDelayed({
            if (!keyguardManager.isKeyguardLocked) {
                onCorrectPinAttempt()  // Reset if device is unlocked
            }
            checkUnlockStatus()  // Re-check periodically
        }, 2000)  // Check every 2 seconds (you can adjust this interval)
    }

    private fun onWrongPinAttempt() {
        synchronized(lock) {
            if (isMagicServiceRunning) return // Prevent multiple invocations
            currentFailedAttempts++
            Log.d("IntruderTrackingService", "Wrong PIN attempt detected. Failed attempts: $currentFailedAttempts")
            if (currentFailedAttempts >= attemptThreshold) {
                startMagicService()
                isMagicServiceRunning = true // Update this only after starting the service
            }
        }
    }

    private fun onCorrectPinAttempt() {
        synchronized(lock) {
            currentFailedAttempts = 0
            isMagicServiceRunning = false // Reset the flag if device unlocks
            //Log.d("IntruderTrackingService", "Device unlocked. Resetting failed attempts and flags.")
        }
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "intruder_detection_channel"
        val channelName = "Intruder Detection Service"
        val importance = NotificationManager.IMPORTANCE_LOW
        val notificationChannel = NotificationChannel(channelId, channelName, importance)
        notificationManager.createNotificationChannel(notificationChannel)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Intruder Detection Active")
            .setContentText("Monitoring Passwords Attempts...")
            .setSmallIcon(R.drawable.info) 
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val notification = notificationBuilder.build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    1,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                )
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            Log.e("IntruderTrackingService", "Failed to start foreground service", e)
            stopSelf()
        }
    }

    private fun startMagicService() {
        Intent(this, MagicServiceClass::class.java).also {
            Log.d("IntruderTrackingService", "Attempt threshold met, starting the MagicService.")
            ContextCompat.startForegroundService(this, it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(passwordAttemptReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // This service does not bind to any activities
    }
}