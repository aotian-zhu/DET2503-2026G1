package no.dte2503.volunteer.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(context: Context) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private fun connected() = manager.getNetworkCapabilities(manager.activeNetwork)?.let {
        it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } == true
    private val mutable = MutableStateFlow(connected())
    val online = mutable.asStateFlow()
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { mutable.value = connected() }
        override fun onLost(network: Network) { mutable.value = connected() }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { mutable.value = connected() }
    }
    init { manager.registerDefaultNetworkCallback(callback) }
    fun close() { manager.unregisterNetworkCallback(callback) }
}
