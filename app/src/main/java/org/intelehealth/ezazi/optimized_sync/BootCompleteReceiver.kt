package org.intelehealth.ezazi.optimized_sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val intentAction = intent?.action
        if (intent != null && intentAction.equals("android.intent.action.BOOT_COMPLETED")) {
            context?.let { OptimizedSyncWorker.enqueuePeriodicWork(it) }
        }
    }
}