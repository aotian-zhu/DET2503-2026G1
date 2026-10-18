package no.dte2503.volunteer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import no.dte2503.volunteer.MainViewModel
import no.dte2503.volunteer.AppUiState
import no.dte2503.volunteer.data.*
import no.dte2503.volunteer.ui.theme.Glacier

@Composable
fun HomeScreen(model: MainViewModel, onOpenTasks: () -> Unit, onOpenFunctions: () -> Unit, onViewOnMap: (String) -> Unit, onOpenActions: (String) -> Unit) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val next = state.tasks.firstOrNull { it.status != TaskStatus.DONE }
    val nearest = tasksByDistance(state.tasks.filter { it.status != TaskStatus.DONE }, state.currentPosition).firstOrNull()
    val meeting = state.locations.firstOrNull { it.id == state.shift.meetingPointId }
    val done = state.tasks.count { it.status == TaskStatus.DONE }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxSize().testTag("home_screen")) {
        item {
            ArcticHero {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("YOUR VOLUNTEER DAY", style = MaterialTheme.typography.labelSmall, color = Glacier)
                    if (state.isDemoMode) Text("DEMO", style = MaterialTheme.typography.labelSmall, color = Glacier)
                }
                Text("Hey, ${state.signedInName.substringBefore(' ')}.\nReady for the mountain?", style = MaterialTheme.typography.headlineMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp), tint = Glacier)
                    Text("${state.shift.startTime} – ${state.shift.endTime}", Modifier.padding(start = 8.dp), fontWeight = FontWeight.SemiBold)
                }
                Text("$done of ${state.tasks.size} tasks completed", style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { if (state.tasks.isEmpty()) 0f else done.toFloat() / state.tasks.size }, modifier = Modifier.fillMaxWidth(), color = Glacier, trackColor = Color.White.copy(alpha = .18f))
            }
        }
        item {
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Badge, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(state.shift.title, style = MaterialTheme.typography.titleMedium)
                        Text("Meet at ${meeting?.name ?: "your assigned meeting point"}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (meeting != null) state.currentPosition?.let { current ->
                    val distance = distanceMetres(current, meeting.position)
                    Text("${formatDistance(distance)} to your meeting point · ${walkingEstimate(distance, state.accuracyMetres)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                if (state.arrivalLocationId != null) Text(arrivalDescription(state), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
        item { WeatherCard(state, model::refresh) }
        item { SectionTitle("Up next") }
        item {
            InfoCard(highlighted = true, onClick = onOpenTasks) {
                if (next == null) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary)
                    Text("All tasks completed", style = MaterialTheme.typography.titleLarge)
                    Text("Thanks for your work. Check the inbox before your handover.")
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(next.dueTime, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        StatusPill(next.status)
                    }
                    Text(next.title, style = MaterialTheme.typography.titleLarge)
                    Text(state.locations.firstOrNull { it.id == next.locationId }?.name ?: next.place.area, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onOpenTasks) { Text("View tasks"); Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.padding(start = 6.dp).size(18.dp)) }
                        OutlinedButton(onClick = { onViewOnMap(next.id) }) { Icon(Icons.Rounded.Map, null, Modifier.size(18.dp)); Text(" Map") }
                    }
                }
            }
        }
        if (state.currentPosition != null && nearest != null) item {
            InfoCard(onClick = { onViewOnMap(nearest.task.id) }, modifier = Modifier.testTag("nearest_task_card")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.NearMe, null, tint = MaterialTheme.colorScheme.secondary)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("Closest unfinished task", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        Text(nearest.task.title, style = MaterialTheme.typography.titleMedium)
                        Text("${nearest.distanceMetres?.let(::formatDistance)} away · straight-line distance", style = MaterialTheme.typography.bodySmall)
                        nearest.distanceMetres?.let { Text(walkingEstimate(it, state.accuracyMetres), style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
        item { SectionTitle("On-site essentials") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = { onOpenActions("checkin") }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Icon(Icons.Rounded.QrCodeScanner, null, Modifier.size(18.dp)); Text(" Check in") }
                FilledTonalButton(onClick = { onOpenActions("report") }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Icon(Icons.Rounded.Flag, null, Modifier.size(18.dp)); Text(" Report issue") }
            }
        }
        item {
            InfoCard(onClick = onOpenFunctions) {
                Text("Your place in the team", style = MaterialTheme.typography.titleMedium)
                Text(model.assignedFunctions().joinToString { it.name }.ifBlank { "Your coordinator will assign your role." }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("View your volunteer profile →", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun WeatherCard(state: AppUiState, onRefresh: () -> Unit) {
    val weather = state.weather
    val context = LocalContext.current
    InfoCard(modifier = Modifier.testTag("weather_card")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CloudQueue, null, tint = MaterialTheme.colorScheme.secondary)
            Text("Narvik weather", Modifier.padding(start = 10.dp).weight(1f), style = MaterialTheme.typography.titleMedium)
            if (weather != null) Text("${weather.temperatureC.roundToInt()}°", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }
        if (weather == null) {
            Text(if (state.refreshing) "Checking current conditions…" else "Connect and refresh to load current conditions.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        } else {
            Text("${weatherDescription(weather.weatherCode)} · Feels like ${weather.feelsLikeC.roundToInt()}° · Wind ${weather.windKmh.roundToInt()} km/h", style = MaterialTheme.typography.bodyMedium)
            Text(when {
                weather.feelsLikeC <= 0 -> "For an outdoor assignment, bring warm layers and check the briefing for icy surfaces."
                weather.windKmh >= 30 -> "Windy conditions: take your event jacket and check the outdoor briefing."
                weather.weatherCode >= 51 -> "Wet or snowy conditions: bring a weatherproof layer for outdoor tasks."
                else -> "Check your briefing and pack an extra layer before heading outdoors."
            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            val time = Instant.ofEpochMilli(weather.fetchedAtMillis).atZone(ZoneId.of("Europe/Oslo")).format(DateTimeFormatter.ofPattern("dd MMM · HH:mm", Locale.UK))
            val cached = !state.isOnline || state.weatherError != null || System.currentTimeMillis() - weather.fetchedAtMillis > 30 * 60_000
            Text("${if (cached) "Saved reading" else "Updated"} $time (Narvik time)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.weatherError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/"))) }, contentPadding = PaddingValues(0.dp)) { Text("Weather by Open-Meteo", style = MaterialTheme.typography.labelSmall) }
            if (weather == null && state.isOnline) TextButton(onClick = onRefresh, enabled = !state.refreshing) { Text("Refresh") }
        }
    }
}
