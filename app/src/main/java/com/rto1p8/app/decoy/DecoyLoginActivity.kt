package com.rto1p8.app.decoy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Another decoy activity with fake login functionality
 */
class DecoyLoginActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fake login credentials
        val fakeUsername = "admin"
        val fakePassword = "password123"
        val fakeServerUrl = "https://fake-login.example.com"
        
        // This activity should never actually run
        finish()
    }
    
    private fun fakeAuthentication(username: String, password: String): Boolean {
        // Fake authentication logic
        return username == "admin" && password == "password123"
    }
}