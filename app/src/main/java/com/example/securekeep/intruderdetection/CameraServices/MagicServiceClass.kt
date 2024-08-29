package com.example.securekeep.intruderdetection.CameraServices

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.securekeep.R
import com.example.securekeep.intruderdetection.IntruderHiddenCamera.CameraConfig
import com.example.securekeep.intruderdetection.IntruderHiddenCamera.CameraError
import com.example.securekeep.intruderdetection.IntruderHiddenCamera.HiddenCameraService
import com.example.securekeep.intruderdetection.IntruderHiddenCamera.HiddenCameraUtils
import com.example.securekeep.intruderdetection.IntruderHiddenCamera.config.CameraFacing
import com.example.securekeep.intruderdetection.IntruderHiddenCamera.config.CameraFocus
import com.example.securekeep.intruderdetection.IntruderHiddenCamera.config.CameraImageFormat
import com.example.securekeep.intruderdetection.IntruderHiddenCamera.config.CameraResolution
import java.io.File
import java.io.FileOutputStream
import java.util.Random

class MagicServiceClass : HiddenCameraService() {

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        Log.d("MagicService", "onCreate: Camera Service Started")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

            if (HiddenCameraUtils.canOverDrawOtherApps(this)) {
                val cameraConfig = CameraConfig()
                    .getBuilder(this)
                    .setCameraFacing(CameraFacing.FRONT_FACING_CAMERA)
                    .setCameraResolution(CameraResolution.MEDIUM_RESOLUTION)
                    .setImageFormat(CameraImageFormat.FORMAT_JPEG)
                    .setCameraFocus(CameraFocus.AUTO)
                    .build()

                startCamera(cameraConfig)

                Handler().postDelayed({
                    takePicture()
                }, 2000L)
            } else {
                HiddenCameraUtils.openDrawOverPermissionSetting(this)
            }
        } else {
            Toast.makeText(this, "Camera permission not available", Toast.LENGTH_SHORT).show()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "camera_channel"
            val channelName = "Intruder Selfie Service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationChannel = NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, "camera_channel")
            .setContentTitle("Intruder Selfie")
            .setContentText("Captured Intruder Selfie")
            .setSmallIcon(R.drawable.info) // Replace with your own icon
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val notification = notificationBuilder.build()

        startForeground(1, notification)
    }

    // to save in gallery or any front folders of local storage
    /*override fun onImageCapture(@NonNull imageFile: File) {
        Log.d("MagicService", "onImageCapture: Taking Picture")

        val path = imageFile.path
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        var decodeFile = BitmapFactory.decodeFile(path, BitmapFactory.Options())

        try {
            val exifInterface = ExifInterface(path)
            val orientation = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)?.toInt() ?: 1
            val matrix = Matrix()

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    Log.e("MagicService", "ExifInterface.ORIENTATION_ROTATE_90")
                    matrix.setRotate(90f)
                }
                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    Log.e("MagicService", "ExifInterface.ORIENTATION_ROTATE_180")
                    matrix.setRotate(180f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    Log.e("MagicService", "ExifInterface.ORIENTATION_ROTATE_270")
                    matrix.setRotate(270f)
                }
            }

            decodeFile = Bitmap.createBitmap(decodeFile, 0, 0, options.outWidth, options.outHeight, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val fileDir = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            Log.d("MagicService", "onImageCapture: Saving Picture in lower versions")
            getExternalFilesDir(Environment.DIRECTORY_PICTURES) // Use app-specific directory
        } else {
            Log.d("MagicService", "onImageCapture: Saving Picture in higher versions")
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Intruder Feature")
        }

        if (fileDir != null && (!fileDir.exists() && !fileDir.mkdirs())) {
            Log.i("MagicService", "Can't create directory to save the image")
            return
        }

        val fileName = "Image-" + Random().nextInt(10000) + ".jpg"
        val imageFileToSave = File(fileDir, fileName)

        Log.i("path", imageFileToSave.absolutePath)

        try {
            FileOutputStream(imageFileToSave).use { fos ->
                decodeFile.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                fos.flush()
            }
            Log.d("MagicService", "onImageCapture: Image saved successfully: ${imageFileToSave.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("MagicService", "onImageCapture: Error saving image")
        }

        stopSelf()
    }*/

    // to save in hidden folders of local storage
    override fun onImageCapture(@NonNull imageFile: File) {
        Log.d("MagicService", "onImageCapture: Taking Picture")

        val path = imageFile.path
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        var decodeFile = BitmapFactory.decodeFile(path, BitmapFactory.Options())

        try {
            val exifInterface = ExifInterface(path)
            val orientation = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)?.toInt() ?: 1
            val matrix = Matrix()

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            }

            decodeFile = Bitmap.createBitmap(decodeFile, 0, 0, options.outWidth, options.outHeight, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Use app-specific directory for saving images
        val fileDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) // App-specific pictures directory

        if (fileDir != null && (!fileDir.exists() && !fileDir.mkdirs())) {
            Log.i("MagicService", "Can't create directory to save the image")
            return
        }

        val fileName = "Image-" + Random().nextInt(10000) + ".jpg"
        val imageFileToSave = File(fileDir, fileName)

        Log.i("path", imageFileToSave.absolutePath)

        try {
            FileOutputStream(imageFileToSave).use { fos ->
                decodeFile.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                fos.flush()
            }
            Log.d("MagicService", "onImageCapture: Image saved successfully: ${imageFileToSave.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("MagicService", "onImageCapture: Error saving image")
        }

        stopSelf()
    }


    override fun onCameraError(@CameraError.CameraErrorCodes errorCode: Int) {
        when (errorCode) {
            CameraError.ERROR_CAMERA_OPEN_FAILED -> {
                Log.d("MagicService", "onCameraError: ERROR_CAMERA_OPEN_FAILED ${R.string.error_cannot_open}")
            }
            CameraError.ERROR_IMAGE_WRITE_FAILED -> {
                Log.d("MagicService", "onCameraError: ERROR_IMAGE_WRITE_FAILED ${R.string.error_cannot_write}")
            }
            CameraError.ERROR_CAMERA_PERMISSION_NOT_AVAILABLE -> {
                Log.d("MagicService", "onCameraError: ERROR_CAMERA_PERMISSION_NOT_AVAILABLE ${R.string.error_cannot_get_permission}")
            }
            CameraError.ERROR_DOES_NOT_HAVE_OVERDRAW_PERMISSION -> {
                HiddenCameraUtils.openDrawOverPermissionSetting(this)
            }
            CameraError.ERROR_DOES_NOT_HAVE_FRONT_CAMERA -> {
                Log.d("MagicService", "onCameraError: ERROR_DOES_NOT_HAVE_FRONT_CAMERA ${R.string.error_not_having_camera}")
            }
        }
        stopSelf()
    }
}

