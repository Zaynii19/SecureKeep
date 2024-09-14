package com.example.securekeep.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.R
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
    private var systemToneUri: Uri? = null

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

        // Retrieve sound level from shared preferences
        currentSoundLevel = sharedPreferences.getInt("SOUND_LEVEL", 70)
        setSystemSoundLevel(currentSoundLevel)
        // Retrieve motion sensitivity level from shared preferences
        sensitivityThreshold = sharedPreferences.getFloat("motionSensitivity", 15.0f)
        // Retrieve tone settings from SharedPreferences
        toneId = sharedPreferences.getInt("alarm_tone", R.raw.alarm_tune_1)
        val toneUriString = sharedPreferences.getString("alarm_tone_uri", null)
        systemToneUri = toneUriString?.let { Uri.parse(it) } // Convert to Uri if exists

        binding.alarmToneName.text = when (toneId) {
            R.raw.alarm_tune_1 -> "Tone 1"
            R.raw.alarm_tune_1 -> "Tone 2"
            R.raw.alarm_tune_3 -> "Tone 3"
            R.raw.alarm_tune_4 -> "Tone 4"
            R.raw.alarm_tune_5 -> "Tone 5"
            else -> "System Tones" // Default to System Tones if no match is found
        }

        // Initialize sound SeekBar
        binding.seekBarSound.max = 100
        binding.seekBarSound.progress = currentSoundLevel

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.seekBarSound.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setSystemSoundLevel(progress)
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
        binding.seekBarMotion.progress = (sensitivityThreshold / 30.0f * 100).toInt() // Convert to SeekBar progress

        binding.seekBarMotion.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sensitivityThreshold = progress / 100f * 30.0f

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
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0) // Remove FLAG_SHOW_UI
    }

    private fun showTonePickerDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_tone_picker, null)
        builder.setView(dialogView)

        val dialog = builder.create()

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
        )

        tuneOptions.forEachIndexed { index, layout ->
            layout.setOnClickListener {
                if (index == 5) {
                    // Stop the currently playing tone when "System Tones" is selected
                    stopMediaPlayer()

                    // Open system ringtone picker
                    val ringtonePickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                    ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, systemToneUri)
                    startActivityForResult(ringtonePickerIntent, REQUEST_CODE_RINGTONE_PICKER)

                    // Update the selected layout background for system tones
                    updateSelectedToneLayout(layout, 0) // Pass 0 for system tone selection
                } else {
                    // Play the selected internal tone
                    playTone(tuneIds[index])
                    updateSelectedToneLayout(layout, tuneIds[index])
                }
            }
        }

        dialog.setOnDismissListener {
            stopMediaPlayer() // Ensure MediaPlayer is stopped when the dialog is dismissed
        }

        // Handle Apply button click
        val applyBtn = dialogView.findViewById<Button>(R.id.applyBtn)
        applyBtn.setOnClickListener {
            toneId = selectedToneId // Set the currently selected tone ID

            // Stop any currently playing tone, including system tones
            stopMediaPlayer()

            // Update the name display
            binding.alarmToneName.text = when (toneId) {
                R.raw.alarm_tune_1 -> "Tone 1"
                R.raw.alarm_tune_2 -> "Tone 2"
                R.raw.alarm_tune_3 -> "Tone 3"
                R.raw.alarm_tune_4 -> "Tone 4"
                R.raw.alarm_tune_5 -> "Tone 5"
                else -> {
                    systemToneUri?.let {
                        val ringtone: Ringtone = RingtoneManager.getRingtone(applicationContext, it)
                        ringtone.stop() // Stop the system tone when applying
                    }
                    "System Tone" // Label for system tone
                }
            }

            // Save tone ID or URI in shared preferences
            val editor = sharedPreferences.edit()
            if (toneId == 0) { // If toneId is 0, it means a system tone is selected
                systemToneUri?.let { editor.putString("alarm_tone_uri", it.toString()) }
                editor.putInt("alarm_tone", 0) // Store 0 as toneId to indicate system tone
            } else {
                editor.putInt("alarm_tone", toneId)
                editor.putString("alarm_tone_uri", null) // Clear system tone URI
            }
            editor.apply()

            dialog.dismiss()
        }


        dialog.show()
        dialog.setOnDismissListener {
            stopMediaPlayer()
        }
    }


    // Handle the result from the ringtone picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_RINGTONE_PICKER && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                systemToneUri = uri
                playTone(uri) // Play the selected ringtone automatically
            }
        }
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

    private fun playTone(uri: Uri) {
        // Stop and release the previous MediaPlayer if it's still playing
        mediaPlayer?.stop()
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@SettingActivity, uri)
            prepare()
            start() // Play the selected ringtone
        }
    }

    // Helper method to update selected tone layout
    private fun updateSelectedToneLayout(layout: ConstraintLayout, toneId: Int) {
        selectedLayout?.background = ContextCompat.getDrawable(this, R.drawable.simple_round_boarder)
        layout.background = ContextCompat.getDrawable(this, R.drawable.selected_tone_round_boarder)
        selectedLayout = layout
        selectedToneId = toneId
    }

    private fun stopMediaPlayer() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release MediaPlayer if the activity is destroyed to avoid memory leaks
        mediaPlayer?.release()
    }

    companion object {
        private const val REQUEST_CODE_RINGTONE_PICKER = 1001
    }
}
