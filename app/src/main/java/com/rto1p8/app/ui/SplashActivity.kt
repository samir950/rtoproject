package com.rto1p8.app.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.rto1p8.app.MainActivity
import com.rto1p8.app.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({ //Do something after 100ms
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }, 3000)
    }
}