package com.example.securekeep.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.securekeep.databinding.FragmentAddEmailBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddEmailFragment : BottomSheetDialogFragment() {
    private val binding by lazy {
        FragmentAddEmailBinding.inflate(layoutInflater)
    }
    private var onEmailUpdatedListener: OnEmailUpdatedListener? = null

    interface OnEmailUpdatedListener {
        fun onEmailUpdated(email: String)
    }

    companion object {
        fun newInstance(listener: OnEmailUpdatedListener): AddEmailFragment {
            val fragment = AddEmailFragment()
            fragment.onEmailUpdatedListener = listener
            return fragment
        }
    }

    private var userEmail = ""
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = requireActivity().getSharedPreferences("IntruderPrefs", Context.MODE_PRIVATE)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding.saveBtn.setOnClickListener {
            if (binding.email.text.isNullOrEmpty() ||
                !binding.email.text!!.contains("@", false) ||
                !binding.email.text!!.contains(".com", false)){
                Toast.makeText(requireContext(), "Enter Valid Email", Toast.LENGTH_SHORT).show()
            }else{
                userEmail = binding.email.text.toString()
                //Save the User Email in shared preferences
                val editor = sharedPreferences.edit()
                editor.putString("UserEmail", userEmail)
                editor.apply()
                Toast.makeText(requireContext(), "Email Saved Successfully", Toast.LENGTH_SHORT).show()
                onEmailUpdatedListener?.onEmailUpdated(userEmail)
                dismiss()
            }
        }

        // Inflate the layout for this fragment
        return binding.root
    }

}