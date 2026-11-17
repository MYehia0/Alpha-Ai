package com.example.alpha_ai.core.network.internal

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.alpha_ai.core.network.ConnectivityChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Online means the active network has internet access that the system has validated, so a
 * Wi-Fi network with no route to the internet counts as offline.
 */
internal class AndroidConnectivityChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) : ConnectivityChecker {

    override fun isOnline(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
