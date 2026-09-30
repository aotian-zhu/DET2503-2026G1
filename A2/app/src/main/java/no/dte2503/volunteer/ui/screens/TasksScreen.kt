package no.dte2503.volunteer.ui.screens

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
        items(tasks.size, key = { tasks[it].id }) { index ->
            val task = tasks[index]
            val location = viewModel.locations.firstOrNull { it.id == task.locationId }
            val scenario = viewModel.scenarios.firstOrNull { it.id == task.scenarioId }
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
                            Text("${task.dueTime} · ${location?.name ?: task.locationId}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                listOfNotNull(task.place.area, task.place.floor).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Text(task.details)
                    scenario?.let {
                        Text("Support scenario: ${it.name}", style = MaterialTheme.typography.labelMedium)
                    }
                    // 卡片只表达用户意图；状态推进、跨页定位和联系编排由上层处理。
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (task.status != TaskStatus.DONE) {
                            AssistChip(
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.advanceTask(task.id) },
                                label = {
                                    Text(
                                        if (task.status == TaskStatus.TODO) "Start task" else "Mark complete",
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                    )
                                },
                            )
                        }
                        AssistChip(
                            modifier = Modifier.weight(1.15f),
                            onClick = { onViewOnMap(task.id) },
                            label = {
                                Text(
                                    "View on map",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                )
                            },
                            leadingIcon = { Icon(Icons.Rounded.Map, null) },
                        )
                        AssistChip(
                            modifier = Modifier.weight(1.35f),
                            onClick = { onContactCoordinator(task.id) },
                            label = {
                                Text(
                                    "Contact coordinator",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
