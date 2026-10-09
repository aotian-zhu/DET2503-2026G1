package no.dte2503.volunteer.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.preference.PreferenceManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import no.dte2503.volunteer.MainViewModel
import no.dte2503.volunteer.data.GeoPointData
import no.dte2503.volunteer.data.TaskWithDistance
import no.dte2503.volunteer.data.VolunteerTask
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(
    viewModel: MainViewModel,
    tasks: List<VolunteerTask>,
    position: GeoPointData?,
    denied: Boolean,
    locationError: String?,
    locateRequestId: Int,
    selectedTaskId: String?,
) {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(isNetworkAvailable(context)) }
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    DisposableEffect(context) {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isOnline = true }
            override fun onLost(network: Network) { isOnline = isNetworkAvailable(context) }
        }
        connectivity.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), callback)
        onDispose { connectivity.unregisterNetworkCallback(callback) }
    }
    // Screen 负责 Android 权限与定位 API；ViewModel 只接收平台无关的坐标和拒绝状态。
    fun loadLocation() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return
        fun applyLocation(location: Location?) {
            if (location != null) {
                viewModel.setCurrentPosition(location.latitude, location.longitude)
            } else viewModel.setLocationError("Current location is unavailable. Please set a location in the emulator controls.")
        }
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                if (location != null) applyLocation(location)
                else fusedClient.lastLocation.addOnSuccessListener { fallback ->
                    if (fallback != null) applyLocation(fallback) else {
                        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        val managerFallback = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
                            .maxByOrNull { it.time }
                        applyLocation(managerFallback)
                    }
                }
            }
            .addOnFailureListener { viewModel.setLocationError("Could not get your location. Please try again.") }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result.values.any { it }
        viewModel.setLocationPermissionDenied(!granted)
        if (granted) loadLocation()
    }
    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) loadLocation()
    }

    val taskItems = viewModel.tasksWithDistance(position, tasks)
    val listState = rememberLazyListState()
    LaunchedEffect(selectedTaskId, taskItems.map { it.task.id }) {
        val selectedIndex = taskItems.indexOfFirst { it.task.id == selectedTaskId }
        if (selectedIndex >= 0) listState.animateScrollToItem(selectedIndex + 1)
    }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(390.dp)) {
            OsmMap(viewModel, taskItems, position, locateRequestId, selectedTaskId, isOnline)
            Button(
                onClick = {
                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (fine || coarse) loadLocation() else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            ) {
                Icon(Icons.Rounded.MyLocation, null)
                Text(" Locate me", modifier = Modifier.padding(start = 4.dp))
            }
        }
        if (!isOnline) {
            Text("Offline: showing previously cached map tiles.", modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (denied || locationError != null) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOff, null, tint = MaterialTheme.colorScheme.error)
                Text(locationError ?: "Location permission is off. Distances cannot be calculated.", modifier = Modifier.padding(start = 8.dp))
            }
        }
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(Modifier.padding(vertical = 10.dp)) {
                    Text("Tasks on map", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Select a task to focus its exact meeting point. Area and floor describe the task point inside the venue.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(taskItems.size, key = { taskItems[it].task.id }) { index ->
                val item = taskItems[index]
                val task = item.task
                val location = viewModel.locations.firstOrNull { it.id == task.locationId }
                val isSelected = selectedTaskId == task.id
                Card(
                    onClick = { viewModel.selectTask(task.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    ),
                    border = BorderStroke(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Place,
                            null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        )
                        Column(Modifier.padding(start = 10.dp).weight(1f)) {
                            Text(
                                task.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                location?.name ?: "Location unavailable",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                listOfNotNull(task.place.area, task.place.floor).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            item.distanceMetres?.let(::formatDistance) ?: "—",
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OsmMap(
    viewModel: MainViewModel,
    taskItems: List<TaskWithDistance>,
    position: GeoPointData?,
    locateRequestId: Int,
    selectedTaskId: String?,
    isOnline: Boolean,
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val markerColor = MaterialTheme.colorScheme.primaryContainer.toArgb()
    val markerBorderColor = MaterialTheme.colorScheme.primary.toArgb()
    var map by remember { mutableStateOf<MapView?>(null) }
    var lastFocusedTaskId by remember { mutableStateOf<String?>(null) }
    var lastFocusedLocateId by remember { mutableStateOf(0) }
    var lastOverlaySignature by remember { mutableStateOf<String?>(null) }
    val overlaySignature = buildString {
        append(selectedTaskId)
        append('|')
        append(position)
        taskItems.forEach { append('|').append(it.task.id).append(':').append(it.distanceMetres?.toInt()) }
    }
    // MapView 有独立生命周期，离开 Compose 时必须释放其瓦片与监听资源。
    DisposableEffect(Unit) { onDispose { map?.onDetach() } }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
            Configuration.getInstance().userAgentValue = context.packageName
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(14.5)
                controller.setCenter(GeoPoint(59.9110, 10.7440))
                map = this
            }
        },
        update = { mapView ->
            mapView.setUseDataConnection(isOnline)
            if (overlaySignature != lastOverlaySignature) {
                mapView.overlays.clear()
                taskItems.forEach { item ->
                val task = item.task
                val location = viewModel.locations.firstOrNull { it.id == task.locationId }
                val isSelected = task.id == selectedTaskId
                val markerDot = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (isSelected) markerBorderColor else markerColor)
                    setStroke(if (isSelected) 7 else 5, 0xFFFFFFFF.toInt())
                    setSize(if (isSelected) 48 else 38, if (isSelected) 48 else 38)
                }
                val marker = Marker(mapView).apply {
                    this.position = GeoPoint(task.place.position.latitude, task.place.position.longitude)
                    title = task.title
                    snippet = listOfNotNull(
                        location?.name ?: "Location unavailable",
                        task.place.area,
                        task.place.floor,
                        item.distanceMetres?.let(::formatDistance) ?: "Distance unavailable",
                    ).joinToString(" • ")
                    icon = markerDot
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { clickedMarker, _ ->
                        viewModel.selectTask(task.id)
                        clickedMarker.showInfoWindow()
                        true
                    }
                }
                mapView.overlays.add(marker)
                if (isSelected && selectedTaskId != lastFocusedTaskId) {
                    mapView.controller.setZoom(18.8)
                    mapView.controller.animateTo(marker.position)
                    marker.showInfoWindow()
                    lastFocusedTaskId = selectedTaskId
                }
            }
            if (selectedTaskId == null) lastFocusedTaskId = null
            position?.let { current ->
                val dot = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(primaryColor)
                    setStroke(6, 0xFFFFFFFF.toInt())
                    setSize(42, 42)
                }
                val currentPoint = GeoPoint(current.latitude, current.longitude)
                mapView.overlays.add(Marker(mapView).apply {
                    this.position = currentPoint
                    title = "Your location"
                    icon = dot
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                })
                if (locateRequestId > lastFocusedLocateId) {
                    mapView.controller.setZoom(17.5)
                    mapView.controller.setCenter(currentPoint)
                    lastFocusedLocateId = locateRequestId
                }
            }
                lastOverlaySignature = overlaySignature
                mapView.invalidate()
            }
        },
    )
}

private fun isNetworkAvailable(context: Context): Boolean {
    val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivity.activeNetwork ?: return false
    return connectivity.getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}

private fun formatDistance(metres: Float): String =
    if (metres < 1000) "${metres.toInt()} m" else String.format("%.1f km", metres / 1000)
