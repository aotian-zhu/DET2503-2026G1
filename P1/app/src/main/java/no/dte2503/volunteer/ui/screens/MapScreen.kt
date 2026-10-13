package no.dte2503.volunteer.ui.screens

import android.Manifest
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import kotlin.math.roundToInt
import no.dte2503.volunteer.MainViewModel
import no.dte2503.volunteer.AppUiState
import no.dte2503.volunteer.data.*
import no.dte2503.volunteer.platform.LocationTracker
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(model: MainViewModel) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var facilities by rememberSaveable { mutableStateOf(false) }
    var focusUserToken by remember { mutableIntStateOf(0) }
    val tracker = remember(model, context) { LocationTracker(context, { fix -> model.setCurrentPosition(fix.latitude, fix.longitude, fix.accuracy) }, model::setLocationMessage) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) { tracker.start(); focusUserToken++ }
        else model.setLocationMessage("Location access was denied. Your task markers are still available.", true)
    }
    DisposableEffect(tracker, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (tracker.hasPermission()) tracker.start()
                else model.setLocationMessage(model.uiState.value.locationMessage?.takeIf { it.contains("denied") } ?: "Tap Locate me to see your position and distances.", true)
            }
            if (event == Lifecycle.Event.ON_PAUSE) tracker.stop()
        }
        lifecycle.lifecycle.addObserver(observer)
        if (lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            if (tracker.hasPermission()) tracker.start() else model.setLocationMessage("Tap Locate me to see your position and distances.", true)
        }
        onDispose { tracker.stop(); lifecycle.lifecycle.removeObserver(observer) }
    }
    val taskItems = tasksByDistance(state.tasks, state.currentPosition)
    val locationItems = state.locations.map { LocationWithDistance(it, state.currentPosition?.let { position -> distanceMetres(position, it.position) }) }.sortedBy { it.distanceMetres ?: Float.MAX_VALUE }
    val list = rememberLazyListState()
    LaunchedEffect(state.selectedTaskId) { if (state.selectedTaskId != null) facilities = false }
    LaunchedEffect(state.selectedTaskId, state.selectedLocationId, facilities, taskItems.map { it.task.id }, locationItems.map { it.location.id }) {
        val index = if (facilities) locationItems.indexOfFirst { it.location.id == state.selectedLocationId } else taskItems.indexOfFirst { it.task.id == state.selectedTaskId }
        if (index >= 0) list.animateScrollToItem(index + 1) else list.scrollToItem(0)
    }
    BoxWithConstraints(Modifier.fillMaxSize().testTag("map_screen")) {
        val mapHeight = (maxHeight * .48f).coerceIn(180.dp, 380.dp)
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = !facilities, onClick = { facilities = false }, label = { Text("Tasks") })
                FilterChip(selected = facilities, onClick = { facilities = true }, label = { Text("Facilities") }, modifier = Modifier.testTag("facilities_filter"))
                Spacer(Modifier.weight(1f))
                Text("● You", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            Box(Modifier.fillMaxWidth().height(mapHeight).clip(RoundedCornerShape(22.dp))) {
                OsmMap(model, state, facilities, focusUserToken)
                FilledTonalButton(onClick = {
                    if (tracker.hasPermission()) { tracker.start(); focusUserToken++; model.selectTask(null) }
                    else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).testTag("locate_me")) { Icon(Icons.Rounded.MyLocation, null, Modifier.size(18.dp)); Text(" Locate me") }
                Surface(Modifier.align(Alignment.BottomStart), color = MaterialTheme.colorScheme.surface.copy(alpha = .94f)) {
                    Text("© OpenStreetMap contributors", style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/copyright"))) }.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            state.locationMessage?.let { message ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (message.contains("settings") || message.contains("denied")) TextButton(onClick = {
                        val intent = if (message.contains("denied") || message.contains("permission")) Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")) else Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        context.startActivity(intent)
                    }, contentPadding = PaddingValues(4.dp)) { Text("Settings", style = MaterialTheme.typography.labelSmall) }
                }
            }
            if (state.currentPosition != null) Text("GPS accuracy ≈ ${state.accuracyMetres?.roundToInt() ?: 0} m · Distances are straight-line", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!state.isOnline) Text("Offline · Visited map tiles may be cached. Markers and GPS still work.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            LazyColumn(Modifier.weight(1f), state = list, contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text(if (facilities) "Nearby facilities" else if (state.currentPosition != null) "Tasks · Nearest first" else "Your task points", style = MaterialTheme.typography.titleMedium) }
                if (facilities) items(locationItems, key = { it.location.id }) { item ->
                    val location = item.location
                    InfoCard(highlighted = state.selectedLocationId == location.id, onClick = { model.selectLocation(location.id) }, modifier = Modifier.testTag("facility_${location.id}")) {
                        Text(location.name, style = MaterialTheme.typography.titleMedium)
                        Text(location.description, style = MaterialTheme.typography.bodyMedium)
                        item.distanceMetres?.let { Text("${formatDistance(it)} away", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary) }
                    }
                } else items(taskItems, key = { it.task.id }) { item ->
                    val task = item.task
                    InfoCard(highlighted = state.selectedTaskId == task.id, onClick = { model.selectTask(task.id) }, modifier = Modifier.testTag("map_task_${task.id}")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Place, null, tint = MaterialTheme.colorScheme.primary)
                            Text(task.title, Modifier.padding(start = 8.dp).weight(1f), style = MaterialTheme.typography.titleMedium)
                            item.distanceMetres?.let { Text(formatDistance(it), style = MaterialTheme.typography.labelMedium) }
                        }
                        Text(listOfNotNull(state.locations.firstOrNull { it.id == task.locationId }?.name, task.place.area, task.place.floor).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StatusPill(task.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun OsmMap(model: MainViewModel, state: AppUiState, facilities: Boolean, focusUserToken: Int) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var map by remember { mutableStateOf<MapView?>(null) }
    var lastFocus by remember { mutableStateOf<String?>(null) }
    var lastUserToken by remember { mutableIntStateOf(0) }
    val density = context.resources.displayMetrics.density
    val taskColor = MaterialTheme.colorScheme.primary.toArgb()
    val facilityColor = MaterialTheme.colorScheme.secondary.toArgb()
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) map?.onResume() else if (event == Lifecycle.Event.ON_PAUSE) map?.onPause() }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer); map?.onPause(); map?.onDetach() }
    }
    AndroidView(modifier = Modifier.fillMaxSize(), factory = {
        Configuration.getInstance().apply {
            userAgentValue = "${context.packageName}/0.5"
            osmdroidBasePath = File(context.filesDir, "osm")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
            tileFileSystemCacheMaxBytes = 64L * 1024 * 1024
            tileFileSystemCacheTrimBytes = 48L * 1024 * 1024
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(14.5)
            val centre = state.locations.firstOrNull()?.position ?: MockRepository.centre
            controller.setCenter(GeoPoint(centre.latitude, centre.longitude))
            onResume()
            map = this
        }
    }, update = { view ->
        view.setUseDataConnection(state.isOnline)
        view.overlays.clear()
        fun addPoint(id: String, point: GeoPointData, title: String, selected: Boolean, facility: Boolean, onClick: () -> Unit) {
            val marker = Marker(view).apply {
                position = GeoPoint(point.latitude, point.longitude)
                this.title = title
                icon = GradientDrawable().apply {
                    shape = if (facility) GradientDrawable.RECTANGLE else GradientDrawable.OVAL
                    cornerRadius = 6 * density
                    setColor(if (facility) facilityColor else taskColor)
                    setStroke((3 * density).roundToInt(), if (selected) 0xFF8CE2F1.toInt() else 0xFFFFFFFF.toInt())
                    val size = ((if (selected) 23 else 17) * density).roundToInt()
                    setSize(size, size)
                }
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setOnMarkerClickListener { _, _ -> onClick(); true }
            }
            view.overlays.add(marker)
            if (selected && lastFocus != id) { view.controller.setZoom(16.8); view.controller.animateTo(marker.position); lastFocus = id }
        }
        if (facilities) state.locations.forEach { location -> addPoint("location:${location.id}", location.position, location.name, state.selectedLocationId == location.id, true) { model.selectLocation(location.id) } }
        else state.tasks.forEach { task -> addPoint("task:${task.id}", task.place.position, task.title, state.selectedTaskId == task.id, false) { model.selectTask(task.id) } }
        if (state.selectedTaskId == null && state.selectedLocationId == null) lastFocus = null
        state.currentPosition?.let { current ->
            val marker = Marker(view).apply {
                position = GeoPoint(current.latitude, current.longitude); title = "Your location"
                icon = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xFF00B7C2.toInt()); setStroke((4 * density).roundToInt(), 0xFFFFFFFF.toInt()); val size = (23 * density).roundToInt(); setSize(size, size) }
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
            view.overlays.add(marker)
            if (focusUserToken > lastUserToken) { view.controller.animateTo(marker.position); view.controller.setZoom(15.5); lastUserToken = focusUserToken }
        }
        view.invalidate()
    })
}
