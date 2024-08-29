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

import android.content.Context
import android.hardware.Camera
import com.example.securekeep.intruderdetection.IntruderHiddenCamera.config.CameraFacing
import com.example.securekeep.intruderdetection.IntruderHiddenCamera.config.CameraFocus
import com.example.securekeep.intruderdetection.IntruderHiddenCamera.config.CameraImageFormat
import com.example.securekeep.intruderdetection.IntruderHiddenCamera.config.CameraResolution
import com.example.securekeep.intruderdetection.IntruderHiddenCamera.config.CameraRotation
import java.io.File

/**
 * Created by Keval on 12-Nov-16.
 *
 * @author [&#39;https://github.com/kevalpatel2106&#39;]['https://github.com/kevalpatel2106']
 */
class CameraConfig() {
    private var mContext: Context? = null

    @get:CameraResolution.SupportedResolution
    @CameraResolution.SupportedResolution
    var resolution: Int = CameraResolution.MEDIUM_RESOLUTION
        private set

    @get:CameraFacing.SupportedCameraFacing
    @CameraFacing.SupportedCameraFacing
    var facing: Int = CameraFacing.REAR_FACING_CAMERA
        private set

    @get:CameraImageFormat.SupportedImageFormat
    @CameraImageFormat.SupportedImageFormat
    var imageFormat: Int = CameraImageFormat.FORMAT_JPEG
        private set

    @get:CameraRotation.SupportedRotation
    @CameraRotation.SupportedRotation
    var imageRotation: Int = CameraRotation.ROTATION_0
        private set

    @CameraFocus.SupportedCameraFocus
    private var mCameraFocus: Int = CameraFocus.AUTO

    var imageFile: File? = null
        private set

    fun getBuilder(context: Context?): Builder {
        mContext = context
        return Builder()
    }

    val focusMode: String?
        get() {
            when (mCameraFocus) {
                CameraFocus.AUTO -> return Camera.Parameters.FOCUS_MODE_AUTO
                CameraFocus.CONTINUOUS_PICTURE -> return Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                CameraFocus.NO_FOCUS -> return null
                else -> throw RuntimeException("Invalid camera focus mode.")
            }
        }

