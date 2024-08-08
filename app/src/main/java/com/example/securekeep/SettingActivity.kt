package com.example.securekeep

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.databinding.ActivitySettingBinding

class SettingActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySettingBinding.inflate(layoutInflater)
    }
    private lateinit var audioManager: AudioManager
    private var currentSoundLevel: Int = 0

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

        binding.backBtn.setOnClickListener {
            startActivity(Intent(this@SettingActivity, MainActivity::class.java))
        }

        // Retrieving selected attempts, alert status
        val preferences = getPreferences(MODE_PRIVATE)

        // Initialize SeekBar
        binding.seekBarSound.max = 100 // Set the max value for the SeekBar
        binding.seekBarSound.progress = preferences.getInt("SOUND_LEVEL", currentSoundLevel)
        setSystemSoundLevel(currentSoundLevel)

        binding.seekBarSound.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setSoundLevel(progress)
                currentSoundLevel = progress

                // Storing sound level in shared preferences
                val editor = getPreferences(MODE_PRIVATE).edit()
                editor.putInt("SOUND_LEVEL", currentSoundLevel)
                editor.apply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Optional: handle when the user starts touching the SeekBar
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Optional: handle when the user stops touching the SeekBar
            }
        })
    }

    private fun setSystemSoundLevel(level: Int) {
        // Ensure the level is between 0 and 100
        val clampedLevel = level.coerceIn(0, 100)

        // Get the max volume level for the stream
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        // Calculate the desired volume based on the percentage level
        val volume = (clampedLevel * maxVolume) / 100

        // Set the system volume
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI)
    }


    /*private fun getCurrentSoundLevel(): Int {
        // Fetch the current sound level as a percentage
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return (currentVolume * 100) / maxVolume
    }*/

    private fun setSoundLevel(level: Int) {
        // Set the sound level based on SeekBar progress
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = (level * maxVolume) / 100
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI)
    }
}
