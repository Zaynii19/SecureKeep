package com.example.securekeep

import android.content.Context
import android.content.SharedPreferences

class MyPreferences(private val context: Context) {

    private var isVibrate = false
    private var isFlash = false
    private var isAlarmActive = false
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)

    fun storePreferences() {
        isAlarmActive = false
        isVibrate = false
        isFlash = false

        val editor = sharedPreferences.edit()

        editor.putBoolean("AlarmStatusTouch", isAlarmActive)
        editor.putBoolean("FlashStatusTouch", isFlash)
        editor.putBoolean("VibrateStatusTouch", isVibrate)

        editor.putBoolean("AlarmStatusPocket", isAlarmActive)
        editor.putBoolean("FlashStatusPocket", isFlash)
        editor.putBoolean("VibrateStatusPocket", isVibrate)

        editor.putBoolean("AlarmStatusOverCharge", isAlarmActive)
        editor.putBoolean("FlashStatusOverCharge", isFlash)
        editor.putBoolean("VibrateStatusTouch", isVibrate)

        editor.putBoolean("AlarmStatusWifi", isAlarmActive)
        editor.putBoolean("FlashStatusWifi", isFlash)
        editor.putBoolean("VibrateStatusWifi", isVibrate)

        editor.putBoolean("AlarmStatusCharge", isAlarmActive)
        editor.putBoolean("FlashStatusCharge", isFlash)
        editor.putBoolean("VibrateStatusCharge", isVibrate)

        editor.putBoolean("AlarmStatusEarphone", isAlarmActive)
        editor.putBoolean("FlashStatusEarphone", isFlash)
        editor.putBoolean("VibrateStatusEarphone", isVibrate)

        editor.apply()
    }
}