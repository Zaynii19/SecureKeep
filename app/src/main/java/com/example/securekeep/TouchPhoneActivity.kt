package com.example.securekeep

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.databinding.ActivityTouchPhoneBinding

class TouchPhoneActivity : AppCompatActivity(), SensorEventListener {

    private val binding by lazy {
        ActivityTouchPhoneBinding.inflate(layoutInflater)
    }

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private var mAccel: Float = 0.0f
    private var mAccelCurrent: Float = 0.0f
    private var mAccelLast: Float = 0.0f
    private lateinit var alertDialog: AlertDialog
    private var isAlarmActive = false
    private lateinit var cdt: CountDownTimer
    private var isAlarmTriggered = false
    private var isMotionDetectionEnabled = false
    private var mediaPlayer: MediaPlayer? = null
    private var isVibrate = false
    private var isFlash = false
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private lateinit var handler: android.os.Handler
    private val flashRunnable = object : Runnable {
        override fun run() {
            toggleFlashlight()
            handler.postDelayed(this, 1000) // Flash every second
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize flash light service
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handler = android.os.Handler(Looper.getMainLooper())
        try {
            cameraId = cameraManager.cameraIdList[0] // Usually, the first camera is the back camera
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            Toast.makeText(this, "Camera not accessible", Toast.LENGTH_SHORT).show()
            return
        }

        // Initialize sensor manager and accelerometer
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!

        alertDialog = AlertDialog.Builder(this)
            .setTitle("Will Be Activated In 10 Seconds")
            .setMessage("00:10")
            .setCancelable(false)
            .create()

        binding.powerBtn.setOnClickListener {
            if (!isAlarmActive) {
                isAlarmActive = true
                alertDialog.show()

                cdt = object : CountDownTimer(10000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        alertDialog.setMessage("00:${millisUntilFinished / 1000}")
                    }

                    override fun onFinish() {
                        if (alertDialog.isShowing) {
                            alertDialog.dismiss()
                        }
                        Toast.makeText(this@TouchPhoneActivity, "Motion Detection Mode Activated", Toast.LENGTH_SHORT).show()
                        binding.powerBtn.setImageResource(R.drawable.power_off)
                        isMotionDetectionEnabled = true // Enable motion detection here
                    }
                }.start()
            } else {
                Toast.makeText(this@TouchPhoneActivity, "Motion Detection Mode Deactivated", Toast.LENGTH_SHORT).show()
                binding.powerBtn.setImageResource(R.drawable.power_on)
                isMotionDetectionEnabled = false
                isAlarmActive = false
                isAlarmTriggered = false // Reset the alarm trigger flag

                // Stop the alarm tone
                mediaPlayer?.apply {
                    if (isPlaying) {
                        stop()
                        prepare() // Prepare the MediaPlayer for future use
                    }
                }

                // Stop vibration and flashlight
                handler.removeCallbacks(flashRunnable)
                if (isFlash) {
                    toggleFlashlight() // Ensure the flashlight is turned off
                }
                isFlash = false
                binding.switchBtnF.setImageResource(R.drawable.switch_off)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // For Android 12 (API level 31) and above
                    val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    val vibrator = vibratorManager.defaultVibrator
                    vibrator.cancel()
                } else {
                    // For Android versions below 12
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibrator.cancel()
                }
                isVibrate = false
                binding.switchBtnV.setImageResource(R.drawable.switch_off)
            }
        }


        // Vibration button
        binding.switchBtnV.setOnClickListener {
            if (!isVibrate) {
                isVibrate = true
                binding.switchBtnV.setImageResource(R.drawable.switch_on)
                Toast.makeText(this, "Vibration Enabled", Toast.LENGTH_SHORT).show()
            } else {
                isVibrate = false
                binding.switchBtnV.setImageResource(R.drawable.switch_off)
                Toast.makeText(this, "Vibration Disabled", Toast.LENGTH_SHORT).show()
            }
        }
        // Flash button
        binding.switchBtnF.setOnClickListener {
            if (!isFlash) {
                isFlash = true
                binding.switchBtnF.setImageResource(R.drawable.switch_on)
                Toast.makeText(this, "Flash Turned on", Toast.LENGTH_SHORT).show()
            } else {
                isFlash = false
                binding.switchBtnF.setImageResource(R.drawable.switch_off)
                Toast.makeText(this, "Flash Turned off", Toast.LENGTH_SHORT).show()
            }
        }

        mAccel = 0.0f
        mAccelCurrent = SensorManager.GRAVITY_EARTH
        mAccelLast = SensorManager.GRAVITY_EARTH

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, 0)

        // Initialize MediaPlayer for alarm tone
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_tune_1)
    }

    private fun triggerAlarm() {
        // Play the alarm tone
        mediaPlayer?.apply {
            if (!isPlaying) {
                start()
            } else {
                seekTo(0) // Reset to the beginning if already playing
            }
        }
    }

    private fun triggerVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 (API level 31) and above
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            // For Android versions below 12
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    private fun triggerFlashLight() {
        handler.post(flashRunnable)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        mediaPlayer?.release() // Release MediaPlayer resources
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (isMotionDetectionEnabled && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val values = event.values
            val x = values[0]
            val y = values[1]
            val z = values[2]
            mAccelLast = mAccelCurrent
            mAccelCurrent = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta = mAccelCurrent - mAccelLast
            mAccel = mAccel * 0.9f + delta

            if (mAccel > 1.0 && !isAlarmTriggered) { // Adjust sensitivity as needed
                isAlarmTriggered = true
                triggerAlarm()
                if (isVibrate && isFlash) {
                    triggerVibration()
                    triggerFlashLight()
                } else if (isVibrate){
                    triggerVibration()
                } else if (isFlash){
                    triggerFlashLight()
                }
                // Start the PIN entry activity
                /*startActivity(Intent(this, EnterPinActivity::class.java))
                finish()*/
            }
        }
    }


    private fun toggleFlashlight() {
        try {
            cameraManager.setTorchMode(cameraId, !isFlash)
            isFlash = !isFlash
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            Toast.makeText(this, "Error toggling flashlight", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do nothing
    }
}
