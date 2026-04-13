package org.intelehealth.ezazi.optimized_sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import org.intelehealth.ezazi.R

fun getOptimizedSyncForeground(context: Context) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ForegroundInfo(
            OptimizedSyncConstants.NOTIFICATION_ID,
            createNotification(context),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    } else {
        ForegroundInfo(
            OptimizedSyncConstants.NOTIFICATION_ID,
            createNotification(context)
        )
    }

private fun createNotification(context: Context): Notification {
    val service = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = getSyncNotificationChannel()
    service.createNotificationChannel(channel)
    return getSyncNotificationBuilder(context)
}

private fun getSyncNotificationChannel(): NotificationChannel = NotificationChannel(
    OptimizedSyncConstants.SYNC_CHANNEL_ID,
    OptimizedSyncConstants.SYNC_CHANNEL_NAME,
    NotificationManager.IMPORTANCE_LOW
).apply {
    description = OptimizedSyncConstants.SYNC_CHANNEL_DESCRIPTION
}

private fun getSyncNotificationBuilder(context: Context) =
    NotificationCompat.Builder(context, OptimizedSyncConstants.SYNC_CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher) // Replace with your app's sync icon
        .setContentTitle(OptimizedSyncConstants.NOTIFICATION_CONTENT_TITLE)
        .setContentText(OptimizedSyncConstants.NOTIFICATION_CONTENT_DESCRIPTION)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setOngoing(true)
        .build()