package com.example.securekeep

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
            val isFirstLaunch = sharedPreferences.getBoolean("IS_FIRST_LAUNCH", true)
            val storedPin = sharedPreferences.getString("USER_PIN", null)

            val nextActivity = when {
                isFirstLaunch || storedPin == null -> {
                    CreatePinActivity::class.java
                }
                else -> {
                    MainActivity::class.java
                }
            }

            startActivity(Intent(this, nextActivity))
            finish()
        }, 3000) // 3 seconds
    }
}


