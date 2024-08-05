package com.example.securekeep

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder

class PocketService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var proximitySensor: Sensor
    private var isVibrate: Boolean = false
    private var isFlash: Boolean = false

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)!!
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isVibrate = intent?.getBooleanExtra("IS_VIBRATE", false) ?: false
        isFlash = intent?.getBooleanExtra("IS_FLASH", false) ?: false

        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_UI)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] >= proximitySensor.maximumRange) {
                val alarmIntent = Intent(this, EnterPinActivity::class.java)
                alarmIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                alarmIntent.putExtra("IS_VIBRATE", isVibrate)
                alarmIntent.putExtra("IS_FLASH", isFlash)
                startActivity(alarmIntent)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do nothing
    }
}
