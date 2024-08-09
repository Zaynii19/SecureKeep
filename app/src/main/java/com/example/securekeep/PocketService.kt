package com.example.securekeep

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log

class PocketService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var proximitySensor: Sensor
    private lateinit var wakeLock: PowerManager.WakeLock
    private var isActivated = false
    private var isVibrate: Boolean = false
    private var isFlash: Boolean = false

    override fun onCreate() {
        super.onCreate()

        // Initialize SensorManager and Proximity Sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)!!

        // Initialize WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::PocketServiceWakelock")
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isVibrate = intent?.getBooleanExtra("IS_VIBRATE", false) ?: false
        isFlash = intent?.getBooleanExtra("IS_FLASH", false) ?: false
        isActivated = true
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_UI)
        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        wakeLock.release()
        isActivated = false
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PROXIMITY && isActivated) {
            Log.d("PocketService", "Proximity value: ${event.values[0]}")
            if (event.values[0] < proximitySensor.maximumRange) {
                triggerAlarm()
            }
        }
    }

    private fun triggerAlarm() {
        val alarmIntent = Intent(this, EnterPinActivity::class.java)
        alarmIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        alarmIntent.putExtra("IS_VIBRATE", isVibrate)
        alarmIntent.putExtra("IS_FLASH", isFlash)
        startActivity(alarmIntent)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do nothing
    }
}
