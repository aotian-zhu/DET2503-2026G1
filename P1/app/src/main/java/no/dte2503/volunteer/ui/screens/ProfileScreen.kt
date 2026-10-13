package no.dte2503.volunteer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.dte2503.volunteer.MainViewModel
import no.dte2503.volunteer.data.ThemeMode

@Composable
fun ProfileScreen(model: MainViewModel, onLogout: () -> Unit) {
    val state by model.uiState.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxSize().testTag("profile_screen")) {
        item {
            InfoCard(highlighted = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Text(state.signedInName.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineMedium)
                    }
                    Column(Modifier.padding(start = 16.dp).weight(1f)) {
                        Text(state.signedInName, style = MaterialTheme.typography.titleLarge)
                        Text(if (state.isDemoMode) "Narvik 2029 · Demo volunteer" else "Narvik 2029 · Volunteer", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item { SectionTitle("Volunteer details") }
        item {
            InfoCard {
                Text(state.signedInEmail, style = MaterialTheme.typography.bodyMedium)
                if (state.profile.phone.isNotBlank()) Text(state.profile.phone)
                Text("Volunteer ID · ${state.profile.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(state.shift.title, style = MaterialTheme.typography.titleMedium)
                Text("${state.shift.startTime} – ${state.shift.endTime} · ${state.locations.firstOrNull { it.id == state.shift.meetingPointId }?.name ?: "Meeting point unassigned"}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        item { SectionTitle("Your team roles") }
        if (model.assignedFunctions().isEmpty()) item { InfoCard { Text("Your coordinator has not assigned a team role yet.") } }
        model.assignedFunctions().forEach { function -> item(key = function.id) {
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Badge, null, tint = MaterialTheme.colorScheme.primary); Text(function.name, Modifier.padding(start = 10.dp), style = MaterialTheme.typography.titleMedium) }
                Text(function.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } }
        item {
            InfoCard {
                Text("Work access", style = MaterialTheme.typography.titleMedium)
                model.effectivePermissions().forEach { permission -> Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.CheckCircleOutline, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary); Text(permission.label, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium) } }
            }
        }
        item {
            InfoCard {
                Text("Make yourself at home", style = MaterialTheme.typography.titleMedium)
                Text("Choose your appearance", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode -> FilterChip(selected = state.themeMode == mode, onClick = { model.setTheme(mode) }, label = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) }, modifier = Modifier.testTag("theme_${mode.name}")) }
                }
            }
        }
        item {
            InfoCard {
                Text("Available offline", style = MaterialTheme.typography.titleMedium)
                Text("Your shift, tasks, locations and team updates stay saved on this device.", style = MaterialTheme.typography.bodyMedium)
                if (state.isDemoMode) Text("${state.checkInCount} demo check-ins · ${state.incidentCount} demo reports\nDemo messages and reports stay on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else Text("${state.pendingCount} task or read updates waiting to sync. Team messages and reports require a connection.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().testTag("sign_out")) { Text(if (state.isDemoMode) "Leave demo" else "Sign out") } }
        item { Text("Arena Companion v0.5\nProgramming for Mobile Devices 2 · P1", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
