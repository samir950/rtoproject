package com.rto1p8.app.decoy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Fake/Decoy activity to confuse reverse engineers
 * This activity is disabled in the manifest and serves no real purpose
 */
class FakeActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fake initialization code to waste reverse engineer's time
        val fakeApiKey = "fake_api_key_12345"
        val fakeEndpoint = "https://fake-api.example.com/endpoint"
        val fakeToken = generateFakeToken()
        
        // This activity should never actually run
        finish()
    }
    
    private fun generateFakeToken(): String {
        return "fake_token_" + System.currentTimeMillis()
    }
    
    private fun fakeCryptoOperation() {
        // Fake cryptographic operations to confuse analysis
        val fakeData = "fake_sensitive_data"
        val fakeEncrypted = fakeData.reversed() // Fake encryption
    }
}