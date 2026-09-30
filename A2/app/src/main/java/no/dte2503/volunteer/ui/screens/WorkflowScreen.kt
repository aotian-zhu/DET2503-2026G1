package no.dte2503.volunteer.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import no.dte2503.volunteer.MainViewModel
import no.dte2503.volunteer.data.IncidentCategory
import no.dte2503.volunteer.data.IncidentDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var category by remember { mutableStateOf(IncidentCategory.OTHER) }
    var description by remember { mutableStateOf("") }
    var locationId by remember { mutableStateOf<String?>(null) }
    var taskId by remember { mutableStateOf<String?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var categoryMenu by remember { mutableStateOf(false) }
    var locationMenu by remember { mutableStateOf(false) }
    var taskMenu by remember { mutableStateOf(false) }
    // 扫码与文件选择属于平台能力；业务校验和提交仍由 ViewModel/仓库完成。
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result -> result.contents?.let(viewModel::checkIn) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { photoUri = it }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Text("On-site actions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        state.errorMessage?.let { AssistChip(onClick = viewModel::clearError, label = { Text(it) }) }
        state.actionMessage?.let { AssistChip(onClick = viewModel::clearActionMessage, label = { Text(it) }) }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row { Icon(Icons.Rounded.QrCodeScanner, null); Text(" QR check-in", fontWeight = FontWeight.SemiBold) }
                Text("Scan the checkpoint code assigned to your current shift. Repeated scans are safely treated as already checked in.")
                Button(onClick = { scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Scan shift checkpoint").setBeepEnabled(false)) }, enabled = !state.isSubmittingWorkflow) { Text("Scan QR code") }
                if (viewModel.isDemoMode) TextButton(onClick = { viewModel.checkIn("arena-checkin:v1:${viewModel.shift.id}:00000000-0000-0000-0000-000000000001:demo-token") }) { Text("Use demo QR") }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row { Icon(Icons.Rounded.ReportProblem, null); Text(" Incident report", fontWeight = FontWeight.SemiBold) }
                Box { OutlinedButton(onClick = { categoryMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("Category: ${category.name.lowercase().replaceFirstChar(Char::uppercase)}") }; DropdownMenu(categoryMenu, { categoryMenu = false }) { IncidentCategory.entries.forEach { item -> DropdownMenuItem({ Text(item.name.lowercase().replaceFirstChar(Char::uppercase)) }, { category = item; categoryMenu = false }) } } }
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Box { OutlinedButton(onClick = { locationMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("Location: ${viewModel.locations.firstOrNull { it.id == locationId }?.name ?: "None"}") }; DropdownMenu(locationMenu, { locationMenu = false }) { DropdownMenuItem({ Text("None") }, { locationId = null; locationMenu = false }); viewModel.locations.forEach { location -> DropdownMenuItem({ Text(location.name) }, { locationId = location.id; locationMenu = false }) } } }
                Box { OutlinedButton(onClick = { taskMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("Task: ${state.tasks.firstOrNull { it.id == taskId }?.title ?: "None"}") }; DropdownMenu(taskMenu, { taskMenu = false }) { DropdownMenuItem({ Text("None") }, { taskId = null; taskMenu = false }); state.tasks.forEach { task -> DropdownMenuItem({ Text(task.title) }, { taskId = task.id; taskMenu = false }) } } }
                OutlinedButton(onClick = { photoPicker.launch("image/*") }) { Icon(Icons.Rounded.CameraAlt, null); Text(if (photoUri == null) " Choose optional photo" else " Photo selected") }
                Button(onClick = {
                    val uri = photoUri
                    val bytes = uri?.let { context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() } }
                    val mime = uri?.let(context.contentResolver::getType)
                    viewModel.submitIncident(IncidentDraft(category, description.trim(), locationId, taskId), bytes, mime)
                }, enabled = description.trim().length >= 3 && !state.isSubmittingWorkflow, modifier = Modifier.fillMaxWidth()) { Text(if (state.isSubmittingWorkflow) "Submitting…" else "Submit incident") }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
