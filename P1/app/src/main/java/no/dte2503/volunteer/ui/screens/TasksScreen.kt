package no.dte2503.volunteer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.dte2503.volunteer.MainViewModel
import no.dte2503.volunteer.data.*

@Composable
fun TasksScreen(model: MainViewModel, onContactCoordinator: (String) -> Unit, onViewOnMap: (String) -> Unit) {
    val state by model.uiState.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf("All") }
    var nearby by rememberSaveable { mutableStateOf(false) }
    val filtered = state.tasks.filter { when (filter) { "Open" -> it.status != TaskStatus.DONE; "Done" -> it.status == TaskStatus.DONE; else -> true } }
    val items = if (nearby && state.currentPosition != null) tasksByDistance(filtered, state.currentPosition) else filtered.map { TaskWithDistance(it, state.currentPosition?.let { position -> distanceMetres(position, it.place.position) }) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize().testTag("tasks_screen")) {
        item {
            Text("One task at a time.", style = MaterialTheme.typography.headlineMedium)
            Text("${state.tasks.count { it.status != TaskStatus.DONE }} remaining · ${state.shift.startTime} – ${state.shift.endTime}", Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Open", "Done").forEach { label -> FilterChip(selected = filter == label, onClick = { filter = label }, label = { Text(label) }, modifier = Modifier.testTag("filter_$label")) }
                if (state.currentPosition != null) FilterChip(selected = nearby, onClick = { nearby = !nearby }, label = { Text("Nearest") })
            }
        }
        if (items.isEmpty()) item { InfoCard { Text("Nothing here for now.", style = MaterialTheme.typography.titleMedium); Text("Try another filter or check back after your next update.") } }
        items(items, key = { it.task.id }) { item ->
            val task = item.task
            InfoCard(modifier = Modifier.testTag("task_${task.id}")) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(task.dueTime, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    StatusPill(task.status)
                }
                Text(task.title, style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Place, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                    Column(Modifier.padding(start = 8.dp)) {
                        Text(state.locations.firstOrNull { it.id == task.locationId }?.name ?: task.place.area, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                        Text(listOfNotNull(task.place.area, task.place.floor).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item.distanceMetres?.let { Text("${formatDistance(it)} away · straight-line", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium) }
                item.distanceMetres?.let { Text(walkingEstimate(it, state.accuracyMetres), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (task.id in state.pendingTaskIds) Text("Saved on this phone · Waiting to sync", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.testTag("pending_${task.id}"))
                Text(task.details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (task.status != TaskStatus.DONE) Button(onClick = { model.advanceTask(task.id) }, enabled = task.id !in state.busyTasks, modifier = Modifier.weight(1f).testTag("advance_${task.id}")) {
                        Text(if (task.id in state.busyTasks) "Saving…" else if (task.status == TaskStatus.TODO) "Start task" else "Complete task")
                    }
                    else OutlinedButton(onClick = { model.reopenTask(task.id) }, enabled = task.id !in state.busyTasks, modifier = Modifier.weight(1f).testTag("reopen_${task.id}")) {
                        Icon(Icons.Rounded.Undo, null, Modifier.size(18.dp))
                        Text(if (task.id in state.busyTasks) "Saving…" else " Reopen task")
                    }
                    OutlinedButton(onClick = { onViewOnMap(task.id) }, modifier = Modifier.weight(1f).testTag("map_${task.id}")) { Icon(Icons.Rounded.Map, null, Modifier.size(18.dp)); Text(" View map") }
                }
                TextButton(onClick = { onContactCoordinator(task.id) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.testTag("contact_${task.id}")) { Icon(Icons.Rounded.ChatBubbleOutline, null, Modifier.size(18.dp)); Text(" Contact coordinator", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}
