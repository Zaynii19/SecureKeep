package com.example.securekeep

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class MyPreferences(context: Context) {

    private var isVibrate = false
    private var isFlash = false
    private var isAlarmActive = false
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)

    fun storePreferences() {
        isAlarmActive = false
        isVibrate = false
        isFlash = false

        sharedPreferences.edit {
            putBoolean("AlarmStatusTouch", isAlarmActive)
            putBoolean("FlashStatusTouch", isFlash)
            putBoolean("VibrateStatusTouch", isVibrate)

            putBoolean("AlarmStatusPocket", isAlarmActive)
            putBoolean("FlashStatusPocket", isFlash)
            putBoolean("VibrateStatusPocket", isVibrate)

            putBoolean("AlarmStatusOverCharge", isAlarmActive)
            putBoolean("FlashStatusOverCharge", isFlash)
            putBoolean("VibrateStatusTouch", isVibrate)

            putBoolean("AlarmStatusWifi", isAlarmActive)
            putBoolean("FlashStatusWifi", isFlash)
            putBoolean("VibrateStatusWifi", isVibrate)

            putBoolean("AlarmStatusCharge", isAlarmActive)
            putBoolean("FlashStatusCharge", isFlash)
            putBoolean("VibrateStatusCharge", isVibrate)

            putBoolean("AlarmStatusEarphone", isAlarmActive)
            putBoolean("FlashStatusEarphone", isFlash)
            putBoolean("VibrateStatusEarphone", isVibrate)
        }
    }
}