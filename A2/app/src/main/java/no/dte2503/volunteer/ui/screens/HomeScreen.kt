package no.dte2503.volunteer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import no.dte2503.volunteer.MainViewModel
import no.dte2503.volunteer.data.TaskStatus

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenTasks: () -> Unit,
    onOpenFunctions: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val nextTask = state.tasks.firstOrNull { it.status != TaskStatus.DONE }
    val meetingPoint = viewModel.locations.first { it.id == viewModel.shift.meetingPointId }
    LazyColumn(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Good morning, ${state.signedInName}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Here is what you need for today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { SectionTitle("Current shift") }
        item {
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AccessTime, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(viewModel.shift.title, fontWeight = FontWeight.SemiBold)
                        Text("${viewModel.shift.startTime}–${viewModel.shift.endTime} · ${meetingPoint.name}")
                    }
                }
            }
        }
        item { SectionTitle("Your functions") }
        items(viewModel.assignedFunctions().size) { index ->
            val function = viewModel.assignedFunctions()[index]
            InfoCard(onClick = onOpenFunctions) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Badge, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(function.name, fontWeight = FontWeight.SemiBold)
                        Text(function.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item { SectionTitle("Next action") }
        item {
            InfoCard(highlighted = true, onClick = if (nextTask != null) onOpenTasks else null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(nextTask?.dueTime ?: "All done", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(nextTask?.title ?: "No remaining tasks", fontWeight = FontWeight.SemiBold)
                        nextTask?.let { Text(it.details, style = MaterialTheme.typography.bodyMedium) }
                    }
                    Icon(Icons.Rounded.ArrowForward, null)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

@Composable
fun InfoCard(
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer else Color.White,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        ),
    ) { Column(Modifier.padding(16.dp)) { content() } }
}
