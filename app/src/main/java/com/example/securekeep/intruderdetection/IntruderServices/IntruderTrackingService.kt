package com.example.securekeep.intruderdetection.IntruderServices

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.securekeep.R

class IntruderTrackingService : Service() {
    private var currentFailedAttempts = 0
    private var attemptThreshold = 0
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var keyguardManager: KeyguardManager
    private val handler = Handler()
    private var isMagicServiceRunning = false
    private val lock = Any() // Lock object for synchronization


    private val passwordAttemptReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "PASSWORD_ATTEMPT_FAILED") {
                onWrongPinAttempt()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("IntruderPrefs", MODE_PRIVATE)
        attemptThreshold = sharedPreferences.getInt("AttemptThreshold", 2)

        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        // Register the BroadcastReceiver
        val filter = IntentFilter("PASSWORD_ATTEMPT_FAILED")
        registerReceiver(passwordAttemptReceiver, filter, RECEIVER_EXPORTED)

        // Start checking for unlocks
        checkUnlockStatus()
        startForegroundService()
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
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "intruder_detection_channel"
            val channelName = "Intruder Detection Service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationChannel = NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, "intruder_detection_channel")
            .setContentTitle("Intruder Detection Active")
            .setContentText("Monitoring Passwords Attempts...")
            .setSmallIcon(R.drawable.info) // Replace with your own icon
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val notification = notificationBuilder.build()

        startForeground(1, notification)
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
