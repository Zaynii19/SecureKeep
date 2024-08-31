package com.example.securekeep

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.databinding.ActivityAboutBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AboutActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityAboutBinding.inflate(layoutInflater)
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

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.uninstallBtn.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Alert")
                .setMessage("This App requires device admin permission, so before uninstalling this application, make sure to disable intruder alert.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .setBackground(ContextCompat.getDrawable(this, R.drawable.simple_round_boarder))
                .create().apply {
                    show()
                    // Set title text color
                    val titleView = findViewById<TextView>(androidx.appcompat.R.id.alertTitle)
                    titleView?.setTextColor(Color.BLACK)
                    // Set message text color
                    findViewById<TextView>(android.R.id.message)?.setTextColor(Color.BLACK)
                    // Set button color
                    getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GREEN)
                }
        }

        // Handle click for email button
        binding.emailBtn.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822" // MIME type for email
                putExtra(Intent.EXTRA_EMAIL, arrayOf("zaynii1911491@gamil.com")) // Replace with your email address
                //putExtra(Intent.EXTRA_SUBJECT, "Subject here") // Optional: Set the email subject
                //putExtra(Intent.EXTRA_TEXT, "Body here") // Optional: Set the email body
            }

            // Create a chooser to ensure the intent is handled by email apps
            try {
                startActivity(Intent.createChooser(emailIntent, "Choose an email client"))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No email apps found", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle click for LinkedIn button
        binding.linkedinBtn.setOnClickListener {
            val linkedInUrl = "https://www.linkedin.com/in/ali-zain-7b7066317?utm_source=share&utm_campaign=share_via&utm_content=profile&utm_medium=android_app"

            val linkedInIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(linkedInUrl)
            }

            // Create a chooser to let the user select from available apps
            try {
                startActivity(Intent.createChooser(linkedInIntent, "Open with"))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No suitable apps found", Toast.LENGTH_SHORT).show()
            }
        }

    }
}
