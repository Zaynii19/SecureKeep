package com.example.securekeep

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.securekeep.antiPocket.AntiPocketActivity
import com.example.securekeep.chargingDetect.ChargeDetectActivity
import com.example.securekeep.databinding.ActivityMainBinding
import com.example.securekeep.earphoneDetection.EarphonesActivity
import com.example.securekeep.homeRCV.RCVModel
import com.example.securekeep.homeRCV.RvAdapter
import com.example.securekeep.intruderDetection.IntruderActivity
import com.example.securekeep.intruderDetection.PermissionActivity
import com.example.securekeep.settings.SettingActivity
import com.example.securekeep.touchDetection.TouchPhoneActivity
import com.example.securekeep.wifiDetection.WifiActivity

class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var toggle: ActionBarDrawerToggle
    private var categoryList = ArrayList<RCVModel>()
    private var permissionRequestCount = 0  // Tracks permission request count

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        requestNotificationPermission() // Request notification permission when activity starts
        initializeCategoryList()
    }

    private fun setupUI() {
        binding.settingBtn.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
            binding.main.closeDrawer(GravityCompat.START) // Close the drawer after starting the activity
        }

        toggle = ActionBarDrawerToggle(this, binding.main, binding.toolbar, R.string.open, R.string.close)
        binding.main.addDrawerListener(toggle)
        toggle.drawerArrowDrawable.color = ContextCompat.getColor(this, android.R.color.white)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        binding.navBar.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.wifiDetect -> {
                    startActivity(Intent(this, WifiActivity::class.java))
                    binding.main.closeDrawer(GravityCompat.START) // Close the drawer
                }
                R.id.earphoneDetect -> {
                    startActivity(Intent(this, EarphonesActivity::class.java))
                    binding.main.closeDrawer(GravityCompat.START) // Close the drawer
                }
                R.id.touchDetect -> {
                    startActivity(Intent(this, TouchPhoneActivity::class.java))
                    binding.main.closeDrawer(GravityCompat.START) // Close the drawer
                }
                R.id.intruder -> {
                    if (checkPermissionsForService(this)) {
                        startActivity(Intent(this, IntruderActivity::class.java))
                        binding.main.closeDrawer(GravityCompat.START) // Close the drawer
                    } else {
                        startActivity(Intent(this, PermissionActivity::class.java))
                        binding.main.closeDrawer(GravityCompat.START) // Close the drawer
                    }
                }
                R.id.chargeDetect -> {
                    startActivity(Intent(this, ChargeDetectActivity::class.java))
                    binding.main.closeDrawer(GravityCompat.START) // Close the drawer
                }
                R.id.pocketDetect -> {
                    startActivity(Intent(this, AntiPocketActivity::class.java))
                    binding.main.closeDrawer(GravityCompat.START) // Close the drawer
                }
                R.id.about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                    binding.main.closeDrawer(GravityCompat.START) // Close the drawer
                }
            }
            true
        }

        binding.rcv.layoutManager = GridLayoutManager(this, 2)
        val adapter = RvAdapter(this, categoryList)
        binding.rcv.adapter = adapter
        binding.rcv.setHasFixedSize(true)
    }

    private fun initializeCategoryList() {
        categoryList.add(RCVModel(R.drawable.intruder, "Intruder Alert", "Capture Intruder's photo upon unauthorized unlock attempt"))
        categoryList.add(RCVModel(R.drawable.touch, "Don't Touch My Phone", "Detects when your phone is moved"))
        categoryList.add(RCVModel(R.drawable.pocket, "Anti Pocket Detection", "Detect when remove from pocket"))
        categoryList.add(RCVModel(R.drawable.phone_charge, "Charging Detection", "Detect when charger is unplugged"))
        categoryList.add(RCVModel(R.drawable.wifi, "Wifi Detection", "Alarm when someone try to on/off your wifi"))
        categoryList.add(RCVModel(R.drawable.battery, "Avoid Over Charging", "Alarm when battery is fully charged"))
        categoryList.add(RCVModel(R.drawable.headphone, "Earphones Detection", "Earphones detections"))
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            permissionRequestCount++
            if (permissionRequestCount >= 2) {
                openAppSettings() // Open settings after the user denies permission twice
            } else {
                Toast.makeText(this, "Notification permission is required.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    companion object {
        fun checkPermissionsForService(context: Context): Boolean { 
            val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            val overlayPermission = Settings.canDrawOverlays(context)

            return cameraPermission == PackageManager.PERMISSION_GRANTED && overlayPermission
        }

        @Suppress("DEPRECATION")
        fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
            val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            return manager.getRunningServices(Int.MAX_VALUE)
                .any { it.service.className == serviceClass.name }
        }
    }
}