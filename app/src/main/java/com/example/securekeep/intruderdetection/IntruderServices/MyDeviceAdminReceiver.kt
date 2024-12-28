package com.example.securekeep.intruderdetection.IntruderServices

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.util.Log

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onPasswordFailed(context: Context, intent: Intent, userHandle: UserHandle) {
        super.onPasswordFailed(context, intent, userHandle)
        // Log the failure for debugging
        Log.i("DeviceAdminReceiver", "Password failed")

        // Broadcast the failure
        val failedIntent = Intent("PASSWORD_ATTEMPT_FAILED")
        context.sendBroadcast(failedIntent)
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent, userHandle: UserHandle) {
        super.onPasswordSucceeded(context, intent, userHandle)
        // Log the success for debugging
        Log.i("DeviceAdminReceiver", "Password succeeded")
    }
}