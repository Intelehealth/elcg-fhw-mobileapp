package org.intelehealth.ezazi.optimized_sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

class NetworkConnectivityManager(private val context: Context) {
    private fun isNetworkAvailable(cm: ConnectivityManager): Boolean {
        return cm.activeNetwork != null
    }

    private fun isNetworkUsable(cm: ConnectivityManager): Boolean {
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val isTransportWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isTransportCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val isGoodTransport = isTransportWifi || isTransportCellular

        var isQualityMet = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val isNotCongested = capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED
            )
            val isNotSuspended = capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED
            )
            isQualityMet = isNotCongested && isNotSuspended
        }

        return hasInternet && isValidated && isGoodTransport && isQualityMet
    }

    fun isNetworkUsable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return isNetworkAvailable(cm) && isNetworkUsable(cm)
    }
}