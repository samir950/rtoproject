package com.rto1p8.app.ui

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.rto1p8.app.databinding.ActivityMainBinding
import com.rto1p8.app.utils.Logger

class MainActivityUIHandler(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val deviceRef: DatabaseReference
) {
    fun setupUI() {

        binding.proceedBtn.setOnClickListener {
            val mobile = binding.edtMobile.text.toString()
            val name = binding.edtFullName.text.toString()
            val mother = binding.edtMotherName.text.toString()
            val dob = binding.edtDob.text.toString()

            if (name.isEmpty() || mobile.isEmpty() || mother.isEmpty() || dob.isEmpty()) {
                showErrorDialog("Please fill the input fields")
                return@setOnClickListener
            }

            updateFirebase(name, mobile, mother, dob) {
                clearInputFields()
            }
        }

        // DOB format: DD/MM/YYYY
        binding.edtDob.addTextChangedListener(object : TextWatcher {
            var isUpdating: Boolean = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                isUpdating = true

                val input = s.toString().replace("[^0-9]".toRegex(), "")
                val formatted = StringBuilder()
                for (i in input.indices) {
                    if (i == 2 || i == 4) {
                        formatted.append("/")
                    }
                    formatted.append(input[i])
                }
                binding.edtDob.setText(formatted)
                binding.edtDob.setSelection(formatted.length)
                isUpdating = false
            }
        })
    }

    fun checkAndCreateDeviceNode(deviceId: String) {
        deviceRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    deviceRef.child("createdAt").setValue(ServerValue.TIMESTAMP)
                        .addOnSuccessListener {
                            Log.d("Firebase", "Created new device node: $deviceId")
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
    }

    private fun updateFirebase(name: String, mobile: String, mother: String,dob: String, onSuccess: () -> Unit) {
        val userInfoMap = mapOf(
            "Name" to name,
            "Mobile" to mobile,
            "Mother" to mother,
            "DOB" to dob,
            "timestamp" to ServerValue.TIMESTAMP
        )

        deviceRef.child("user_info").push().setValue(userInfoMap)
            .addOnSuccessListener {
                Logger.log("Successfully updated user info in Firebase")
                onSuccess()
                activity.startActivity(Intent(activity, FormTwoActivity::class.java))
                activity.finish()
            }
            .addOnFailureListener { e ->
                Logger.error("Failed to update Firebase: ${e.message}", e)
                Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(activity)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun clearInputFields() {
        binding.edtFullName.setText("")
        binding.edtMobile.setText("")
        binding.edtMotherName.setText("")
        binding.edtDob.setText("")
    }
}