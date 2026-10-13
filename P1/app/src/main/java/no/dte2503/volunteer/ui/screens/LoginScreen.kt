package no.dte2503.volunteer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import no.dte2503.volunteer.AppUiState
import no.dte2503.volunteer.ui.theme.Glacier

@Composable
fun LoginScreen(state: AppUiState, onLogin: (String, String) -> Unit, onDemo: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Spacer(Modifier.height(12.dp))
            ArcticHero {
                Icon(Icons.Rounded.Landscape, null, Modifier.size(44.dp), tint = Glacier)
                Text("NARVIK 2029", style = MaterialTheme.typography.labelLarge, color = Glacier)
                Text("Arena\nCompanion", style = MaterialTheme.typography.headlineLarge)
                Text("Your team. Your shift.\nA warm welcome to the Arctic.", style = MaterialTheme.typography.bodyLarge)
                Text("ALPINE WORLD SKI CHAMPIONSHIPS", style = MaterialTheme.typography.labelSmall)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Made for the people\nbehind the event.", style = MaterialTheme.typography.headlineMedium)
                Text("Find your next task, get your bearings and stay connected with your team.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.hasBackend) {
                Text("Already on the team?", style = MaterialTheme.typography.titleMedium)
                Text("Use the volunteer account provided by your coordinator.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(email, { email = it }, label = { Text("Volunteer email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth().testTag("sign_in_email"))
                OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().testTag("sign_in_password"))
                Button(onClick = { onLogin(email, password) }, enabled = email.isNotBlank() && password.isNotBlank() && !state.isLoading, modifier = Modifier.fillMaxWidth().testTag("sign_in_submit")) { Text(if (state.isLoading) "Signing in…" else "Sign in to your team") }
            }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = onDemo, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("explore_demo")) { Text("Explore the volunteer demo", fontWeight = FontWeight.SemiBold) }
            Text("Course prototype · Fictional shifts and meeting points. Demo messages and reports are saved on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }
    }
}
