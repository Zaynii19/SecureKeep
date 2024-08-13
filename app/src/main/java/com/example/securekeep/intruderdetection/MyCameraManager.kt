package com.example.securekeep.intruderdetection

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.util.Log
import android.view.Surface
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

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
            if (ContextCompat.checkSelfPermission(
                    mContext,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request permission if not granted
                // You should handle this request in the activity where the method is called
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
            if (e.reason == CameraAccessException.CAMERA_IN_USE) {
                Log.e("CameraManager", "Camera is currently in use by another application")
                // Handle the camera in use error, maybe retry or inform the user
            } else {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("Recycle")
    private fun setupCameraOutputs() {
        // Initialize the texture property
        texture = SurfaceTexture(123)
        texture.setDefaultBufferSize(1920, 1080)

        val surface = Surface(texture)

        try {
            val captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequestBuilder.addTarget(surface)

            cameraDevice!!.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                    captureSession!!.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                    takePicture()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e("CameraManager", "Configuration failed")
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun takePicture() {
        val captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder.addTarget(Surface(texture))

        try {
            captureSession?.apply {
                stopRepeating()
                abortCaptures()
                capture(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                        super.onCaptureCompleted(session, request, result)
                        savePicture()
                    }
                }, null)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun savePicture() {
        // Add code to save picture here
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
