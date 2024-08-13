package com.example.securekeep.intruderdetection

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        // Log the failure for debugging
        Log.i("DeviceAdminReceiver", "Password failed")

        // Broadcast the failure
        val failedIntent = Intent("PASSWORD_ATTEMPT_FAILED")
        context.sendBroadcast(failedIntent)
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        // Log the success for debugging
        Log.i("DeviceAdminReceiver", "Password succeeded")
    }
}
