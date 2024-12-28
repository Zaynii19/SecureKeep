package com.example.securekeep.intruderdetection.IntruderServices

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.securekeep.R
import com.example.securekeep.intruderdetection.IntruderSelfieActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Locale
import java.util.Properties
import java.util.Random
import java.util.concurrent.Executors
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class MagicServiceClass : Service() {
    private var isEmail = false
    private lateinit var sharedPreferences: SharedPreferences
    private var userEmail = ""

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var backgroundHandler: Handler
    private lateinit var imageReader: ImageReader

    override fun onCreate() {
        super.onCreate()

        Log.d("MagicService", "onCreate: Camera Service Started")

        sharedPreferences = getSharedPreferences("IntruderPrefs", MODE_PRIVATE)
        isEmail = sharedPreferences.getBoolean("EmailStatus", false)
        userEmail = sharedPreferences.getString("UserEmail", "") ?: ""

        Log.d("MagicService", "onCreate: EmailStatus: $isEmail")
        Log.d("MagicService", "onCreate: UserEmail: $userEmail")

        // Start the background thread for camera handling
        val handlerThread = HandlerThread("CameraBackground")
        handlerThread.start()
        backgroundHandler = Handler(handlerThread.looper)

        // Get the CameraManager and CameraId
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = getFrontFacingCameraId(cameraManager)

        Log.d("MagicService", "Service Created and Camera ID obtained")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        takePicture()
        return START_STICKY
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

    private fun takePicture() {
        try {
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e("MagicService", "Camera access exception: ${e.message}")
        } catch (e: SecurityException) {
            Log.e("MagicService", "Permissions not granted: ${e.message}")
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        @RequiresApi(Build.VERSION_CODES.P)
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            startCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            Log.e("MagicService", "Camera device error: $error")
        }
    }

    /*private fun startCaptureSession() {
        try {
            imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1)
            imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler)

            val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequestBuilder.addTarget(imageReader.surface)

            // Set the JPEG orientation
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 270)

            cameraDevice.createCaptureSession(
                listOf(imageReader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        // Capture the picture
                        captureSession.capture(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                                Log.d("MagicService", "Picture taken and saved")
                                stopSelf() // Stop service after capturing the image
                            }
                        }, backgroundHandler)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("MagicService", "Capture session configuration failed")
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.e("MagicService", "Error starting capture session: ${e.message}")
        }
    }*/

    @RequiresApi(Build.VERSION_CODES.P)
    private fun startCaptureSession() {
        try {
            imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1)
            imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler)

            val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequestBuilder.addTarget(imageReader.surface)

            // Set the JPEG orientation
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 270)

            // Create output configurations for the session
            val outputConfig = OutputConfiguration(imageReader.surface)
            val outputConfigs = listOf(outputConfig)

            // Use an executor for callbacks
            val executor = Executors.newSingleThreadExecutor()

            // Create session configuration
            val sessionConfig = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputConfigs,
                executor,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        try {
                            // Capture the picture
                            captureSession.capture(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                                    Log.d("MagicService", "Picture taken and saved")
                                    stopSelf() // Stop service after capturing the image
                                }
                            }, backgroundHandler)
                        } catch (e: CameraAccessException) {
                            Log.e("MagicService", "Capture failed: ${e.message}")
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("MagicService", "Capture session configuration failed")
                    }
                }
            )

            // Start the session
            cameraDevice.createCaptureSession(sessionConfig)
        } catch (e: Exception) {
            Log.e("MagicService", "Error starting capture session: ${e.message}")
        }
    }

    private val imageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage()
        image?.let {
            val buffer: ByteBuffer = it.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            saveCapturedImage(bytes) // Save the image without additional rotations
            it.close()
        }
    }


    private fun saveCapturedImage(imageBytes: ByteArray) {
        Log.d("MagicService", "saveCapturedImage: Saving Picture")

        val fileDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return

        // Ensure the directory exists
        if (!fileDir.exists()) {
            if (!fileDir.mkdirs()) {
                Log.e("MagicService", "Failed to create directory: ${fileDir.absolutePath}")
                return
            }
        }

        val fileName = "Image-" + Random().nextInt(10000) + ".jpg"
        val imageFileToSave = File(fileDir, fileName)

        try {
            // Write the image bytes directly to the file
            FileOutputStream(imageFileToSave).use { fos ->
                fos.write(imageBytes)
            }

            Log.d("MagicService", "Image saved successfully: ${imageFileToSave.absolutePath}")

            // Optionally, send the email if email sending is enabled
            if (isEmail) {
                sendEmailWithImageWithCoroutine(imageFileToSave)
            }

        } catch (e: IOException) {
            Log.e("MagicService", "saveCapturedImage: Error saving image: ${e.localizedMessage}")
        }
    }


    private fun getFrontFacingCameraId(cameraManager: CameraManager): String {
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val cameraFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (cameraFacing != null && cameraFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                return cameraId
            }
        }
        throw RuntimeException("Front facing camera not found")
    }

    private fun sendEmailWithImageWithCoroutine(imageFile: File) {
        CoroutineScope(Dispatchers.IO).launch {
            sendEmailWithImage(imageFile)
        }
    }

    // Using JavaMail
    private fun sendEmailWithImage(imageFile: File) {
        try {
            // Log the size of the image file
            Log.d("MagicService", "Image file size: ${imageFile.length()} bytes")

            val props = Properties().apply {
                put("mail.smtp.host", "smtp.gmail.com") // Use appropriate SMTP host
                put("mail.smtp.port", "587")
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    val senderEmail = "zaynii1911491@gmail.com" // Sender email
                    val senderPassword = "eeyepbpiadbraobu" // Use your app password for Gmail
                    return PasswordAuthentication(senderEmail, senderPassword)
                }
            })

            // Get the current timestamp
            val currentTime = System.currentTimeMillis()
            val timestamp = java.text.SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()).format(currentTime)

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress("zaynii1911491@gmail.com")) // Email from
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(userEmail)) // Email to
                subject = "Intruder Alert"

                // Create a multipart message
                val multipart = MimeMultipart()

                // Create the body part for the text
                val textBodyPart = MimeBodyPart().apply {
                    setText("An intruder was detected trying to access your device at $timestamp.")
                }

                // Create the body part for the attachment
                val attachmentBodyPart = MimeBodyPart().apply {
                    attachFile(imageFile)
                }

                // Add both parts to the multipart
                multipart.addBodyPart(textBodyPart)
                multipart.addBodyPart(attachmentBodyPart)

                // Set the content of the message to the multipart
                setContent(multipart)
            }

            Transport.send(message)
            Log.d("MagicService", "Email sent successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MagicService", "Error sending email: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraDevice.close()
    }

    // Using MailJet API
    /*private fun sendEmailWithImage(imageFile: File) {
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

    // to save in gallery or any front folders of local storage
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
}

