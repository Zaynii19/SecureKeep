package com.example.securekeep.intruderdetection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import android.Manifest
import com.example.securekeep.R


class CameraService : Service() {
    private lateinit var myCameraManager: MyCameraManager

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        myCameraManager = MyCameraManager.getInstance(this)
        createNotificationChannel()
        startForeground(1, createNotification())
        captureSelfie()
        Log.d("CameraService", "Service created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CameraService", "Service Started.")
        return START_NOT_STICKY
    }

    private fun captureSelfie() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            myCameraManager.takePhoto()
            Log.d("CameraService", "Capture selfie request sent.")
        } else {
            Log.e("CameraService", "Camera permission not granted.")
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Camera Service")
            .setContentText("Capturing selfie...")
            .setSmallIcon(R.drawable.camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val name = "Camera Service Channel"
        val descriptionText = "Channel for Camera Service"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CameraService", "CameraService destroyed.")
    }

    companion object {
        private const val CHANNEL_ID = "CameraServiceChannel"
    }
}


