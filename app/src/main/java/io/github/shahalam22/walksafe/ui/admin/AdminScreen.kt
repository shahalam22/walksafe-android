package io.github.shahalam22.walksafe.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.shahalam22.walksafe.data.model.SignedInUser
import io.github.shahalam22.walksafe.ui.admin.server.ServerScreen
import io.github.shahalam22.walksafe.ui.admin.sessions.SessionDetailScreen
import io.github.shahalam22.walksafe.ui.admin.sessions.SessionsScreen
import io.github.shahalam22.walksafe.ui.admin.setup.SetupScreen
import io.github.shahalam22.walksafe.ui.admin.users.UsersScreen
import kotlinx.serialization.Serializable

@Serializable data object SessionsRoute
@Serializable data class SessionDetailRoute(val sessionId: String)
@Serializable data object UsersRoute
@Serializable data object ServerRoute
@Serializable data object SetupRoute

private data class Tab(val route: Any, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(SessionsRoute, "Sessions", Icons.AutoMirrored.Filled.List),
    Tab(UsersRoute, "Users", Icons.Filled.Person),
    Tab(ServerRoute, "Server", Icons.Filled.Settings),
    Tab(SetupRoute, "Phone setup", Icons.Filled.Info),
)

/** The admin dashboard: session data, blind-user accounts, the server address, setup steps. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(user: SignedInUser, onOpenGuide: () -> Unit, onSignOut: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("WalkSafe Admin")
                        Text(user.email, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenGuide) { Icon(Icons.Filled.PlayArrow, "Open the guidance screen") }
                    IconButton(onClick = onSignOut) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Sign out") }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                TABS.forEach { tab ->
                    val selected = destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true ||
                        (tab.route == SessionsRoute && destination?.hasRoute(SessionDetailRoute::class) == true)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = SessionsRoute, modifier = Modifier.padding(padding)) {
            composable<SessionsRoute> {
                SessionsScreen(onOpenSession = { nav.navigate(SessionDetailRoute(it)) })
            }
            composable<SessionDetailRoute> { SessionDetailScreen(onBack = { nav.popBackStack() }) }
            composable<UsersRoute> { UsersScreen() }
            composable<ServerRoute> { ServerScreen() }
            composable<SetupRoute> { SetupScreen() }
        }
    }
}
