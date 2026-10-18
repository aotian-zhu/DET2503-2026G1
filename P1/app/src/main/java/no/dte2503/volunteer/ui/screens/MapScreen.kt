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
import no.dte2503.volunteer.platform.hasLocationAccess
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

@Composable
fun MapScreen(model: MainViewModel) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var facilities by rememberSaveable { mutableStateOf(false) }
    var focusUserToken by remember { mutableIntStateOf(0) }
    var showDemoWalk by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) { model.requestLocationRefresh(); focusUserToken++ }
        else model.setLocationMessage("Location access was denied. Your task markers are still available.", true)
    }
    if (showDemoWalk) AlertDialog(
        onDismissRequest = { showDemoWalk = false },
        title = { Text("Rehearse a Narvik walk") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Use labelled practice positions to explore the demo from anywhere. This does not change your phone's GPS or send a location to the team.")
            demoWalkPoints.forEach { point -> OutlinedButton(onClick = { model.setDemoWalk(point.id); focusUserToken++; showDemoWalk = false }, modifier = Modifier.fillMaxWidth().testTag("practice_${point.id}")) { Text(point.label) } }
        } },
        confirmButton = { TextButton(onClick = { model.requestLocationRefresh(); showDemoWalk = false }) { Text("Use phone GPS") } },
    )
    LaunchedEffect(state.selectedLocationId) { if (state.selectedLocationId != null) facilities = true }
    val taskItems = tasksByDistance(state.tasks, state.currentPosition)
    val locationItems = state.locations.map { LocationWithDistance(it, state.currentPosition?.let { position -> distanceMetres(position, it.position) }) }.sortedBy { it.distanceMetres ?: Float.MAX_VALUE }
    val list = rememberLazyListState()
    LaunchedEffect(state.selectedTaskId) { if (state.selectedTaskId != null) facilities = false }
    LaunchedEffect(state.selectedTaskId, state.selectedLocationId, facilities, taskItems.map { it.task.id }, locationItems.map { it.location.id }) {
        val index = if (facilities) locationItems.indexOfFirst { it.location.id == state.selectedLocationId } else taskItems.indexOfFirst { it.task.id == state.selectedTaskId }
        if (index >= 0) list.animateScrollToItem(index + 1) else list.scrollToItem(0)
    }
    BoxWithConstraints(Modifier.fillMaxSize().testTag("map_screen")) {
        val mapHeight = (maxHeight * .43f).coerceIn(120.dp, 320.dp)
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = !facilities, onClick = { facilities = false }, label = { Text("Tasks") })
                FilterChip(selected = facilities, onClick = { facilities = true }, label = { Text("Facilities") }, modifier = Modifier.testTag("facilities_filter"))
                Spacer(Modifier.weight(1f))
                Text(if (state.locationIsPreview) "● Practice" else "● You", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            Box(Modifier.fillMaxWidth().height(mapHeight).clip(RoundedCornerShape(22.dp))) {
                OsmMap(model, state, facilities, focusUserToken)
                FilledTonalButton(onClick = {
                    model.setDemoWalk(null)
                    if (hasLocationAccess(context)) { model.requestLocationRefresh(); focusUserToken++; model.selectTask(null) }
                    else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).testTag("locate_me")) { Icon(Icons.Rounded.MyLocation, null, Modifier.size(18.dp)); Text(" Locate me") }
                if (state.isDemoMode) FilledTonalButton(onClick = { showDemoWalk = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).testTag("demo_walk"), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) { Text("Demo walk", style = MaterialTheme.typography.labelMedium) }
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
            if (state.currentPosition != null) Text(if (state.locationIsPreview) "Practice position · Not your phone GPS · Straight-line distances" else "GPS accuracy ≈ ${state.accuracyMetres?.roundToInt() ?: 0} m · Straight-line distances", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("position_source"))
            if (state.arrivalLocationId != null) Text(arrivalDescription(state), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.testTag("arrival_status"))
            if (!state.isOnline) Text("Offline · Visited map tiles may be cached. Markers and GPS still work.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            LazyColumn(Modifier.weight(1f).testTag("map_points_list"), state = list, contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(if (facilities) "Nearby facilities" else if (state.currentPosition != null) "Tasks · Nearest first" else "Your task points", style = MaterialTheme.typography.titleMedium)
                    Text("Numbered tasks · Square facilities · Walking estimates use 4 km/h and straight-line distance. Allow extra for hills, snow and access restrictions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } }
                if (facilities) items(locationItems, key = { it.location.id }) { item ->
                    val location = item.location
                    InfoCard(highlighted = state.selectedLocationId == location.id, onClick = { model.selectLocation(location.id) }, modifier = Modifier.testTag("facility_${location.id}")) {
                        Text(location.name, style = MaterialTheme.typography.titleMedium)
                        Text(location.description, style = MaterialTheme.typography.bodyMedium)
                        item.distanceMetres?.let { Text("${formatDistance(it)} away · ${walkingEstimate(it, state.accuracyMetres)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary) }
                        if (state.selectedLocationId == location.id) {
                            TextButton(onClick = { model.watchArrival(if (state.arrivalLocationId == location.id) null else location.id) }, modifier = Modifier.testTag("watch_${location.id}")) { Icon(Icons.Rounded.NotificationsActive, null, Modifier.size(18.dp)); Text(if (state.arrivalLocationId == location.id) " Stop arrival reminder" else " Watch arrival") }
                            Text("150 m arrival zone · Reminds you while this app is open. Precise GPS improves reliability.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
                        item.distanceMetres?.let { Text(walkingEstimate(it, state.accuracyMetres), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
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
    val doneColor = MaterialTheme.colorScheme.tertiary.toArgb()
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) map?.onResume() else if (event == Lifecycle.Event.ON_PAUSE) map?.onPause() }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer); map?.onPause(); map?.onDetach() }
    }
    AndroidView(modifier = Modifier.fillMaxSize(), factory = {
        Configuration.getInstance().apply {
            userAgentValue = "${context.packageName}/${no.dte2503.volunteer.BuildConfig.VERSION_NAME}"
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
        fun circle(point: GeoPointData, radius: Double, color: Int) {
            view.overlays.add(Polygon(view).apply {
                points = Polygon.pointsAsCircle(GeoPoint(point.latitude, point.longitude), radius)
                fillPaint.color = (color and 0x00FFFFFF) or 0x22000000
                outlinePaint.color = color
                outlinePaint.strokeWidth = 2 * density
            })
        }
        state.locations.firstOrNull { it.id == state.arrivalLocationId }?.let { circle(it.position, ARRIVAL_RADIUS_METRES.toDouble(), facilityColor) }
        if (!state.locationIsPreview) state.currentPosition?.let { position -> state.accuracyMetres?.takeIf { it.isFinite() && it in 1f..5000f }?.let { circle(position, it.toDouble(), 0xFF00B7C2.toInt()) } }
        fun addPoint(id: String, point: GeoPointData, title: String, selected: Boolean, facility: Boolean, label: String, completed: Boolean = false, onClick: () -> Unit) {
            val marker = Marker(view).apply {
                position = GeoPoint(point.latitude, point.longitude)
                this.title = title
                icon = mapMarker(context, label, if (completed) doneColor else if (facility) facilityColor else taskColor, selected, facility)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setOnMarkerClickListener { _, _ -> onClick(); true }
            }
            view.overlays.add(marker)
            if (selected && lastFocus != id) { view.controller.setZoom(16.8); view.controller.animateTo(marker.position); lastFocus = id }
        }
        if (facilities) state.locations.forEach { location -> addPoint("location:${location.id}", location.position, location.name, state.selectedLocationId == location.id, true, if (location.id == state.shift.meetingPointId) "H" else "i") { model.selectLocation(location.id) } }
        else state.tasks.forEachIndexed { index, task -> addPoint("task:${task.id}", task.place.position, task.title, state.selectedTaskId == task.id, false, if (task.status == TaskStatus.DONE) "✓" else "${index + 1}", task.status == TaskStatus.DONE) { model.selectTask(task.id) } }
        if (state.selectedTaskId == null && state.selectedLocationId == null) lastFocus = null
        state.currentPosition?.let { current ->
            val marker = Marker(view).apply {
                position = GeoPoint(current.latitude, current.longitude); title = if (state.locationIsPreview) "Practice position" else "Your location"
                icon = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xFF00B7C2.toInt()); setStroke((4 * density).roundToInt(), 0xFFFFFFFF.toInt()); val size = (23 * density).roundToInt(); setSize(size, size) }
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
            view.overlays.add(marker)
            if (focusUserToken > lastUserToken) { view.controller.animateTo(marker.position); view.controller.setZoom(15.5); lastUserToken = focusUserToken }
        }
        view.invalidate()
    })
}
