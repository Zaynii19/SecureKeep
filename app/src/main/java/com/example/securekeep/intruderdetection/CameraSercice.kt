package com.example.securekeep.intruderdetection

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.securekeep.R


class CameraService : Service() {
    private lateinit var myCameraManager: MyCameraManager

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        myCameraManager = MyCameraManager.getInstance(this)

        startForegroundService()

        Log.d("CameraService", "Service created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CameraService", "Service Started.")
        captureSelfie() // Moved to onStartCommand to ensure service is operational.
        return START_STICKY // Change if you want it to restart automatically
    }

    private fun captureSelfie() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            myCameraManager.takePhoto()
            Log.d("CameraService", "Capture selfie request sent.")
        } else {
            Log.e("CameraService", "Camera permission not granted.")
        }
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel (required for Android O and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "intruder_detection_channel"
            val channelName = "Intruder Detection Service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationChannel = NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // Create the notification
        val notificationBuilder = NotificationCompat.Builder(this, "intruder_detection_channel")
            .setContentTitle("Intruder Detection Active")
            .setContentText("Detecting Screen Lock")
            .setSmallIcon(R.drawable.info)  // Replace with your own icon
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val notification = notificationBuilder.build()

        // Start the service in the foreground
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CameraService", "CameraService destroyed.")
    }
}


