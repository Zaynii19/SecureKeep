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
package com.example.securekeep.intruderdetection.IntruderHiddenCamera.config

import androidx.annotation.IntDef


/**
 * Created by Keval on 10-Nov-16.
 * Supported camera facings.
 *
 * @author [&#39;https://github.com/kevalpatel2106&#39;]['https://github.com/kevalpatel2106']
 */
class CameraFacing private constructor() {
    init {
        throw RuntimeException("Cannot initialize this class.")
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(*[REAR_FACING_CAMERA, FRONT_FACING_CAMERA])
    annotation class SupportedCameraFacing
    companion object {
        /**
         * Rear facing camera id.
         *
         * @see android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK
         */
        const val REAR_FACING_CAMERA: Int = 0

        /**
         * Front facing camera id.
         *
         * @see android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT
         */
        const val FRONT_FACING_CAMERA: Int = 1
    }
}