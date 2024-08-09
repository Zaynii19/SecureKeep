package com.example.securekeep

import android.content.Intent
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
    private val binding by lazy {
        ActivityCreatePinBinding.inflate(layoutInflater)
    }
    private lateinit var pinDots: Array<View>
    private var enteredPin = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean("IS_FIRST_LAUNCH", true)
        val storedPin = sharedPreferences.getString("USER_PIN", null)

        val isPinChanged = intent.getBooleanExtra("CHANGE_PIN", false)

        // If there's already a PIN stored and it's not the first launch, navigate to MainActivity
        if (!isFirstLaunch && storedPin != null && isPinChanged) {
            Toast.makeText(this@CreatePinActivity, "Enter New Pin", Toast.LENGTH_SHORT).show()
        }

        // Handle first launch setup
        if (isFirstLaunch) {
            binding.backBtn.visibility = View.INVISIBLE
            sharedPreferences.edit().putBoolean("IS_FIRST_LAUNCH", false).apply()
        }

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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
                    pinCreate()
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
        if (enteredPin.length == 4) {
            // Save the entered PIN in SharedPreferences
            val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("USER_PIN", enteredPin)
            editor.apply()

            Toast.makeText(this, "Pin Created Successfully", Toast.LENGTH_SHORT).show()

            // Launch MainActivity and finish CreatePinActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
