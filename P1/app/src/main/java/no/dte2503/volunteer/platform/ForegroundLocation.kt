package no.dte2503.volunteer.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import no.dte2503.volunteer.AppUiState
import no.dte2503.volunteer.MainViewModel

fun hasLocationAccess(context: Context) = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

// One subscription across the signed-in app, stopped as soon as the activity pauses.
@Composable
fun ForegroundLocation(model: MainViewModel, state: AppUiState) {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current
    val tracker = remember(model, context) {
        LocationTracker(context, { model.setCurrentPosition(it.latitude, it.longitude, it.accuracy) }, model::setLocationMessage)
    }
    DisposableEffect(tracker, lifecycle, state.locationIsPreview, state.locationRefreshToken) {
        fun start() {
            if (state.locationIsPreview) return
            if (tracker.hasPermission()) tracker.start()
            else model.setLocationMessage(model.uiState.value.locationMessage?.takeIf { it.contains("denied") }
                ?: "Tap Locate me on Map to see your position and distances.", true)
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE -> { tracker.stop(); model.setLocationMessage("Location updates resume when you return to the app.", true) }
                else -> Unit
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        if (lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
        onDispose { tracker.stop(); lifecycle.lifecycle.removeObserver(observer) }
    }
}
