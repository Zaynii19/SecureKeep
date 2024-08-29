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

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.securekeep.intruderdetection.IntruderHiddenCamera.config.CameraFacing

/**
 * Created by Keval on 27-Oct-16.
 * This abstract class provides ability to handle background camera to the fragment in which it is
 * extended.
 *
 * @author [&#39;https://github.com/kevalpatel2106&#39;]['https://github.com/kevalpatel2106']
 */
abstract class HiddenCameraFragment : Fragment(), CameraCallbacks {
    private var mCachedCameraConfig: CameraConfig? = null
    private var mCameraPreview: CameraPreview? = null

    /**
     * Start the hidden camera. Make sure that you check for the runtime permissions before you start
     * the camera.
     *
     * @param cameraConfig camera configuration [CameraConfig]
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    protected fun startCamera(cameraConfig: CameraConfig) {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) { //check if the camera permission is available

            onCameraError(CameraError.ERROR_CAMERA_PERMISSION_NOT_AVAILABLE)
        } else if (cameraConfig.facing === CameraFacing.FRONT_FACING_CAMERA
            && !HiddenCameraUtils.isFrontCameraAvailable(requireActivity())
        ) {   //Check if for the front camera

            onCameraError(CameraError.ERROR_DOES_NOT_HAVE_FRONT_CAMERA)
        } else {
            //Add the camera preview surface to the root of the activity view.
            if (mCameraPreview == null) mCameraPreview = addPreView()
            mCameraPreview!!.startCameraInternal(cameraConfig)
            mCachedCameraConfig = cameraConfig
        }
    }

    /**
     * Call this method to capture the image using the camera you initialized. Don't forget to
     * initialize the camera using [.startCamera] before using this function.
     */
    protected fun takePicture() {
        if (mCameraPreview != null) {
            if (mCameraPreview!!.isSafeToTakePictureInternal) {
                mCameraPreview!!.takePictureInternal()
            }
        } else {
            throw RuntimeException("Background camera not initialized. Call startCamera() to initialize the camera.")
        }
    }

    /**
     * Stop and release the camera forcefully.
     */
    protected fun stopCamera() {
        mCachedCameraConfig = null //Remove config.
        mCameraPreview?.stopPreviewAndFreeCamera()
    }

    /**
     * Add camera preview to the root of the activity layout.
     *
     * @return [CameraPreview] that was added to the view.
     */
    private fun addPreView(): CameraPreview {
        //create fake camera view
        val cameraSourceCameraPreview: CameraPreview = CameraPreview(requireContext(), this)
        cameraSourceCameraPreview.setLayoutParams(
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val view = (requireActivity().window.decorView.rootView as ViewGroup).getChildAt(0)

        if (view is LinearLayout) {
            val params = LinearLayout.LayoutParams(1, 1)
            view.addView(cameraSourceCameraPreview, params)
        } else if (view is RelativeLayout) {
            val params = RelativeLayout.LayoutParams(1, 1)
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE)
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            view.addView(cameraSourceCameraPreview, params)
        } else if (view is FrameLayout) {
            val params = FrameLayout.LayoutParams(1, 1)
            view.addView(cameraSourceCameraPreview, params)
        } else {
            throw RuntimeException("Root view of the activity/fragment cannot be other than Linear/Relative or frame layout")
        }

        return cameraSourceCameraPreview
    }

    override fun onResume() {
        super.onResume()
        if (mCachedCameraConfig != null) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                return
            }
            startCamera(mCachedCameraConfig!!)
        }
    }

    override fun onPause() {
        super.onPause()
        mCameraPreview?.stopPreviewAndFreeCamera()
    }
}