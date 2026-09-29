package no.dte2503.volunteer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import no.dte2503.volunteer.MainViewModel
import no.dte2503.volunteer.data.VolunteerFunction

@Composable
fun ProfileScreen(viewModel: MainViewModel, onLogout: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val functions = viewModel.assignedFunctions()
    val permissions = viewModel.effectivePermissions()
    val meetingPoint = viewModel.locations.firstOrNull { it.id == viewModel.shift.meetingPointId }
    val initial = state.signedInName.firstOrNull()?.uppercase() ?: "V"
    val context = LocalContext.current
    var dialogTitle by remember { mutableStateOf<String?>(null) }
    var dialogBody by remember { mutableStateOf("") }
    var editingEmail by remember { mutableStateOf(false) }
    var emailDraft by remember(state.signedInEmail) { mutableStateOf(state.signedInEmail) }
    var emailError by remember { mutableStateOf(false) }

    fun showDetails(title: String, body: String) {
        dialogTitle = title
        dialogBody = body
    }

    if (editingEmail) {
        AlertDialog(
            onDismissRequest = { editingEmail = false },
            title = { Text("Edit email") },
            text = {
                OutlinedTextField(
                    value = emailDraft,
                    onValueChange = { emailDraft = it; emailError = false },
                    label = { Text("Username or email") },
                    isError = emailError,
                    supportingText = { if (emailError) Text("Email cannot be empty") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (viewModel.updateEmail(emailDraft)) editingEmail = false else emailError = true
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingEmail = false }) { Text("Cancel") } },
        )
    }

    dialogTitle?.let { title ->
        AlertDialog(
            onDismissRequest = { dialogTitle = null },
            title = { Text(title) },
            text = { Text(dialogBody) },
            confirmButton = { TextButton(onClick = { dialogTitle = null }) { Text("Close") } },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(initial, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.padding(start = 16.dp).weight(1f)) {
                        Text(state.signedInName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(functions.joinToString { it.name }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Volunteer ID · ${viewModel.profile.id.removePrefix("volunteer_")}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Text("Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ProfileGroup {
                ProfileRow(Icons.Rounded.Email, "Username or email", state.signedInEmail.ifBlank { "Not provided" }) {
                    emailDraft = state.signedInEmail
                    emailError = false
                    editingEmail = true
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ProfileRow(Icons.Rounded.Phone, "Phone", viewModel.profile.phone) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${viewModel.profile.phone}"))
                    if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
                    else showDetails("Phone", "Call ${viewModel.profile.phone}")
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ProfileRow(Icons.Rounded.Schedule, "Current shift", "${viewModel.shift.startTime}–${viewModel.shift.endTime}") {
                    showDetails(
                        "Current shift",
                        "${viewModel.shift.title}\n${viewModel.shift.startTime}–${viewModel.shift.endTime}\nMeeting point: ${meetingPoint?.name ?: "Not available"}",
                    )
                }
            }
        }
        item {
            Text("Work access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ProfileGroup {
                ProfileRow(Icons.Rounded.Badge, "Function modules", "${functions.size} assigned") {
                    showDetails("Function modules", functions.formatDetails())
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ProfileRow(Icons.Rounded.Lock, "Permissions", "${permissions.size} active") {
                    showDetails("Permissions", permissions.joinToString("\n") { "• ${it.label}" }.ifBlank { "No active permissions" })
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Assigned functions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                functions.forEach { function ->
                    Card(
                        onClick = { showDetails(function.name, function.description) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(function.name, fontWeight = FontWeight.SemiBold)
                            Text(function.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Text("Sign out")
            }
        }
    }
}

private fun List<VolunteerFunction>.formatDetails(): String =
    joinToString("\n\n") { "${it.name}\n${it.description}" }.ifBlank { "No assigned functions" }

@Composable
private fun ProfileGroup(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
    ) { Column { content() } }
}

@Composable
private fun ProfileRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
