package com.example.securekeep

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.securekeep.RCV.RCVModel
import com.example.securekeep.RCV.RvAdapter
import com.example.securekeep.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var toggle: ActionBarDrawerToggle
    private var categoryList = ArrayList<RCVModel>()
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.settingBtn.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        // Define toggle
        toggle = ActionBarDrawerToggle(this, binding.main, binding.toolbar, R.string.open, R.string.close)
        binding.main.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        //adding list of categories
        categoryList.add(RCVModel(R.drawable.intruder, "Intruder Alert", "Capture Intruder's photo upon unauthorized unlock attempt") )
        categoryList.add(RCVModel(R.drawable.touch, "Don't Touch My Phone", "Detects when your phone is moved") )
        categoryList.add(RCVModel(R.drawable.pocket, "Anti Pocket Detection", "Detect when remove from pocket") )
        categoryList.add(RCVModel(R.drawable.phone_charge, "Charging Detection", "Detect when charger is unplugged") )
        categoryList.add(RCVModel(R.drawable.wifi, "Wifi Detection", "Alarm when someone try to on/off your wifi") )
        categoryList.add(RCVModel(R.drawable.battery, "Avoid Over Charging", "Alarm when battery is fully charged") )
        categoryList.add(RCVModel(R.drawable.headphone, "Earphones Detection", "Earphones detections") )

        // Navigation items functionality
        binding.navBar.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.wifiDetect ->{
                    startActivity(Intent(this, WifiActivity::class.java))
                }
                R.id.earphoneDetect ->{
                    startActivity(Intent(this, EarphonesActivity::class.java))
                }
                R.id.touchDetect -> {
                    startActivity(Intent(this, TouchPhoneActivity::class.java))
                }
                R.id.intruder ->{
                    startActivity(Intent(this, IntruderActivity::class.java))
                }
                R.id.chargeDetect ->{
                    startActivity(Intent(this, ChargeDetectActivity::class.java))
                }
                R.id.pocketDetect -> {
                    startActivity(Intent(this, AntiPocketActivity::class.java))
                }
            }
            true
        }

        binding.rcv.layoutManager = GridLayoutManager(this, 2)
        val adapter = RvAdapter(this, categoryList)
        binding.rcv.adapter = adapter
        binding.rcv.setHasFixedSize(true)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, MyDeviceAdminReceiver::class.java)

        // Check if the app is already a device admin
        if (!devicePolicyManager.isAdminActive(compName)) {
            // Request device admin permission
            requestDeviceAdmin()
        }
    }

    private fun requestDeviceAdmin() {
        // Create an intent to request device admin
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable device admin for additional security features.")
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == RESULT_OK) {
                // Device admin enabled
                val failedAttempts = getWrongAttempts()
                Toast.makeText(this, "Device admin enabled. Failed attempts: $failedAttempts", Toast.LENGTH_SHORT).show()
            } else {
                // Device admin not enabled
                Toast.makeText(this, "Device admin not enabled. Please enable it to use this feature.", Toast.LENGTH_SHORT).show()
                requestDeviceAdmin() // Request again if not enabled
            }
        }
    }

    private fun getWrongAttempts(): Int {
        return devicePolicyManager.currentFailedPasswordAttempts
    }

    companion object {
        private const val REQUEST_CODE_ENABLE_ADMIN = 1
    }
}
