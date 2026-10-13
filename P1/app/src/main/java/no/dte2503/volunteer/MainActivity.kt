package no.dte2503.volunteer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import no.dte2503.volunteer.ui.screens.*
import no.dte2503.volunteer.ui.theme.VolunteerHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: MainViewModel = viewModel()
            val state by model.uiState.collectAsStateWithLifecycle()
            VolunteerHubTheme(state.themeMode) {
                val lightBars = MaterialTheme.colorScheme.background.luminance() > .5f
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = lightBars
                        isAppearanceLightNavigationBars = lightBars
                    }
                }
                VolunteerApp(model, state)
            }
        }
    }
}

private data class Destination(val route: String, val label: String, val title: String, val icon: ImageVector)
private val destinations = listOf(
    Destination("home", "Today", "Your day", Icons.Rounded.WbSunny),
    Destination("tasks", "Tasks", "Your assignments", Icons.Rounded.Checklist),
    Destination("map", "Map", "Explore Narvik", Icons.Rounded.Map),
    Destination("inbox", "Inbox", "Team updates", Icons.Rounded.Forum),
    Destination("profile", "You", "Volunteer profile", Icons.Rounded.PersonOutline),
)
private fun NavHostController.openTopLevel(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolunteerApp(model: MainViewModel, state: AppUiState) {
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle, model) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) model.onResume() }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    if (!state.isInitialized) {
        Surface(Modifier.fillMaxSize()) { Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        return
    }
    if (!state.isLoggedIn) {
        LoginScreen(state, model::login, model::enterDemo)
        return
    }
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"
    val workflow = route.startsWith("actions")
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage, state.actionMessage) {
        val error = state.errorMessage
        val notice = state.actionMessage
        if (error != null) { snackbar.showSnackbar(error, duration = SnackbarDuration.Long); model.clearError() }
        else if (notice != null) { snackbar.showSnackbar(notice); model.clearActionMessage() }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (!workflow) Column {
                TopAppBar(
                    title = { Column {
                        Text("NARVIK 2029", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(destinations.firstOrNull { it.route == route }?.title ?: "Arena Companion", style = MaterialTheme.typography.titleLarge)
                    } },
                    actions = {
                        IconButton(onClick = { nav.navigate("actions/checkin") }, modifier = Modifier.testTag("open_actions")) { Icon(Icons.Rounded.QrCodeScanner, "Check in or report an issue") }
                        IconButton(onClick = { model.refresh() }, enabled = state.isOnline && !state.refreshing) {
                            if (state.refreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Rounded.Refresh, "Refresh updates and weather")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
                if (!state.isOnline || state.pendingCount > 0 || state.syncError != null) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (!state.isOnline) Icons.Rounded.CloudOff else Icons.Rounded.Sync, null, Modifier.size(16.dp))
                            Text(when {
                                !state.isOnline && state.pendingCount > 0 -> "Offline · ${state.pendingCount} updates waiting to sync"
                                !state.isOnline -> "Offline · Your saved shift is available"
                                state.syncError != null -> state.syncError
                                else -> "${state.pendingCount} saved updates waiting to sync"
                            }, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (!workflow) NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        modifier = Modifier.testTag("nav_${destination.route}"),
                        selected = route == destination.route,
                        onClick = {
                            if (destination.route == "inbox") model.openConversation(null)
                            nav.openTopLevel(destination.route)
                        },
                        icon = {
                            if (destination.route == "inbox" && state.announcements.any { !it.isRead }) BadgedBox(badge = { Badge() }) { Icon(destination.icon, null) }
                            else Icon(destination.icon, null)
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, "home", Modifier.padding(padding).imePadding()) {
            composable("home") { HomeScreen(model, { nav.openTopLevel("tasks") }, { nav.openTopLevel("profile") }, { taskId -> model.selectTask(taskId); nav.openTopLevel("map") }, { section -> nav.navigate("actions/$section") }) }
            composable("tasks") { TasksScreen(model, { taskId -> model.openConversation(taskId); nav.openTopLevel("inbox") }, { taskId -> model.selectTask(taskId); nav.openTopLevel("map") }) }
            composable("map") { MapScreen(model) }
            composable("inbox") { InboxScreen(model) }
            composable("profile") { ProfileScreen(model, model::logout) }
            composable("actions/{section}") { entry -> WorkflowScreen(model, entry.arguments?.getString("section") ?: "checkin") { nav.popBackStack() } }
        }
    }
}
