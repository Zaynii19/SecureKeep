package com.example.securekeep.intruderdetection.IntruderServices

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
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
import com.example.securekeep.intruderdetection.IntruderHiddenCamera.config.CameraRotation
import com.example.securekeep.intruderdetection.IntruderSelfieActivity
import com.mailjet.client.MailjetRequest
import com.mailjet.client.resource.Emailv31
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.Random


class MagicServiceClass : HiddenCameraService() {
    private var isEmail = false
    private lateinit var sharedPreferences: SharedPreferences
    private var userEmail = ""
    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        Log.d("MagicService", "onCreate: Camera Service Started")

        sharedPreferences = getSharedPreferences("IntruderPrefs", MODE_PRIVATE)
        isEmail = sharedPreferences.getBoolean("EmailStatus", false)
        userEmail = sharedPreferences.getString("UserEmail", "") ?: ""

        Log.d("MagicService", "onCreate: UserEmail: $userEmail")

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
                    .setImageRotation(CameraRotation.ROTATION_270) // Set to portrait
                    .build()

                startCamera(cameraConfig)

                Handler(Looper.getMainLooper()).postDelayed({
                    takePicture()
                },100)

            } else {
                HiddenCameraUtils.openDrawOverPermissionSetting(this)
            }
        } else {
            Toast.makeText(this, "Camera permission not available", Toast.LENGTH_SHORT).show()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        // Intent to launch EnterPinActivity when the notification is clicked
        val intent = Intent(this, IntruderSelfieActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "camera_channel"
            val channelName = "Intruder Selfie Service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationChannel = NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, "camera_channel")
            .setContentIntent(pendingIntent) // Perform action when clicked
            .setContentTitle("Intruder Detected")
            .setContentText("Someone has tried to unlock your phone, Click to view intruder photo")
            .setSmallIcon(R.drawable.info) // Replace with your own icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true) // Automatically remove notification when clicked

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
    override fun onImageCapture(imageFile: File) {
        Log.d("MagicService", "onImageCapture: Taking Picture")

        val path = imageFile.path
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
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

        val fileDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return

        if (!fileDir.exists() && !fileDir.mkdirs()) {
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

            if (isEmail){
                // Send the image via email
                sendEmailWithImageWithCoroutine(imageFile)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("MagicService", "onImageCapture: Error saving image")
        }

        stopSelf()
    }

    private fun sendEmailWithImageWithCoroutine(imageFile: File) {
        CoroutineScope(Dispatchers.IO).launch {
            sendEmailWithImage(imageFile)
        }
    }

    private fun sendEmailWithImage(imageFile: File) {
        try {
            val base64Image = encodeImageToBase64(imageFile)
            if (base64Image.isEmpty()) {
                Log.e("MagicService", "Base64 image is empty, skipping email send.")
                return
            }

            // Log the size of the image file
            Log.d("MagicService", "Image file size: ${imageFile.length()} bytes")

            // Get the current timestamp
            val currentTime = System.currentTimeMillis()
            val timestamp = java.text.SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()).format(currentTime)

            val email = JSONArray().put(
                JSONObject().apply {
                    put(Emailv31.Message.FROM, JSONObject().apply {
                        put("Email", "zaynii1911491@gmail.com")
                        put("Name", "SecureKeep")
                    })
                    put(
                        Emailv31.Message.TO, JSONArray().put(
                            JSONObject().apply {
                                put("Email", userEmail)
                            }
                        )
                    )
                    put(Emailv31.Message.SUBJECT, "Intruder Alert!")
                    put(Emailv31.Message.TEXTPART, "An intruder was detected at $timestamp.")
                    put(
                        Emailv31.Message.ATTACHMENTS, JSONArray().put(
                            JSONObject().apply {
                                put("ContentType", "image/png")
                                put("Filename", imageFile.name)
                                put("Base64Content", base64Image)
                            }
                        )
                    )
                }
            )

            // Build email request
            val request = MailjetRequest(Emailv31.resource).apply {
                property(Emailv31.MESSAGES, email)
            }

            // Send email using the MailjetService
            val response = MailjetService.sendEmail(request)
            response?.let {
                Log.d("MagicService", "Email sent response: ${it.data}")
            } ?: Log.e("MagicService", "Email sending failed: Response is null")

        } catch (e: Exception) {
            Log.e("MagicService", "Exception occurred while sending email: ${e.localizedMessage}")
        }
    }


    private fun encodeImageToBase64(file: File): String {
        // Decode the image file
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: run {
            Log.e("MailjetService", "Failed to decode the image file: ${file.absolutePath}")
            return ""
        }

        // Log the dimensions of the bitmap
        Log.d("MagicService", "Decoded image size: ${bitmap.width}x${bitmap.height} pixels")

        return bitmapToBase64(bitmap)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        // Log base64 length
        Log.d("MagicService", "Base64 size: ${byteArray.size} bytes")

        return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
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

