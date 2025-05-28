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
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.securekeep.HomeRCV.RCVModel
import com.example.securekeep.HomeRCV.RvAdapter
import com.example.securekeep.antipocket.AntiPocketActivity
import com.example.securekeep.chargingdetect.ChargeDetectActivity
import com.example.securekeep.databinding.ActivityMainBinding
import com.example.securekeep.earphonedetection.EarphonesActivity
import com.example.securekeep.intruderdetection.IntruderActivity
import com.example.securekeep.intruderdetection.PermissionActivity
import com.example.securekeep.settings.SettingActivity
import com.example.securekeep.touchdetection.TouchPhoneActivity
import com.example.securekeep.wifidetection.WifiActivity

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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_REQUEST_CODE)
            }
        }
    }

    // Handle the result of permission requests
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val deniedPermissions = permissions.filterIndexed { index, _ -> grantResults[index] != PackageManager.PERMISSION_GRANTED }
            if (deniedPermissions.isNotEmpty()) {
                permissionRequestCount++
                if (permissionRequestCount >= 2) {
                    openAppSettings() // Open settings after the user denies permission twice
                } else {
                    Toast.makeText(this, "Notification permission permission is required.", Toast.LENGTH_SHORT).show()
                }
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
        private const val PERMISSION_REQUEST_CODE = 101

        fun checkPermissionsForService(context: Context): Boolean {
            val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            val overlayPermission = Settings.canDrawOverlays(context)

            // Prepare a list of missing permissions
            val missingPermissions = mutableListOf<String>()

            // Check CAMERA permission
            if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.CAMERA)
            }

            // Check for storage permissions based on Android version (highest to lowest)
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                    // Android 15+
                    val hasPartialAccess = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    ) == PackageManager.PERMISSION_GRANTED

                    val hasFullAccess = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasPartialAccess && !hasFullAccess) {
                        missingPermissions.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                        missingPermissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    // Android 13-14
                    val readMediaImagesPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_IMAGES
                    )
                    if (readMediaImagesPermission != PackageManager.PERMISSION_GRANTED) {
                        missingPermissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    // Android 10-12
                    val readStoragePermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    if (readStoragePermission != PackageManager.PERMISSION_GRANTED) {
                        missingPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
                else -> {
                    // Android 9 (Pie) and below
                    val writeStoragePermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    val readStoragePermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )

                    if (writeStoragePermission != PackageManager.PERMISSION_GRANTED) {
                        missingPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    if (readStoragePermission != PackageManager.PERMISSION_GRANTED) {
                        missingPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }

            return missingPermissions.isEmpty() && overlayPermission
        }

        fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
            val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            return manager.getRunningServices(Int.MAX_VALUE)
                .any { it.service.className == serviceClass.name }
        }
    }
}
