/*
 * Copyright 2017 Keval Patel.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.example.securekeep.intruderdetection.IntruderHiddenCamera

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Matrix
import android.hardware.Camera
import android.os.Build
import android.provider.Settings
import androidx.annotation.WorkerThread
import com.example.securekeep.intruderdetection.IntruderHiddenCamera.config.CameraImageFormat
import com.example.securekeep.intruderdetection.IntruderHiddenCamera.config.CameraRotation
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Created by Keval on 11-Nov-16.
 * This class holds common camera utils.
 *
 * @author [&#39;https://github.com/kevalpatel2106&#39;]['https://github.com/kevalpatel2106']
 */
object HiddenCameraUtils {
    /**
     * Check if the application has "Draw over other app" permission? This permission is available to all
     * the application below Android M (<API 23). But for the API 23 and above user has to enable it mannually if the permission is not available by opening Settings -> Apps -> Gear icon on top-right corner ->
     * Draw Over other apps.
     *
     * @return true if the permission is available.
     * @see 'http://www.androidpolice.com/2015/09/07/android-m-begins-locking-down-floating-apps-requires-users-to-grant-special-permission-to-draw-on-other-apps/'
    </API> */
    @SuppressLint("NewApi")
    fun canOverDrawOtherApps(context: Context?): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    }

    /**
     * This will open settings screen to allow the "Draw over other apps" permission to the application.
     *
     * @param context instance of caller.
     */
    fun openDrawOverPermissionSetting(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Get the cache directory.
     *
     * @param context instance of the caller
     * @return cache directory file.
     */
    fun getCacheDir(context: Context): File {
        return if (context.externalCacheDir == null) context.cacheDir else context.externalCacheDir!!
    }

    /**
     * Check if the device has front camera or not?
     *
     * @param context context
     * @return true if the device has front camera.
     */
    @Suppress("deprecation")
    fun isFrontCameraAvailable(context: Context): Boolean {
        val numCameras = Camera.getNumberOfCameras()
        return numCameras > 0 && context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
    }


    /**
     * Rotate the bitmap by 90 degree.
     *
     * @param bitmap original bitmap
     * @return rotated bitmap
     */
    @WorkerThread
    fun rotateBitmap(bitmap: Bitmap, @CameraRotation.SupportedRotation rotation: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Save image to the file.
     *
     * @param bitmap     bitmap to store.
     * @param fileToSave file where bitmap should stored
     */
    fun saveImageFromFile(
        bitmap: Bitmap,
        fileToSave: File,
        @CameraImageFormat.SupportedImageFormat imageFormat: Int
    ): Boolean {
        var out: FileOutputStream? = null
        var isSuccess: Boolean

        //Decide the image format
        val compressFormat = when (imageFormat) {
            CameraImageFormat.FORMAT_JPEG -> CompressFormat.JPEG
            CameraImageFormat.FORMAT_WEBP -> CompressFormat.WEBP
            CameraImageFormat.FORMAT_PNG -> CompressFormat.PNG
            else -> CompressFormat.PNG
        }
        try {
            if (!fileToSave.exists())
                fileToSave.createNewFile()

            out = FileOutputStream(fileToSave)
            bitmap.compress(compressFormat, 100, out) // bmp is your Bitmap instance
            isSuccess = true
        } catch (e: Exception) {
            e.printStackTrace()
            isSuccess = false
        } finally {
            try {
                out?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return isSuccess
    }
}