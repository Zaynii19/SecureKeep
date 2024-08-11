package com.example.securekeep

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.databinding.ActivityCreatePinBinding

class CreatePinActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private val binding by lazy {
        ActivityCreatePinBinding.inflate(layoutInflater)
    }
    private lateinit var pinDots: Array<View>
    private var enteredPin = ""
    private var isChangePinMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)

        isChangePinMode = intent.getBooleanExtra("CHANGE_PIN", false)

        binding.backBtn.setOnClickListener {
            finish()
        }

        pinDots = arrayOf(
            binding.pinDot1, binding.pinDot2, binding.pinDot3, binding.pinDot4
        )

        setupPinButtons()

    }

    private fun setupPinButtons() {
        val buttonIds = listOf(
            binding.btn0 to "0",
            binding.btn1 to "1",
            binding.btn2 to "2",
            binding.btn3 to "3",
            binding.btn4 to "4",
            binding.btn5 to "5",
            binding.btn6 to "6",
            binding.btn7 to "7",
            binding.btn8 to "8",
            binding.btn9 to "9",
            binding.clearBtn to "CLEAR"
        )

        buttonIds.forEach { (button, value) ->
            button.setOnClickListener {
                if (value == "CLEAR") {
                    onClearClick()
                } else {
                    onDigitClick(value)
                    if (enteredPin.length == 4) {
                        pinCreate()
                    }
                }
            }
        }
    }

    private fun onDigitClick(digit: String) {
        if (enteredPin.length < 4) {
            enteredPin += digit
            updatePinDots()
        }
    }

    private fun updatePinDots() {
        pinDots.forEachIndexed { index, view ->
            view.setBackgroundColor(if (index < enteredPin.length) Color.GREEN else Color.WHITE)
        }
    }

    private fun onClearClick() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.substring(0, enteredPin.length - 1)
            updatePinDots()
        }
    }

    private fun pinCreate() {
        val editor = sharedPreferences.edit()
        editor.putString("USER_PIN", enteredPin)
        editor.apply()

        val isFirstLaunch = sharedPreferences.getBoolean("IS_FIRST_LAUNCH", true)

        if (isFirstLaunch) {
            editor.putBoolean("IS_FIRST_LAUNCH", false).apply()
            Toast.makeText(this, "PIN Created Successfully", Toast.LENGTH_SHORT).show()
            // Start MainActivity after PIN creation on first launch
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            Toast.makeText(this, "PIN Changed Successfully", Toast.LENGTH_SHORT).show()
            // Finish the activity after changing PIN
            finish()
        }
    }

}

