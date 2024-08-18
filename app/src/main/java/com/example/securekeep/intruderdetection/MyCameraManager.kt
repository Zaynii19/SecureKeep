package com.example.securekeep.intruderdetection

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.util.Log
import android.view.Surface
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

@Suppress("DEPRECATION")
class MyCameraManager(private val mContext: Context) {

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private lateinit var texture: SurfaceTexture

    private val cameraManager: CameraManager =
        mContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun takePhoto() {
        if (isFrontCameraAvailable) {
            Log.d("CameraManager", "Taking photo")
            initCamera()
        }
    }

    private val isFrontCameraAvailable: Boolean
        get() {
            return try {
                val cameraIdList = cameraManager.cameraIdList
                cameraIdList.any { id ->
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
                }
            } catch (e: CameraAccessException) {
                e.printStackTrace()
                false
            }
        }

    private fun initCamera() {
        try {
            Log.d("CameraManager", "Camera Initialization")
            val cameraIdList = cameraManager.cameraIdList
            val frontCameraId = cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            } ?: return

            // Check camera permission
            if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e("CameraManager", "Camera permission not granted.")
                return
            }

            cameraManager.openCamera(frontCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    setupCameraOutputs()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    closeCamera()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e("CameraManager", "Camera error: $error")
                    closeCamera()
                }
            }, null)
        } catch (e: CameraAccessException) {
            Log.e("CameraManager", "Exception during camera initialization", e)
        }
    }

    private fun setupCameraOutputs() {
        // Initialize ImageReader for JPEG format
        imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 2)

        texture = SurfaceTexture(0) // Use real texture ID
        texture.setDefaultBufferSize(1920, 1080)

        val surface = Surface(texture)

        try {
            val captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequestBuilder.addTarget(surface)

            cameraDevice!!.createCaptureSession(listOf(surface, imageReader!!.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                    try {
                        captureSession!!.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                        takePicture()
                    } catch (e: CameraAccessException) {
                        Log.e("CameraManager", "Error starting repeating request", e)
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e("CameraManager", "Configuration failed")
                }
            }, null)
        } catch (e: CameraAccessException) {
            Log.e("CameraManager", "Error setting up camera outputs", e)
        }
    }

    private fun takePicture() {
        captureSession?.apply {
            val captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequestBuilder.addTarget(imageReader!!.surface)

            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)

            try {
                stopRepeating()
                capture(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                        super.onCaptureCompleted(session, request, result)
                        imageReader?.acquireLatestImage()?.let { image ->
                            savePicture(image)
                            image.close() // Always close the image
                        }
                    }
                }, null)
            } catch (e: CameraAccessException) {
                Log.e("CameraManager", "Error during capture", e)
            }
        }
    }

    private fun savePicture(image: Image) {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        try {
            val file = File(mContext.getExternalFilesDir(null), "photo.jpg") // Change path as needed
            FileOutputStream(file).use { output ->
                output.write(bytes)
            }
            Log.d("CameraManager", "Photo saved: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("CameraManager", "Error saving photo", e)
        }
    }

    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var mManager: MyCameraManager? = null

        fun getInstance(context: Context): MyCameraManager {
            if (mManager == null) mManager = MyCameraManager(context)
            return mManager!!
        }
    }
}
