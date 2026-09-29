package no.dte2503.volunteer.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import no.dte2503.volunteer.MainViewModel
import no.dte2503.volunteer.data.TaskStatus

@Composable
fun TasksScreen(
    viewModel: MainViewModel,
    tasks: List<no.dte2503.volunteer.data.VolunteerTask>,
    onContactCoordinator: (String) -> Unit,
    onViewOnMap: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Tasks", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Tap the action to move a task forward.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(tasks.size, key = { tasks[it].id }) { index ->
            val task = tasks[index]
            val location = viewModel.locations.first { it.id == task.locationId }
            val scenario = viewModel.scenarios.first { it.id == task.scenarioId }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when (task.status) {
                                TaskStatus.TODO -> Icons.Rounded.RadioButtonUnchecked
                                TaskStatus.IN_PROGRESS -> Icons.Rounded.Timelapse
                                TaskStatus.DONE -> Icons.Rounded.CheckCircle
                            }, null, tint = MaterialTheme.colorScheme.primary
                        )
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(task.title, fontWeight = FontWeight.SemiBold)
                            Text("${task.dueTime} · ${location.name}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                listOfNotNull(task.place.area, task.place.floor).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Text(task.details)
                    Text("Support scenario: ${scenario.name}", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (task.status != TaskStatus.DONE) {
                            AssistChip(onClick = { viewModel.advanceTask(task.id) }, label = { Text(if (task.status == TaskStatus.TODO) "Start task" else "Mark complete") })
                        }
                        AssistChip(
                            onClick = { onViewOnMap(task.id) },
                            label = { Text("View on map") },
                            leadingIcon = { Icon(Icons.Rounded.Map, null) },
                        )
                        AssistChip(onClick = { onContactCoordinator(task.id) }, label = { Text("Contact coordinator") })
                    }
                }
            }
        }
    }
}
