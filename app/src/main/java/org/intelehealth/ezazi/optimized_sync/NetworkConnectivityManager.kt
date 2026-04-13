package org.intelehealth.ezazi.optimized_sync

import android.content.Context
import android.net.ConnectivityManager

class NetworkConnectivityManager(private val context: Context) {
    private fun isNetworkAvailable(cm: ConnectivityManager): Boolean {
        return cm.activeNetwork != null
    }

    private fun hasSufficientBandwidth(cm: ConnectivityManager): Boolean {
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        val downloadSpeed = capabilities.linkDownstreamBandwidthKbps
        val uploadSpeed = capabilities.linkUpstreamBandwidthKbps
        return downloadSpeed >= 150 && uploadSpeed >= 100
    }

    fun isNetworkUsable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return isNetworkAvailable(cm) && hasSufficientBandwidth(cm)
    }
}