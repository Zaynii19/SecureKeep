package com.example.securekeep.intruderdetection

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.securekeep.R
import com.example.securekeep.databinding.ActivityFullPictureBinding

class FullPictureActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityFullPictureBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        intent = intent
        val imageUriString = intent.getStringExtra("SelfieUri")
        val imageUri = imageUriString?.let { Uri.parse(it) } // Convert String back to Uri

        Log.d("FullSelfieActivity", "onCreate: ImageUri: $imageUri")


        Glide.with(this)
            .load(imageUri)
            .apply(RequestOptions().placeholder(R.drawable.camera))
            .into(binding.fullSelfie)
    }
}