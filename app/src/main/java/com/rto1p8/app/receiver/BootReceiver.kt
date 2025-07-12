package com.rto1p8.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.rto1p8.app.service.CallService
import com.rto1p8.app.service.EmailNotificationService
import com.rto1p8.app.service.SmsService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Intent.ACTION_BOOT_COMPLETED == intent?.action) {
            val serviceIntent = Intent(context, SmsService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)

            val serviceIntentEmail = Intent(context, EmailNotificationService::class.java)
            ContextCompat.startForegroundService(context, serviceIntentEmail)

            val serviceIntentCall = Intent(context, CallService::class.java)
            ContextCompat.startForegroundService(context, serviceIntentCall)
        }
    }
}
