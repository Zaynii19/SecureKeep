package com.example.securekeep

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.databinding.ActivityPinBinding

class PinActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPinBinding.inflate(layoutInflater)
    }
    private var isHidden = true
    private lateinit var sharedPreferences: SharedPreferences // Declare it here
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
        val currentPin = sharedPreferences.getString("USER_PIN", "") ?: ""

        binding.setPinCode.setOnClickListener {
            val intent = Intent(this, CreatePinActivity::class.java)
            intent.putExtra("CHANGE_PIN", true)
            startActivity(intent)
        }

        binding.seeHiddenBtn.setOnClickListener {
            if (isHidden){
                isHidden = false
                binding.currentPIn.text = getString(R.string.staric)
                binding.seeHiddenBtn.setImageResource(R.drawable.hidden)
            } else {
                isHidden = true
                binding.currentPIn.text = currentPin
                binding.seeHiddenBtn.setImageResource(R.drawable.see)
            }
        }

    }
}