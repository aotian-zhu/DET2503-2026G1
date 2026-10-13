package no.dte2503.volunteer.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import no.dte2503.volunteer.MainViewModel
import no.dte2503.volunteer.data.*

@Composable
fun WorkflowScreen(model: MainViewModel, initialSection: String, onBack: () -> Unit) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var section by rememberSaveable { mutableStateOf(initialSection) }
    var category by rememberSaveable { mutableStateOf(state.incidentDraft.category.name) }
    var description by rememberSaveable { mutableStateOf(state.incidentDraft.description) }
    var locationId by rememberSaveable { mutableStateOf(state.incidentDraft.locationId) }
    var taskId by rememberSaveable { mutableStateOf(state.incidentDraft.taskId) }
    var photoPath by rememberSaveable { mutableStateOf(state.incidentDraft.photoUri) }
    var cameraDenied by rememberSaveable { mutableStateOf(false) }
    var showManual by rememberSaveable { mutableStateOf(false) }
    var qrText by rememberSaveable { mutableStateOf("") }
    fun draft() = IncidentDraft(IncidentCategory.valueOf(category), description, locationId, taskId, photoPath)
    fun saveDraft() { model.saveIncidentDraft(draft()) }
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result -> result.contents?.let(model::checkIn) }
    fun launchScanner() { scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Scan your shift checkpoint").setBeepEnabled(false).setOrientationLocked(false)) }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> cameraDenied = !granted; if (granted) launchScanner() }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            photoPath = uri.toString()
            saveDraft()
        }
    }
    Column(Modifier.fillMaxSize().testTag("workflow_screen")) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Text("On-site essentials", style = MaterialTheme.typography.titleLarge)
        }
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = section == "checkin", onClick = { section = "checkin" }, label = { Text("Check in") })
            FilterChip(selected = section == "report", onClick = { section = "report" }, label = { Text("Report issue") }, modifier = Modifier.testTag("report_tab"))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (section == "checkin") {
                ArcticHero { Icon(Icons.Rounded.QrCodeScanner, null, Modifier.size(40.dp)); Text("Here for your shift.", style = MaterialTheme.typography.headlineMedium); Text("Scan the checkpoint code at your meeting point.") }
                InfoCard {
                    Text(state.shift.title, style = MaterialTheme.typography.titleMedium)
                    Text("Repeated scans return your existing check-in. They never create another demo record.", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchScanner() else cameraPermission.launch(Manifest.permission.CAMERA) }, enabled = !state.isSubmittingWorkflow, modifier = Modifier.fillMaxWidth().testTag("scan_qr")) { Icon(Icons.Rounded.QrCodeScanner, null); Text(" Scan checkpoint code") }
                    if (cameraDenied) {
                        Text("Camera access is off. Enable it in settings or enter a checkpoint code below.", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Open app settings") }
                    }
                    if (state.isDemoMode) {
                        OutlinedButton(onClick = { model.checkIn(MockRepository.demoQr) }, enabled = !state.isSubmittingWorkflow, modifier = Modifier.fillMaxWidth().testTag("demo_checkin")) { Text("Try the demo checkpoint") }
                        Text("Practice check-ins stay on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { showManual = !showManual }) { Text("Enter a checkpoint code") }
                    if (showManual) {
                        OutlinedTextField(qrText, { qrText = it }, label = { Text("Checkpoint code") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { model.checkIn(qrText) }, enabled = qrText.isNotBlank() && !state.isSubmittingWorkflow) { Text("Confirm check-in") }
                    }
                }
            } else {
                Text("Help us put it right.", style = MaterialTheme.typography.headlineMedium)
                Text("Describe what you noticed. Your draft saves automatically while you write.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                InfoCard {
                    ChoiceMenu("Category", IncidentCategory.valueOf(category).name.lowercase().replaceFirstChar(Char::uppercase), IncidentCategory.entries.map { it.name to it.name.lowercase().replaceFirstChar(Char::uppercase) }) { category = requireNotNull(it); saveDraft() }
                    OutlinedTextField(description, { if (it.length <= 2000) { description = it; saveDraft() } }, label = { Text("What happened?") }, minLines = 4, supportingText = { Text("${description.length}/2000 · At least 3 characters") }, modifier = Modifier.fillMaxWidth().testTag("incident_description"))
                    ChoiceMenu("Location", state.locations.firstOrNull { it.id == locationId }?.name ?: "Choose a location", listOf(null to "Not specified") + state.locations.map { it.id to it.name }) { locationId = it; saveDraft() }
                    ChoiceMenu("Related task", state.tasks.firstOrNull { it.id == taskId }?.title ?: "Optional", listOf(null to "No related task") + state.tasks.map { it.id to it.title }) { taskId = it; saveDraft() }
                    OutlinedButton(onClick = { photoPicker.launch(arrayOf("image/jpeg", "image/png", "image/webp")) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.AddPhotoAlternate, null); Text(if (photoPath == null) " Add a photo" else " Change attached photo") }
                    if (photoPath != null) {
                        Text("Photo attached · Maximum 5 MB", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { photoPath = null; saveDraft() }) { Text("Remove photo") }
                    }
                    if (!state.isDemoMode && !state.isOnline) Text("Your draft is saved. Connect before sending it to your coordinator.", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = {
                        model.submitIncident(draft(), photoPath?.let(Uri::parse)) { category = IncidentCategory.OTHER.name; description = ""; locationId = null; taskId = null; photoPath = null }
                    }, enabled = description.trim().length >= 3 && !state.isSubmittingWorkflow && (state.isDemoMode || state.isOnline), modifier = Modifier.fillMaxWidth().testTag("submit_incident")) { Text(if (state.isSubmittingWorkflow) "Saving…" else if (state.isDemoMode) "Save demo report" else "Send report") }
                    if (state.isDemoMode) Text("Practice reports stay on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ChoiceMenu(label: String, value: String, choices: List<Pair<String?, String>>, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(value, Modifier.weight(1f)); Icon(Icons.Rounded.ExpandMore, null) }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) { choices.forEach { (id, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(id); expanded = false }) } }
        }
    }
}
