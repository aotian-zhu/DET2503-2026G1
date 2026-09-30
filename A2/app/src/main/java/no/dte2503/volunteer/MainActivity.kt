package no.dte2503.volunteer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import no.dte2503.volunteer.ui.screens.HomeScreen
import no.dte2503.volunteer.ui.screens.InboxScreen
import no.dte2503.volunteer.ui.screens.LoginScreen
import no.dte2503.volunteer.ui.screens.MapScreen
import no.dte2503.volunteer.ui.screens.ProfileScreen
import no.dte2503.volunteer.ui.screens.TasksScreen
import no.dte2503.volunteer.ui.screens.WorkflowScreen
import no.dte2503.volunteer.ui.theme.VolunteerHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { VolunteerHubTheme { VolunteerApp() } }
    }
}

private data class Destination(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    Destination("home", "Home", Icons.Rounded.Home),
    Destination("map", "Map", Icons.Rounded.Map),
    Destination("tasks", "Tasks", Icons.Rounded.TaskAlt),
    Destination("inbox", "Inbox", Icons.Rounded.Notifications),
    Destination("profile", "Profile", Icons.Rounded.Person),
)

// 顶层目的地复用同一返回栈并恢复页面状态，避免底栏切换不断叠加页面。
private fun NavHostController.openTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolunteerApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    // 登录态是整个应用壳的门控：未认证时不创建主导航，也不会暴露业务页面。
    if (!state.isLoggedIn) {
        LoginScreen(
            isLoading = state.isLoading,
            errorMessage = state.errorMessage,
            isDemoMode = viewModel.isDemoMode,
            onLogin = viewModel::login,
        )
        return
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val topBarTitle = when (currentRoute) {
        "home" -> "Home"
        "tasks" -> "Tasks"
        "inbox" -> "Inbox"
        else -> null
    }
    Scaffold(
        topBar = {
            if (topBarTitle != null) {
                CenterAlignedTopAppBar(
                    title = { Text(topBarTitle) },
                    actions = {
                        IconButton(onClick = {
                            navController.navigate("actions") { launchSingleTop = true }
                        }) {
                            Icon(Icons.Rounded.QrCodeScanner, "Open on-site actions")
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (currentRoute != "actions") NavigationBar(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            if (destination.route == "inbox" && currentRoute == "inbox") viewModel.openConversation(null)
                            if (currentRoute != destination.route) {
                                navController.openTopLevel(destination.route)
                            }
                        },
                        icon = { Icon(destination.icon, destination.label) },
                        label = { Text(destination.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.surface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    ) { innerPadding ->
        // Activity 只负责编排跨页面意图；业务状态与副作用统一交给同一个 ViewModel。
        NavHost(navController, startDestination = "home", modifier = androidx.compose.ui.Modifier.padding(innerPadding)) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenTasks = { navController.openTopLevel("tasks") },
                    onOpenFunctions = { navController.openTopLevel("profile") },
                )
            }
            composable("map") { MapScreen(viewModel, state.tasks, state.currentPosition, state.locationPermissionDenied, state.selectedTaskId) }
            composable("tasks") {
                TasksScreen(
                    viewModel = viewModel,
                    tasks = state.tasks,
                    onContactCoordinator = { taskId ->
                        viewModel.openConversation(taskId)
                        navController.openTopLevel("inbox")
                    },
                    onViewOnMap = { taskId ->
                        viewModel.selectTask(taskId)
                        navController.openTopLevel("map")
                    },
                )
            }
            composable("actions") { WorkflowScreen(viewModel, onBack = { navController.popBackStack() }) }
            composable("inbox") { InboxScreen(viewModel) }
            composable("profile") { ProfileScreen(viewModel, viewModel::logout) }
        }
    }
}
