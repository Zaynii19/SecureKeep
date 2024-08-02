package com.example.securekeep

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import android.os.Vibrator
import android.content.Context

class TouchDetectionService : AccessibilityService() {

    private lateinit var vibrator: Vibrator

    override fun onServiceConnected() {
        super.onServiceConnected()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_TOUCH_INTERACTION_START or AccessibilityEvent.TYPE_TOUCH_INTERACTION_END
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> {
                triggerAlarm()
            }
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> {
                // Optionally handle touch end
            }
        }
    }

    override fun onInterrupt() {
        // Handle interruptions
    }

    private fun triggerAlarm() {
        Toast.makeText(this, "Alarm Triggered!", Toast.LENGTH_SHORT).show()
        vibrator.vibrate(5000) // Vibrate for 5 seconds
        // You can add more alarm actions here (e.g., play a sound, send a notification)
    }
}
