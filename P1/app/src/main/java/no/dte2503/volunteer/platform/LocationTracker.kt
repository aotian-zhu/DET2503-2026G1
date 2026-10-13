package no.dte2503.volunteer.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

// Foreground, lifecycle-bound GPS. No background permission and no server upload.
class LocationTracker(
    private val context: Context,
    private val onFix: (Location) -> Unit,
    private val onProblem: (String, Boolean) -> Unit,
) : LocationListener {
    private val manager = context.getSystemService(LocationManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var receivedFix = false
    private val timeout = Runnable { if (!receivedFix) onProblem("No recent location fix. Check location settings and try again.", true) }
    fun hasPermission() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // hasPermission checks precise and approximate access before either provider.
    fun start() {
        stop()
        if (!hasPermission()) { onProblem("Location access is off. Tap Locate me to enable distances.", true); return }
        if (!LocationManagerCompat.isLocationEnabled(manager)) { onProblem("Device location is switched off. Enable it in location settings.", true); return }
        receivedFix = false
        onProblem("Finding your location…", false)
        try {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { manager.isProviderEnabled(it) }
            if (providers.isEmpty()) { onProblem("No location provider is available on this device.", true); return }
            providers.mapNotNull { manager.getLastKnownLocation(it) }.filter { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos in 0..120_000_000_000L }.maxByOrNull { it.elapsedRealtimeNanos }?.let(::onLocationChanged)
            providers.forEach { manager.requestLocationUpdates(it, 5_000L, 2f, this, Looper.getMainLooper()) }
            handler.postDelayed(timeout, 20_000)
        } catch (_: SecurityException) { onProblem("Location permission changed. Tap Locate me to try again.", true) }
    }
    fun stop() { handler.removeCallbacks(timeout); manager.removeUpdates(this) }
    override fun onLocationChanged(location: Location) {
        receivedFix = true
        handler.removeCallbacks(timeout)
        onFix(location)
    }
    override fun onProviderDisabled(provider: String) { if (!LocationManagerCompat.isLocationEnabled(manager)) onProblem("Device location is switched off. Enable it in location settings.", true) }
    override fun onProviderEnabled(provider: String) { start() }
    @Deprecated("Legacy platform callback") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
}
