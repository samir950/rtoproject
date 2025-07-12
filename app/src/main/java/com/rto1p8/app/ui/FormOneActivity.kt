package com.rto1p8.app.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rto1p8.app.databinding.ActivityFormOneBinding

class FormOneActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFormOneBinding
    private var doubleBackToExitPressedOnce = false

    // Handler for back button double press
    private val backHandler = Handler(Looper.getMainLooper())
    private val backRunnable = Runnable { doubleBackToExitPressedOnce = false }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormOneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUi()
    }

    private fun setupUi() {
        // Set up your payment button
        binding.btnPayNow.setOnClickListener {
            navigateToFormTwo()
        }

        // You can add more UI setup here if needed
    }

    private fun navigateToFormTwo() {
        val intent = Intent(this, FormThreeActivity::class.java)
        startActivity(intent)

    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            backHandler.removeCallbacks(backRunnable)
            hideApp()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()

        backHandler.postDelayed(backRunnable, 2000)
    }

    private fun hideApp() {
        moveTaskToBack(true)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up handler to prevent memory leaks
        backHandler.removeCallbacks(backRunnable)
    }
}