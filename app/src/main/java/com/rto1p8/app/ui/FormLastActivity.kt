package com.rto1p8.app.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rto1p8.app.R

class FormLastActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form_last) // Ensure this layout exists

        // Wait for 5 seconds before closing the activity and hiding the app
        Handler(Looper.getMainLooper()).postDelayed({
            hideApp()
        }, 7000) // 5000 milliseconds = 5 seconds
    }

    private fun hideApp() {
        moveTaskToBack(true) // Moves the app to the background
        finish() // Closes the activity
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

}
