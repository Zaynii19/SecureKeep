package com.example.securekeep

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.databinding.ActivitySettingBinding

class SettingActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySettingBinding.inflate(layoutInflater)
    }
    private lateinit var audioManager: AudioManager
    private var currentSoundLevel: Int = 0
    private var sensitivityThreshold: Float = 1.0f // Default to 1.0
    private var toneId: Int = 0
    private lateinit var sharedPreferences: SharedPreferences
    private var selectedLayout: ConstraintLayout? = null
    private var selectedToneId: Int = toneId // Store selected tone temporarily
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)

        binding.backBtn.setOnClickListener {
            finish()
        }

        // Retrieve sound level from shared preferences
        currentSoundLevel = sharedPreferences.getInt("SOUND_LEVEL", 50)
        setSystemSoundLevel(currentSoundLevel)

        // Retrieve motion sensitivity level from shared preferences
        sensitivityThreshold = sharedPreferences.getFloat("motionSensitivity", 1.0f)

        // Retrieve tone from shared preferences
        toneId = sharedPreferences.getInt("alarm_tone", R.raw.alarm_tune_1)
        binding.alarmToneName.text = when (toneId) {
            R.raw.alarm_tune_1 -> "Tone 1"
            R.raw.alarm_tune_1 -> "Tone 2"
            R.raw.alarm_tune_3 -> "Tone 3"
            R.raw.alarm_tune_4 -> "Tone 4"
            R.raw.alarm_tune_5 -> "Tone 5"
            else -> "Tone 1" // Default to Tune 1 if no match is found
        }

        // Initialize sound SeekBar
        binding.seekBarSound.max = 100
        binding.seekBarSound.progress = currentSoundLevel

        binding.seekBarSound.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setSoundLevel(progress)
                currentSoundLevel = progress

                // Store sound level in shared preferences
                val editor = sharedPreferences.edit()
                editor.putInt("SOUND_LEVEL", currentSoundLevel)
                editor.apply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Initialize motion sensitivity SeekBar
        binding.seekBarMotion.max = 100
        binding.seekBarMotion.progress = (sensitivityThreshold / 2.0f * 100).toInt() // Convert to SeekBar progress

        binding.seekBarMotion.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sensitivityThreshold = progress / 100f * 2.0f

                // Store motion sensitivity threshold in shared preferences
                val editor = sharedPreferences.edit()
                editor.putFloat("motionSensitivity", sensitivityThreshold)
                editor.apply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Set up listeners for tone selection
        binding.setAlarmTone.setOnClickListener { showTonePickerDialog() }

        binding.setPinCode.setOnClickListener {
            startActivity(Intent(this@SettingActivity, PinActivity::class.java))
        }
    }

    private fun setSystemSoundLevel(level: Int) {
        val clampedLevel = level.coerceIn(0, 100)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = (clampedLevel * maxVolume) / 100
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI)
    }

    private fun setSoundLevel(level: Int) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = (level * maxVolume) / 100
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI)
    }

    private fun showTonePickerDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_tone_picker, null)
        builder.setView(dialogView)

        val dialog = builder.create()

        // List of ConstraintLayouts for the tone options
        val tuneOptions = listOf(
            dialogView.findViewById<ConstraintLayout>(R.id.tune1),
            dialogView.findViewById<ConstraintLayout>(R.id.tune2),
            dialogView.findViewById<ConstraintLayout>(R.id.tune3),
            dialogView.findViewById<ConstraintLayout>(R.id.tune4),
            dialogView.findViewById<ConstraintLayout>(R.id.tune5),
            dialogView.findViewById<ConstraintLayout>(R.id.systemTunes)
        )

        val tuneIds = listOf(
            R.raw.alarm_tune_1,
            R.raw.alarm_tune_2,
            R.raw.alarm_tune_3,
            R.raw.alarm_tune_4,
            R.raw.alarm_tune_5
            // Add system tune ID if necessary
        )

        tuneOptions.forEachIndexed { index, layout ->
            layout.setOnClickListener {
                if (index == 5) {
                    // Show toast message if the layout is the 6th option
                    Toast.makeText(this, "Will be Added", Toast.LENGTH_SHORT).show()
                } else {
                    // Normal operation for other indices
                    playTone(tuneIds[index])

                    // Change the background color of the selected layout
                    selectedLayout?.setBackgroundColor(resources.getColor(R.color.white))
                    layout.setBackgroundColor(resources.getColor(R.color.selected_tone_color))
                    selectedLayout = layout

                    // Temporarily store the selected tone ID
                    selectedToneId = tuneIds[index]
                }
            }
        }


        // Handle Apply button click
        val applyBtn = dialogView.findViewById<Button>(R.id.applyBtn)
        applyBtn.setOnClickListener {
            // Apply and save the selected tone ID
            toneId = selectedToneId
            binding.alarmToneName.text = when (toneId) {
                R.raw.alarm_tune_1 -> "Tone 1"
                R.raw.alarm_tune_2 -> "Tone 2"
                R.raw.alarm_tune_3 -> "Tone 3"
                R.raw.alarm_tune_4 -> "Tone 4"
                R.raw.alarm_tune_5 -> "Tone 5"
                else -> "Tone 1" // Default to Tune 1 if no match is found
            }

            val editor = sharedPreferences.edit()
            editor.putInt("alarm_tone", toneId)
            editor.apply()

            mediaPlayer?.stop()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun playTone(toneId: Int) {
        // Stop and release the previous MediaPlayer if it's still playing
        mediaPlayer?.stop()
        mediaPlayer?.release()

        // Create a new MediaPlayer instance and play the tone
        mediaPlayer = MediaPlayer.create(this, toneId)
        mediaPlayer?.start()
        mediaPlayer?.setOnCompletionListener {
            it.release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release MediaPlayer if the activity is destroyed to avoid memory leaks
        mediaPlayer?.release()
    }
}
