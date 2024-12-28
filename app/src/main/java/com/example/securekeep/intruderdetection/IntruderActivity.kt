package com.example.securekeep.intruderdetection

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.MainActivity
import com.example.securekeep.R
import com.example.securekeep.databinding.ActivityIntruderBinding
import com.example.securekeep.intruderdetection.IntruderAdapter.SelfieModel
import com.example.securekeep.intruderdetection.IntruderServices.IntruderTrackingService
import com.example.securekeep.intruderdetection.IntruderServices.MyDeviceAdminReceiver
import com.example.securekeep.settings.AddEmailFragment
import com.example.securekeep.settings.EmailActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IntruderActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityIntruderBinding.inflate(layoutInflater)
    }
    private var attemptThreshold = 2 // Default threshold value
    private var alertStatus = false
    private var isIntruderServiceRunning = false
    private var isEmail = false
    private lateinit var alertDialog: AlertDialog
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var selfieList: MutableList<SelfieModel>
    private var currentSelfieCount = 0


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        selfieList = mutableListOf()
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, MyDeviceAdminReceiver::class.java)
        sharedPreferences = getSharedPreferences("IntruderPrefs", MODE_PRIVATE)

        isIntruderServiceRunning = MainActivity.isServiceRunning(this@IntruderActivity, IntruderTrackingService::class.java)

        // Retrieving selected attempts and alert status
        alertStatus = sharedPreferences.getBoolean("AlertStatus", false)
        isEmail = sharedPreferences.getBoolean("EmailStatus", false)
        attemptThreshold = sharedPreferences.getInt("AttemptThreshold", 2)
        binding.selectedAttempts.text = String.format(Locale.getDefault(), "%d", attemptThreshold)

        loadSelfiesFromStorage()
        binding.picsCount.text = String.format(Locale.getDefault(), "%d", currentSelfieCount)

        binding.backBtn.setOnClickListener {
            startActivity(Intent(this@IntruderActivity, MainActivity::class.java))
            finish()
        }

        updatePowerButton()

        binding.pinAttemptSelector.setOnClickListener { showNumberPickerDialog() }

        alertDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Will Be Activated In 10 Seconds")
            .setMessage("00:10")
            .setBackground(ContextCompat.getDrawable(this, R.drawable.simple_round_boarder))
            .setCancelable(false)
            .create()

        binding.powerBtn.setOnClickListener {
            if (alertStatus && isIntruderServiceRunning) {
                stopBackgroundService()
                alertStatus = false
                isIntruderServiceRunning = false
                Toast.makeText(this, "Intruder Alert Mode Deactivated", Toast.LENGTH_SHORT).show()
                binding.powerBtn.setImageResource(R.drawable.power_on)
                binding.activateText.text = getString(R.string.tap_to_activate)
                //Save the alert status in shared preferences
                val editor = sharedPreferences.edit()
                editor.putBoolean("AlertStatus", alertStatus)
                editor.apply()
            } else {
                // Check if the app is already a device admin
                if (!devicePolicyManager.isAdminActive(compName)) {
                    requestDeviceAdmin()
                } else {
                    alertDialog.apply {
                        show()
                        // Set title text color
                        val titleView = findViewById<TextView>(androidx.appcompat.R.id.alertTitle)
                        titleView?.setTextColor(Color.BLACK)
                        // Set message text color
                        findViewById<TextView>(android.R.id.message)?.setTextColor(Color.BLACK)
                    }
                    alertStatus = true
                    isIntruderServiceRunning = true

                    object : CountDownTimer(10000, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            alertDialog.setMessage("00:${millisUntilFinished / 1000}")
                        }

                        override fun onFinish() {
                            alertDialog.dismiss()
                            Toast.makeText(this@IntruderActivity, "Intruder Alert Mode Activated", Toast.LENGTH_SHORT).show()
                            binding.powerBtn.setImageResource(R.drawable.power_off)
                            binding.activateText.text = getString(R.string.tap_to_deactivate)

                            startBackgroundService()
                            //Save the alert status in shared preferences
                            val editor = sharedPreferences.edit()
                            editor.putBoolean("AlertStatus", alertStatus)
                            editor.apply()
                        }
                    }.start()
                }
            }
        }

        binding.viewIntruderPic.setOnClickListener {
            startActivity(Intent(this@IntruderActivity, IntruderSelfieActivity::class.java))
            finish()
        }

        binding.emailSwitch.setOnClickListener {
            if (!isEmail){
                val bottomSheetDialog: BottomSheetDialogFragment = AddEmailFragment.newInstance(object : AddEmailFragment.OnEmailUpdatedListener {
                    override fun onEmailUpdated(email: String) {
                        if (email.isNotEmpty()) {
                            isEmail = !isEmail
                            binding.emailSwitch.setImageResource(if (isEmail) R.drawable.switch_on else R.drawable.switch_off)

                            updatePowerButton()

                            // Storing email status value in shared preferences
                            val editor = sharedPreferences.edit()
                            editor.putBoolean("EmailStatus", isEmail)
                            editor.apply()

                            Toast.makeText(this@IntruderActivity, "Email Feature Enable", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
                bottomSheetDialog.show(this.supportFragmentManager, "Email")

            }else{
                isEmail = false
                // Storing email status value in shared preferences
                val editor = sharedPreferences.edit()
                editor.putBoolean("EmailStatus", isEmail)
                editor.apply()
                binding.emailSwitch.setImageResource(if (isEmail) R.drawable.switch_on else R.drawable.switch_off)
                updatePowerButton()
                Toast.makeText(this, "Email Feature disable", Toast.LENGTH_SHORT).show()
            }

        }

        binding.setEmailBtn.setOnClickListener {
            startActivity(Intent(this@IntruderActivity, EmailActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        alertStatus = sharedPreferences.getBoolean("AlertStatus", false)
        updatePowerButton()

        loadSelfiesFromStorage()
        binding.picsCount.text = currentSelfieCount.toString()
    }

    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
        intent.putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Enable device admin for additional security features."
        )
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
    }

    private fun updatePowerButton() {
        if (alertStatus && isIntruderServiceRunning) {
            binding.powerBtn.setImageResource(R.drawable.power_off)
            binding.activateText.text = getString(R.string.tap_to_deactivate)
        } else {
            binding.powerBtn.setImageResource(R.drawable.power_on)
            binding.activateText.text = getString(R.string.tap_to_activate)
        }

        binding.emailSwitch.setImageResource(if (isEmail) R.drawable.switch_on else R.drawable.switch_off)
        if (isEmail){
            binding.setEmailBtn.visibility = View.VISIBLE
        }else{
            binding.setEmailBtn.visibility = View.INVISIBLE
        }
    }

    private fun showNumberPickerDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_attempt_picker, null)
        builder.setView(dialogView)

        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroup)
        val dialog = builder.create()

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedRadioButton = dialogView.findViewById<RadioButton>(checkedId)
            val selectedValue = selectedRadioButton.text.toString()

            val applyBtn = dialogView.findViewById<Button>(R.id.attemptApplyBtn)
            applyBtn.setOnClickListener {
                binding.selectedAttempts.text = selectedValue
                attemptThreshold = selectedValue.toInt()

                // Store attempt threshold in shared preferences
                val editor = sharedPreferences.edit()
                editor.putInt("AttemptThreshold", attemptThreshold)
                editor.apply()

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun startBackgroundService() {
            Log.d("IntruderActivity", "startingBackgroundService ")
            isIntruderServiceRunning = true
            startService(Intent(this@IntruderActivity, IntruderTrackingService::class.java))
    }

    private fun stopBackgroundService() {
        isIntruderServiceRunning = false
        stopService(Intent(this@IntruderActivity, IntruderTrackingService::class.java))
    }

    companion object {
        private const val REQUEST_CODE_ENABLE_ADMIN = 1
    }

    private fun loadSelfiesFromStorage() {
        // Clear the existing list to avoid duplicates
        selfieList.clear()

        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        if (storageDir != null && storageDir.exists()) {
            val imageFiles = storageDir.listFiles { file ->
                file.isFile && (file.extension == "jpg" || file.extension == "jpeg" || file.extension == "png")
            }

            val dateFormatter = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault())

            imageFiles?.forEach { file ->
                val imageUri = Uri.fromFile(file)
                val dateTaken = dateFormatter.format(Date(file.lastModified()))
                selfieList.add(SelfieModel(imageUri, dateTaken))
            }

            currentSelfieCount = selfieList.size

        } else {
            Log.e("IntruderActivity", "Storage directory not found or is empty.")
        }

        Log.d("IntruderActivity", "loadSelfiesFromStorage: Loaded ${selfieList.size} images")
    }
}
