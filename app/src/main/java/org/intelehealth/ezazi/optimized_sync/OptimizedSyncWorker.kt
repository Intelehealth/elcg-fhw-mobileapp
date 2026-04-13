package org.intelehealth.ezazi.optimized_sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.intelehealth.ezazi.syncModule.SyncUtils
import org.intelehealth.ezazi.utilities.SessionManager
import java.lang.Exception
import java.util.concurrent.TimeUnit

class OptimizedSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val networkManager = NetworkConnectivityManager(context = context)
        val powerState = PowerStateProvider(context = context)

        if (!networkManager.isNetworkUsable()) {
            return Result.failure()
        }

        if (!powerState.isPowerRequirementMet()) {
            return Result.failure()
        }

        try {
            setForegroundAsync(foregroundInfo).get()
            val isSyncSuccessful = SyncUtils().periodicSync()
            return if (isSyncSuccessful) Result.success() else Result.retry()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    override fun getForegroundInfo(): ForegroundInfo = getOptimizedSyncForeground(context)

    companion object {
        private fun buildSyncWorkRequest() = PeriodicWorkRequestBuilder<OptimizedSyncWorker>(
            OptimizedSyncConstants.PERIODIC_WORK_INTERVAL_HOURS,
            TimeUnit.HOURS
        ).build()

        @JvmStatic
        fun enqueuePeriodicWork(context: Context) {
            if (SessionManager(context).isSetupComplete) {
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    OptimizedSyncConstants.UNIQUE_PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    buildSyncWorkRequest()
                )
            }
        }

        @JvmStatic
        fun cancelPeriodicWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(
                OptimizedSyncConstants.UNIQUE_PERIODIC_WORK_NAME
            )
        }
    }
}