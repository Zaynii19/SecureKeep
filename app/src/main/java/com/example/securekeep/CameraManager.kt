package com.example.securekeep

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class CameraManager private constructor(private val context: Context) : Camera.PictureCallback,
    Camera.ErrorCallback, Camera.PreviewCallback {

    private var camera: Camera? = null
    private var isTakingPicture = false

    override fun onPictureTaken(data: ByteArray?, camera: Camera?) {
        Log.d("CameraManager", "Picture taken.")
        SavePhotoTask().execute(data)
        stopPreview()
    }

    private fun stopPreview() {
        try {
            camera?.stopPreview()
            camera?.release()
        } catch (e: Exception) {
            Log.e("CameraManager", "Error stopping camera preview: ${e.message}")
        }
    }

    override fun onError(error: Int, camera: Camera?) {
        Log.e("CameraManager", "Camera error: $error")
        releaseCamera()
    }

    private fun releaseCamera() {
        camera?.release()
        camera = null
    }

    fun capturePhoto() {
        if (isTakingPicture) return

        try {
            val cameraId = getFrontFacingCameraId()
            if (cameraId == -1) {
                Log.e("CameraManager", "No front-facing camera found")
                return
            }

            camera = Camera.open(cameraId)
            camera?.setPreviewTexture(SurfaceTexture(0))
            camera?.startPreview()
            camera?.takePicture(null, null, this)

            isTakingPicture = true
        } catch (e: Exception) {
            Log.e("CameraManager", "Error capturing photo: ${e.message}")
            releaseCamera()
        }
    }

    private fun getFrontFacingCameraId(): Int {
        val numberOfCameras = Camera.getNumberOfCameras()
        val info = Camera.CameraInfo()
        for (i in 0 until numberOfCameras) {
            Camera.getCameraInfo(i, info)
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return i
            }
        }
        return -1
    }

    private inner class SavePhotoTask : AsyncTask<ByteArray, Void, String>() {
        override fun doInBackground(vararg data: ByteArray): String {
            val pictureFile = getOutputMediaFile()
            if (pictureFile == null) {
                Log.e("CameraManager", "Error creating media file, check storage permissions")
                return "Error"
            }

            try {
                FileOutputStream(pictureFile).use { fos ->
                    fos.write(data[0])
                }
                Log.d("CameraManager", "Photo saved to: ${pictureFile.absolutePath}")
                return pictureFile.absolutePath
            } catch (e: IOException) {
                Log.e("CameraManager", "Error accessing file: ${e.message}")
                return "Error"
            }
        }

        override fun onPostExecute(result: String?) {
            if (result != "Error") {
                Log.d("CameraManager", "Photo saved successfully.")
            }
            isTakingPicture = false
        }
    }

    private fun getOutputMediaFile(): File? {
        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "IntruderSelfie"
        )

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e("CameraManager", "Failed to create directory")
                return null
            }
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return File("${mediaStorageDir.path}${File.separator}IMG_$timeStamp.jpg")
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        // Not used in this implementation
    }

    companion object {
        private var instance: CameraManager? = null

        fun getInstance(context: Context): CameraManager {
            if (instance == null) {
                instance = CameraManager(context.applicationContext)
            }
            return instance!!
        }
    }
}

