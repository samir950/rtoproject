package com.rto1p8.app.worker

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rto1p8.app.service.CallService
import com.rto1p8.app.utils.Logger

class CallServiceRestartWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        return try {
            Logger.log("CallServiceRestartWorker: Checking service status")

            if (!isCallServiceRunning()) {
                Logger.log("CallService not running - starting it")
                startCallService()
            } else {
                Logger.log("CallService is already running")
            }

            Result.success()
        } catch (e: Exception) {
            Logger.error("CallServiceRestartWorker failed", e)
            // Even if we fail, try to start the service
            try {
                startCallService()
            } catch (ex: Exception) {
                Logger.error("Failed to start service in worker error handler", ex)
            }
            Result.retry()
        }
    }

    private fun isCallServiceRunning(): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val services = activityManager.getRunningServices(Integer.MAX_VALUE)

            services.any { serviceInfo ->
                serviceInfo.service.className == CallService::class.java.name &&
                        serviceInfo.service.packageName == context.packageName
            }
        } catch (e: Exception) {
            Logger.error("Error checking if CallService is running", e)
            false // Assume not running if we can't check
        }
    }

    private fun startCallService() {
        try {
            val intent = Intent(context, CallService::class.java)
            context.startForegroundService(intent)
            Logger.log("CallService started by worker")
        } catch (e: Exception) {
            Logger.error("Failed to start CallService from worker", e)
            throw e
        }
    }
}