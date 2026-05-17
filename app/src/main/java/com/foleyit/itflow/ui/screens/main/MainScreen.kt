package com.foleyit.itflow.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.*
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.data.local.AppPreferences
import com.foleyit.itflow.ui.navigation.*
import com.foleyit.itflow.ui.screens.appointments.AppointmentsScreen
import com.foleyit.itflow.ui.screens.worksheets.SignWorksheetScreen
import com.foleyit.itflow.ui.screens.worksheets.FillWorksheetScreen
import com.foleyit.itflow.ui.screens.profile.ProfileScreen
import com.foleyit.itflow.ui.screens.assets.*
import com.foleyit.itflow.ui.screens.clients.*
import com.foleyit.itflow.ui.screens.credentials.*
import com.foleyit.itflow.ui.screens.dashboard.DashboardScreen
import com.foleyit.itflow.ui.screens.expenses.*
import com.foleyit.itflow.ui.screens.invoices.*
import com.foleyit.itflow.ui.screens.notifications.NotificationsScreen
import com.foleyit.itflow.ui.screens.quotes.*
import com.foleyit.itflow.ui.screens.tickets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Routes that are "root" tabs — show the main AppBar
private val ROOT_ROUTES = setOf(
    Screen.Dashboard.route, Screen.Tickets.route, Screen.Clients.route,
    Screen.Assets.route, Screen.Appointments.route,
    Screen.Credentials.route, Screen.Quotes.route,
    Screen.Invoices.route, Screen.Expenses.route,
    Screen.Notifications.route, Screen.Profile.route
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(prefs: AppPreferences, onLoggedOut: () -> Unit) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var userName by remember { mutableStateOf("") }
    var showUserMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { userName = prefs.userName.first() ?: "" }

    val currentDest by navController.currentBackStackEntryAsState()
    val currentRoute = currentDest?.destination?.route
    val isRootScreen = currentRoute in ROOT_ROUTES

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(Screen.Dashboard.route) { inclusive = false }
            launchSingleTop = true
        }
    }

    Scaffold(
        topBar = {
            if (isRootScreen) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.SyncAlt, null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text("ITFlow MSP", style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                            Icon(Icons.Outlined.Notifications, "Notifications")
                        }
                        Box {
                            IconButton(onClick = { showUserMenu = true }) {
                                Icon(Icons.Outlined.AccountCircle, "Account")
                            }
                            DropdownMenu(expanded = showUserMenu, onDismissRequest = { showUserMenu = false }) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(userName, style = MaterialTheme.typography.labelLarge)
                                            Text("Signed in", style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline)
                                        }
                                    },
                                    onClick = { showUserMenu = false; navController.navigate(Screen.Profile.route) },
                                    leadingIcon = { Icon(Icons.Outlined.Person, null) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Sign out") },
                                    onClick = {
                                        showUserMenu = false
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                try { ApiClient.service().logout() } catch (_: Exception) {}
                                            }
                                            prefs.clearAuth()
                                            ApiClient.clearToken()
                                            onLoggedOut()
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.Logout, null) }
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar {
                val hierarchy = currentDest?.destination?.hierarchy
                bottomNavItems.forEach { item ->
                    val selected = hierarchy?.any { it.route == item.screen.route } == true ||
                                   (currentRoute == item.screen.route)
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigateToTab(item.screen.route) },
                        icon = { Icon(if (selected) item.selectedIcon else item.icon, item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController,
            startDestination = Screen.Dashboard.route,
            modifier = if (isRootScreen) Modifier.padding(padding) else Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(navController) }
            composable(Screen.Tickets.route) { TicketsScreen(navController) }
            composable(Screen.TicketDetail.route) {
                TicketDetailScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0, navController)
            }
            composable(Screen.Clients.route) { ClientsScreen(navController) }
            composable(Screen.ClientDetail.route) {
                ClientDetailScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0, navController)
            }
            composable(Screen.Assets.route) { AssetsScreen(navController) }
            composable(Screen.AssetDetail.route) {
                AssetDetailScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0)
            }
            composable(Screen.Appointments.route) { AppointmentsScreen(navController) }
            composable(Screen.Credentials.route) { CredentialsScreen(navController) }
            composable(Screen.CredDetail.route) {
                CredentialDetailScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0)
            }
            composable(Screen.Quotes.route) { QuotesScreen(navController) }
            composable(Screen.QuoteDetail.route) {
                QuoteDetailScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0)
            }
            composable(Screen.Invoices.route) { InvoicesScreen(navController) }
            composable(Screen.InvoiceDetail.route) {
                InvoiceDetailScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0)
            }
            composable(Screen.Expenses.route) { ExpensesScreen(navController) }
            composable(Screen.AddExpense.route) { AddExpenseScreen { navController.popBackStack() } }
            composable(Screen.Notifications.route) { NotificationsScreen() }
            composable(Screen.Profile.route) { ProfileScreen(navController, prefs) }
            composable(Screen.FillWorksheet.route) {
                FillWorksheetScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0, navController)
            }
            composable(Screen.SignWorksheet.route) {
                SignWorksheetScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0, navController)
            }
        }
    }
}
