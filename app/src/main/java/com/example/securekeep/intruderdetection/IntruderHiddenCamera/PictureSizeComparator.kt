package com.example.securekeep.intruderdetection.IntruderHiddenCamera

import android.hardware.Camera

/**
 * Created by Keval on 14/11/17.
 * This comparator will sort all the [Camera.Size] into the dessending order of the total number
 * of pixels.
 *
 * @author [kevalpatel2106](https://github.com/kevalpatel2106)
 */
internal class PictureSizeComparator : Comparator<Camera.Size?> {
    // Used for sorting in ascending order of
    // roll name

    override fun compare(a: Camera.Size?, b: Camera.Size?): Int {
        return (b!!.height * b.width) - (a!!.height * a.width)
    }
}