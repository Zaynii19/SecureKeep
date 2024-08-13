package com.example.securekeep.alarmsetup

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

//this class executes after install app even user start app or not
class ApplicationClass : Application() {
    companion object {
        const val CHANNEL_ID = "channel1"
    }
    override fun onCreate() {
        super.onCreate()
        val notificationChannel = NotificationChannel(CHANNEL_ID, "Alarm Service", NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.description = "This is an important channel for Alarm"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }
}