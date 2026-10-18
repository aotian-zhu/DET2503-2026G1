package no.dte2503.volunteer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
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
import no.dte2503.volunteer.data.MessagePriority
import no.dte2503.volunteer.AppUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun InboxScreen(model: MainViewModel) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val selected = state.selectedConversationTaskId
    var showTaskUpdates by rememberSaveable { mutableStateOf(false) }
    if (selected != null) { ConversationView(model, selected); return }
    Column(Modifier.fillMaxSize().testTag("inbox_screen")) {
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !showTaskUpdates, onClick = { showTaskUpdates = false }, label = { Text("Team updates") }, modifier = Modifier.testTag("team_updates_filter"))
            FilterChip(selected = showTaskUpdates, onClick = { showTaskUpdates = true }, label = { Text("My task updates") }, modifier = Modifier.testTag("task_updates_filter"))
        }
        if (showTaskUpdates) TaskUpdates(state)
        else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
        item { Text("Stay in the loop.", style = MaterialTheme.typography.headlineMedium); Text("Briefings, updates and the people on your team.", Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { SectionTitle("Announcements") }
        if (state.announcements.isEmpty()) item { InfoCard { Text("No new announcements. Refresh for the latest team updates.") } }
        items(state.announcements, key = { it.id }) { announcement ->
            InfoCard(highlighted = !announcement.isRead, modifier = Modifier.testTag("announcement_${announcement.id}")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (announcement.priority == MessagePriority.NORMAL) Icons.Rounded.Campaign else Icons.Rounded.PriorityHigh, null, tint = MaterialTheme.colorScheme.primary)
                    Text(announcement.title, Modifier.padding(start = 10.dp).weight(1f), style = MaterialTheme.typography.titleMedium)
                }
                if (announcement.priority != MessagePriority.NORMAL) Text(announcement.priority.name.lowercase().replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(announcement.body, style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${announcement.sender} · ${announcement.time}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!announcement.isRead) TextButton(onClick = { model.markAnnouncementRead(announcement.id) }, modifier = Modifier.testTag("read_${announcement.id}")) { Text("Mark read") }
                    else Icon(Icons.Rounded.Done, "Read", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        item { SectionTitle("Task conversations") }
        if (state.conversations.isEmpty()) item { InfoCard { Text("Start a conversation from one of your tasks.", style = MaterialTheme.typography.bodyMedium) } }
        items(state.conversations, key = { it.id }) { conversation ->
            val task = state.tasks.firstOrNull { it.id == conversation.taskId }
            if (task != null) InfoCard(onClick = { model.openConversation(task.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.secondary)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(task.title, style = MaterialTheme.typography.titleMedium)
                        Text(conversation.coordinatorName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (conversation.unreadCount > 0) Badge { Text(conversation.unreadCount.toString()) }
                }
                Text(conversation.messages.lastOrNull()?.body ?: "Open the conversation", style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            }
        }
        item {
            InfoCard {
                Text("Need immediate help?", style = MaterialTheme.typography.titleMedium)
                Text("Contact your on-site coordinator using the radio or contact details given at your briefing.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    }
}

@Composable
private fun TaskUpdates(state: AppUiState) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().testTag("task_updates_list")) {
        item {
            Text("Your task activity", style = MaterialTheme.typography.headlineSmall)
            Text(if (state.isDemoMode) "Your last 40 demo changes, saved on this phone." else "Your last 40 changes on this phone. Offline task changes sync when you reconnect.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.taskUpdates.isEmpty()) item { InfoCard { Text("No task changes yet. Start, complete or reopen one of your tasks.") } }
        items(state.taskUpdates, key = { it.id }) { update ->
            InfoCard(modifier = Modifier.testTag("task_update_${update.id}")) {
                Text(update.title, style = MaterialTheme.typography.titleMedium)
                Text("${taskStatusLabel(update.previousStatus)} → ${taskStatusLabel(update.status)}", style = MaterialTheme.typography.bodyMedium)
                Text(Instant.ofEpochMilli(update.changedAtMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM · HH:mm")), style = MaterialTheme.typography.bodySmall)
                Text(if (state.isDemoMode) "Saved on this device" else if (update.taskId in state.pendingTaskIds) "Latest status waiting to sync" else "Latest status synchronized", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun ConversationView(model: MainViewModel, taskId: String) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val task = state.tasks.firstOrNull { it.id == taskId }
    if (task == null) { LaunchedEffect(taskId) { model.openConversation(null) }; return }
    val conversation = state.conversations.firstOrNull { it.taskId == taskId }
    val messages = conversation?.messages.orEmpty()
    var draft by remember(state.isDemoMode, state.profile.id, taskId) { mutableStateOf(state.messageDrafts[taskId].orEmpty()) }
    val list = rememberLazyListState()
    BackHandler { model.openConversation(null) }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) list.animateScrollToItem(messages.lastIndex) }
    Column(Modifier.fillMaxSize().padding(20.dp).testTag("conversation_screen"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { model.openConversation(null) }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to inbox") }
            Column(Modifier.weight(1f)) { Text(task.title, style = MaterialTheme.typography.titleMedium); Text(conversation?.coordinatorName ?: "Shift coordinator", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (state.isDemoMode) Text("Practice conversation · Saved on this device", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        else if (!state.isOnline) Text("Offline · Your draft is saved on this device. Connect to send.", style = MaterialTheme.typography.bodySmall)
        LazyColumn(Modifier.weight(1f), state = list, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (messages.isEmpty()) item { Text("Ask your coordinator a question about this task.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(messages, key = { it.id }) { message ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.isFromVolunteer) Arrangement.End else Arrangement.Start) {
                    Card(Modifier.fillMaxWidth(.88f), colors = CardDefaults.cardColors(containerColor = if (message.isFromVolunteer) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(message.sender, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(message.body, style = MaterialTheme.typography.bodyMedium)
                            Text(message.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(draft, {
                if (it.length <= 2000) {
                    draft = it
                    model.saveMessageDraft(taskId, it)
                }
            }, label = { Text("Message your coordinator") }, maxLines = 4, modifier = Modifier.weight(1f).testTag("message_input"))
            IconButton(onClick = {
                val submitted = draft
                model.sendMessage(taskId, submitted) { if (draft == submitted) draft = "" }
            }, enabled = draft.isNotBlank() && !state.isSendingMessage && (state.isDemoMode || state.isOnline), modifier = Modifier.testTag("send_message")) { Icon(Icons.AutoMirrored.Rounded.Send, "Send message") }
        }
    }
}
