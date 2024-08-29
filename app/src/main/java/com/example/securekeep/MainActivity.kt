package com.example.securekeep

import android.Manifest
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.securekeep.RCV.RCVModel
import com.example.securekeep.RCV.RvAdapter
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
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
            arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            )
    }
    private val permissionsRequestCode = 1001

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
        requestNecessaryPermissions()  // Request only necessary permissions
        initializeCategoryList()
    }

    private fun setupUI() {
        binding.settingBtn.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }

        toggle = ActionBarDrawerToggle(this, binding.main, binding.toolbar, R.string.open, R.string.close)
        binding.main.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        binding.navBar.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.wifiDetect -> startActivity(Intent(this, WifiActivity::class.java))
                R.id.earphoneDetect -> startActivity(Intent(this, EarphonesActivity::class.java))
                R.id.touchDetect -> startActivity(Intent(this, TouchPhoneActivity::class.java))
                R.id.intruder ->
                    if (checkPermissionsForService(this)) {
                        startActivity(Intent(this, IntruderActivity::class.java))
                    }
                    else{
                        startActivity(Intent(this, PermissionActivity::class.java))
                    }
                R.id.chargeDetect -> startActivity(Intent(this, ChargeDetectActivity::class.java))
                R.id.pocketDetect -> startActivity(Intent(this, AntiPocketActivity::class.java))
            }
            true
        }

        binding.rcv.layoutManager = GridLayoutManager(this, 2)
        val adapter = RvAdapter(this, categoryList)
        binding.rcv.adapter = adapter
        binding.rcv.setHasFixedSize(true)
    }

    private fun requestNecessaryPermissions() {
        val permissionsToRequest = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, permissionsRequestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsRequestCode) {
            val permissionsGranted = grantResults.isNotEmpty() && grantResults.all { result -> result == PackageManager.PERMISSION_GRANTED }

            if (permissionsGranted) {
                Toast.makeText(this, "All requested permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Some permissions were denied", Toast.LENGTH_SHORT).show()
                // Optionally, guide the user to app settings
                openAppSettings()
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
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

    companion object {
        fun checkPermissionsForService(context: Context): Boolean {
            // Android 14 and Above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                val readStoragePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                val notiPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)

                return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                        readStoragePermission == PackageManager.PERMISSION_GRANTED &&
                        notiPermission == PackageManager.PERMISSION_GRANTED
            }
            // Android 10 and less
            else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
                val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                val readStoragePermission =    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                val writeStoragePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)

                return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                        writeStoragePermission == PackageManager.PERMISSION_GRANTED &&
                        readStoragePermission == PackageManager.PERMISSION_GRANTED
            }
            // Android 12 and less
            else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                val readStoragePermission =    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)

                return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                        readStoragePermission == PackageManager.PERMISSION_GRANTED
            }
            // Android 13
            else{
                val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                val readStoragePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)

                return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                        readStoragePermission == PackageManager.PERMISSION_GRANTED
            }
        }
    }
}

