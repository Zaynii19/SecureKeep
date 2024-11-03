package com.example.securekeep.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securekeep.R
import com.example.securekeep.databinding.ActivityEmailBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EmailActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityEmailBinding.inflate(layoutInflater)
    }
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("IntruderPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("UserEmail", null)

        if (userEmail.isNullOrEmpty()){
            binding.emailStatus.text = getString(R.string.add_email_account)
        }else{
            binding.emailStatus.text = getString(R.string.update_email_account)
        }

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.addEmail.setOnClickListener {
            val bottomSheetDialog: BottomSheetDialogFragment = AddEmailFragment.newInstance(object : AddEmailFragment.OnEmailUpdatedListener {
                override fun onEmailUpdated(email: String) {
                    binding.currentEmail.text = email
                }
            })
            bottomSheetDialog.show(this.supportFragmentManager, "Email")
        }

        binding.currentEmail.text = userEmail
    }

}