    inner class Builder() {
        /**
         * Set the resolution of the output camera image. If you don't specify any resolution,
         * default image resolution will set to [CameraResolution.MEDIUM_RESOLUTION].
         *
         * @param resolution Any resolution from:
         *  * [CameraResolution.HIGH_RESOLUTION]
         *  * [CameraResolution.MEDIUM_RESOLUTION]
         *  * [CameraResolution.LOW_RESOLUTION]
         * @return [Builder]
         * @see CameraResolution
         */
        fun setCameraResolution(@CameraResolution.SupportedResolution resolution: Int): Builder {
            //Validate input

            if ((resolution != CameraResolution.HIGH_RESOLUTION) && (
                        resolution != CameraResolution.MEDIUM_RESOLUTION) && (
                        resolution != CameraResolution.LOW_RESOLUTION)
            ) {
                throw RuntimeException("Invalid camera resolution.")
            }

            this@CameraConfig.resolution = resolution
            return this
        }

        /**
         * Set the camera facing with which you want to capture image.
         * Either rear facing camera or front facing camera. If you don't provide any camera facing,
         * default camera facing will be [CameraFacing.FRONT_FACING_CAMERA].
         *
         * @param cameraFacing Any camera facing from:
         *  * [CameraFacing.REAR_FACING_CAMERA]
         *  * [CameraFacing.FRONT_FACING_CAMERA]
         * @return [Builder]
         * @see CameraFacing
         */
        fun setCameraFacing(@CameraFacing.SupportedCameraFacing cameraFacing: Int): Builder {
            //Validate input
            if (cameraFacing != CameraFacing.REAR_FACING_CAMERA &&
                cameraFacing != CameraFacing.FRONT_FACING_CAMERA
            ) {
                throw RuntimeException("Invalid camera facing value.")
            }

            this@CameraConfig.facing = cameraFacing
            return this
        }

        /**
         * Set the camera focus mode. If you don't provide any camera focus mode,
         * default focus mode will be [CameraFocus.AUTO].
         *
         * @param focusMode Any camera focus mode from:
         *  * [CameraFocus.AUTO]
         *  * [CameraFocus.CONTINUOUS_PICTURE]
         *  * [CameraFocus.NO_FOCUS]
         * @return [Builder]
         * @see CameraFacing
         */
        fun setCameraFocus(@CameraFocus.SupportedCameraFocus focusMode: Int): Builder {
            //Validate input
            if ((focusMode != CameraFocus.AUTO) && (
                        focusMode != CameraFocus.CONTINUOUS_PICTURE) && (
                        focusMode != CameraFocus.NO_FOCUS)
            ) {
                throw RuntimeException("Invalid camera focus mode.")
            }

            mCameraFocus = focusMode
            return this
        }

        /**
         * Specify the image format for the output image. If you don't specify any output format,
         * default output format will be [CameraImageFormat.FORMAT_JPEG].
         *
         * @param imageFormat Any supported image format from:
         *  * [CameraImageFormat.FORMAT_JPEG]
         *  * [CameraImageFormat.FORMAT_PNG]
         * @return [Builder]
         * @see CameraImageFormat
         */
        fun setImageFormat(@CameraImageFormat.SupportedImageFormat imageFormat: Int): Builder {
            //Validate input
            if (imageFormat != CameraImageFormat.FORMAT_JPEG &&
                imageFormat != CameraImageFormat.FORMAT_PNG
            ) {
                throw RuntimeException("Invalid output image format.")
            }

            this@CameraConfig.imageFormat = imageFormat
            return this
        }

        /**
         * Specify the output image rotation. The output image will be rotated by amount of degree specified
         * before stored to the output file. By default there is no rotation applied.
         *
         * @param rotation Any supported rotation from:
         *  * [CameraRotation.ROTATION_0]
         *  * [CameraRotation.ROTATION_90]
         *  * [CameraRotation.ROTATION_180]
         *  * [CameraRotation.ROTATION_270]
         * @return [Builder]
         * @see CameraRotation
         */
        fun setImageRotation(@CameraRotation.SupportedRotation rotation: Int): Builder {
            //Validate input
            if ((rotation != CameraRotation.ROTATION_0
                        ) && (rotation != CameraRotation.ROTATION_90
                        ) && (rotation != CameraRotation.ROTATION_180
                        ) && (rotation != CameraRotation.ROTATION_270)
            ) {
                throw RuntimeException("Invalid image rotation.")
            }

            this@CameraConfig.imageRotation = rotation
            return this
        }

        /**
         * Set the location of the out put image. If you do not set any file for the output image, by
         * default image will be stored in the application's cache directory.
         *
         * @param imageFile [File] where you want to store the image.
         * @return [Builder]
         */
        fun setImageFile(imageFile: File?): Builder {
            this@CameraConfig.imageFile = imageFile
            return this
        }

        /**
         * Build the configuration.
         *
         * @return [CameraConfig]
         */
        fun build(): CameraConfig {
            if (this@CameraConfig.imageFile == null) this@CameraConfig.imageFile = defaultStorageFile
            return this@CameraConfig
        }

        private val defaultStorageFile: File
            /**
             * Get the new file to store the image if there isn't any custom file location available.
             * This will create new file into the cache directory of the application.
             */
            get() = File(
                ((HiddenCameraUtils.getCacheDir(mContext!!).absolutePath
                        + File.separator
                        + "IMG_" + System.currentTimeMillis() //IMG_214515184113123.png
                        + (if (this@CameraConfig.imageFormat == CameraImageFormat.FORMAT_JPEG) ".jpeg" else ".png")))
            )
    }
}