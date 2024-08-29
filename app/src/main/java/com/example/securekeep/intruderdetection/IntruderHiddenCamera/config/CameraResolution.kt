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
 * Created by Keval on 12-Nov-16.
 * Supported output image resolutions.
 *
 * @author [&#39;https://github.com/kevalpatel2106&#39;]['https://github.com/kevalpatel2106']
 */
class CameraResolution private constructor() {
    init {
        throw RuntimeException("Cannot initiate CameraResolution.")
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(*[HIGH_RESOLUTION, MEDIUM_RESOLUTION, LOW_RESOLUTION])
    annotation class SupportedResolution
    companion object {
        /**
         * This will capture the image at the highest possible resolution. That means if the camera sensor
         * is of 13MP, output image will have resolution of 13MP.
         */
        const val HIGH_RESOLUTION: Int = 2006

        /**
         * This will capture the image at the medium resolution. That means if the camera sensor
         * is of 13MP, it will take image with resolution that is exact middle of the supported camera
         * resolutions ([Camera.Parameters.getSupportedPictureSizes]).
         */
        const val MEDIUM_RESOLUTION: Int = 7895

        /**
         * This will capture the image at the lowest possible resolution. That means if the camera sensor
         * supports minimum 2MP, output image will have resolution of 2MP.
         */
        const val LOW_RESOLUTION: Int = 7821
    }
}