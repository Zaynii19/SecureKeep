package com.example.securekeep

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log

class CameraService : Service() {

    private lateinit var cameraManager: CameraManager

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        cameraManager = CameraManager.getInstance(this)
        captureSelfie()
        return START_NOT_STICKY
    }

    private fun captureSelfie() {
        cameraManager.capturePhoto()
        Log.d("CameraService", "Capture selfie request sent.")
        // Add a Toast message if needed
        // Toast.makeText(this, "Selfie capture initiated", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CameraService", "CameraService destroyed.")
    }
}
