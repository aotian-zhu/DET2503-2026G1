package no.dte2503.volunteer.ui.screens

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.dte2503.volunteer.BuildConfig
import no.dte2503.volunteer.MainViewModel
import no.dte2503.volunteer.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceChecksScreen(model: MainViewModel, onBack: () -> Unit) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val version = BuildConfig.VERSION_NAME
    val device = "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    val current = state.deviceChecks.values.filter { it.appVersion == version }
    Column(Modifier.fillMaxSize().testTag("device_checks_screen")) {
        TopAppBar(title = { Text("Device test log") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        })
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize().testTag("device_checks_list")) {
            item {
                InfoCard(highlighted = true) {
                    Text("${current.size} of ${deviceTestScenarios.size} scenarios recorded", style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("device_check_summary"))
                    Text("${current.count { it.passed }} passed · ${current.count { !it.passed }} with an issue · Arena Companion $version")
                    Text(device, style = MaterialTheme.typography.bodySmall)
                    Text("Run each scenario first. Write what actually happened on this device. These are your manual observations; untouched tests remain Not run.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Arena Companion $version — device test report")
                            putExtra(Intent.EXTRA_TEXT, deviceTestReport(version, device, state.deviceChecks))
                        }
                        context.startActivity(Intent.createChooser(send, "Export device test report"))
                    }, modifier = Modifier.testTag("export_device_checks")) { Text("Export test report") }
                }
            }
            items(deviceTestScenarios, key = { it.id }) { scenario ->
                val recorded = state.deviceChecks[scenario.id]?.takeIf { it.appVersion == version }
                var observed by rememberSaveable(scenario.id, version) { mutableStateOf(recorded?.observed.orEmpty()) }
                var passed by rememberSaveable(scenario.id, version) { mutableStateOf(recorded?.passed ?: true) }
                InfoCard(modifier = Modifier.testTag("device_check_${scenario.id}")) {
                    Text(scenario.title, style = MaterialTheme.typography.titleMedium)
                    Text(if (recorded == null) "Not run" else if (recorded.passed) "Recorded: Pass" else "Recorded: Issue", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                    Text("Do: ${scenario.steps}", style = MaterialTheme.typography.bodyMedium)
                    Text("Expected: ${scenario.expected}", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(value = observed, onValueChange = { if (it.length <= 2000) observed = it }, label = { Text("What did you observe?") }, minLines = 2, modifier = Modifier.fillMaxWidth().testTag("observed_${scenario.id}"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = passed, onClick = { passed = true }, label = { Text("Pass") })
                        FilterChip(selected = !passed, onClick = { passed = false }, label = { Text("Issue") })
                    }
                    Button(onClick = { model.saveDeviceCheck(scenario.id, observed, passed) }, enabled = observed.trim().length >= 3, modifier = Modifier.testTag("save_check_${scenario.id}")) { Text("Save observed result") }
                }
            }
        }
    }
}
