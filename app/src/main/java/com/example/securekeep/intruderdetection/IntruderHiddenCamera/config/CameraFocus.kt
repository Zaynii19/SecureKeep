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
 * Supported camera focus.
 *
 * @author [&#39;https://github.com/kevalpatel2106&#39;]['https://github.com/kevalpatel2106']
 */
class CameraFocus private constructor() {
    init {
        throw RuntimeException("Cannot initialize this class.")
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(*[AUTO, CONTINUOUS_PICTURE, NO_FOCUS])
    annotation class SupportedCameraFocus
    companion object {
        /**
         * Camera should focus automatically. This is the default focus mode if the camera focus
         * is not set.
         *
         * @see Camera.Parameters.FOCUS_MODE_AUTO
         */
        const val AUTO: Int = 0

        /**
         * Camera should focus automatically.
         *
         * @see Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
         */
        const val CONTINUOUS_PICTURE: Int = 1

        /**
         * Do not focus the camera.
         */
        const val NO_FOCUS: Int = 2
    }
}