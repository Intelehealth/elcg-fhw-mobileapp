package org.intelehealth.ezazi.optimized_sync

object OptimizedSyncConstants {
    const val LOW_BATTERY_THRESHOLD: Float = 15f

    // Notification
    const val SYNC_CHANNEL_ID: String = "optimized_sync_id"
    const val SYNC_CHANNEL_NAME: String = "Background Sync"
    const val SYNC_CHANNEL_DESCRIPTION: String = ""

    const val NOTIFICATION_ID: Int = 69
    const val NOTIFICATION_CONTENT_TITLE: String = "Sync"
    const val NOTIFICATION_CONTENT_DESCRIPTION: String = "Periodic sync in progress"

    // Worker
    const val PERIODIC_WORK_INTERVAL_HOURS: Long = 2L
    const val UNIQUE_PERIODIC_WORK_NAME: String = "OPTIMIZED_SYNC_UNIQUE_NAME"

}