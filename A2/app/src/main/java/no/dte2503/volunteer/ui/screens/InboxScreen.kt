package no.dte2503.volunteer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import no.dte2503.volunteer.MainViewModel
import no.dte2503.volunteer.data.MessagePriority

@Composable
fun InboxScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val selectedTaskId = state.selectedConversationTaskId
    var showEmergencyContacts by remember { mutableStateOf(false) }
    if (showEmergencyContacts) {
        AlertDialog(
            onDismissRequest = { showEmergencyContacts = false },
            title = { Text("Emergency contacts") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Duty coordinator · +47 900 00 001")
                    Text("Medical support · +47 900 00 002")
                    Text("Emergency services · 112")
                }
            },
            confirmButton = { TextButton(onClick = { showEmergencyContacts = false }) { Text("Close") } },
        )
    }
    if (selectedTaskId != null) {
        ConversationView(viewModel, selectedTaskId)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Announcements", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        items(state.announcements, key = { it.id }) { announcement ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (announcement.priority == MessagePriority.IMPORTANT) Icons.Rounded.WarningAmber else Icons.Rounded.Campaign,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(announcement.title, Modifier.padding(start = 10.dp).weight(1f), fontWeight = FontWeight.SemiBold)
                        if (!announcement.isRead) AssistChip(
                            onClick = { viewModel.markAnnouncementRead(announcement.id) },
                            label = { Text("New") },
                        )
                    }
                    Text(announcement.body)
                    Text("${announcement.sender} · ${announcement.time}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text("Task conversations", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        items(state.conversations, key = { it.id }) { conversation ->
            val task = state.tasks.firstOrNull { it.id == conversation.taskId }
            if (task != null) Card(
                onClick = { viewModel.openConversation(task.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(task.title, fontWeight = FontWeight.SemiBold)
                        Text(conversation.coordinatorName, style = MaterialTheme.typography.bodySmall)
                        Text(conversation.messages.lastOrNull()?.body.orEmpty(), maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (conversation.unreadCount > 0) Text(conversation.unreadCount.toString(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Urgent contact", fontWeight = FontWeight.SemiBold)
                    Text("For safety, medical or immediate operational problems, contact the duty coordinator.")
                    Button(onClick = { showEmergencyContacts = true }) { Text("View emergency contacts") }
                }
            }
        }
    }
}

@Composable
private fun ConversationView(viewModel: MainViewModel, taskId: String) {
    val state by viewModel.uiState.collectAsState()
    val task = state.tasks.firstOrNull { it.id == taskId }
    if (task == null) {
        LaunchedEffect(taskId) { viewModel.openConversation(null) }
        return
    }
    val conversation = state.conversations.firstOrNull { it.taskId == taskId }
    val messages = conversation?.messages.orEmpty()
    val location = viewModel.locations.firstOrNull { it.id == task.locationId }
    var draft by remember(taskId) { mutableStateOf("") }
    val listState = rememberLazyListState()
    BackHandler { viewModel.openConversation(null) }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.openConversation(null) }) { Icon(Icons.Rounded.ArrowBack, "Back") }
            Column {
                Text(task.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${task.dueTime} · ${location?.name ?: "Location unavailable"}", style = MaterialTheme.typography.bodySmall)
                Text(
                    listOfNotNull(task.place.area, task.place.floor).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Text(conversation?.coordinatorName ?: "Shift coordinator", color = MaterialTheme.colorScheme.primary)
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.isFromVolunteer) Arrangement.End else Arrangement.Start,
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.82f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (message.isFromVolunteer) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(message.sender, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                            Text(message.body)
                            Text(message.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("Message coordinator") },
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    viewModel.sendMessage(taskId, draft)
                    draft = ""
                },
                enabled = draft.isNotBlank() && !state.isSendingMessage,
            ) { Icon(Icons.Rounded.Send, "Send") }
        }
    }
}
