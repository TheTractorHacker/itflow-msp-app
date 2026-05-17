package com.foleyit.itflow.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.data.local.AppPreferences
import com.foleyit.itflow.ui.navigation.*
import com.foleyit.itflow.ui.screens.assets.*
import com.foleyit.itflow.ui.screens.clients.*
import com.foleyit.itflow.ui.screens.credentials.*
import com.foleyit.itflow.ui.screens.dashboard.DashboardScreen
import com.foleyit.itflow.ui.screens.expenses.*
import com.foleyit.itflow.ui.screens.invoices.*
import com.foleyit.itflow.ui.screens.notifications.NotificationsScreen
import com.foleyit.itflow.ui.screens.quotes.*
import com.foleyit.itflow.ui.screens.tickets.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(prefs: AppPreferences, onLoggedOut: () -> Unit) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var userName by remember { mutableStateOf("") }
    var showMoreSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { userName = prefs.userName.first() ?: "" }

    val currentDest by navController.currentBackStackEntryAsState()

    val inTopLevel = bottomNavItems.any {
        currentDest?.destination?.hierarchy?.any { d -> d.route == it.screen.route } == true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
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
                    IconButton(onClick = {
                        scope.launch {
                            try { ApiClient.service().logout() } catch (_: Exception) {}
                            prefs.clearAuth()
                            ApiClient.clearToken()
                            onLoggedOut()
                        }
                    }) {
                        Icon(Icons.Outlined.AccountCircle, userName)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val hierarchy = currentDest?.destination?.hierarchy
                bottomNavItems.forEach { item ->
                    val selected = hierarchy?.any { it.route == item.screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(if (selected) item.selectedIcon else item.icon, item.label) },
                        label = { Text(item.label) }
                    )
                }
                // More button
                NavigationBarItem(
                    selected = false,
                    onClick = { showMoreSheet = true },
                    icon = { Icon(Icons.Outlined.GridView, "More") },
                    label = { Text("More") }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController, startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(padding)
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
        }
    }

    if (showMoreSheet) {
        ModalBottomSheet(onDismissRequest = { showMoreSheet = false }) {
            MoreSheetContent(
                onNavigate = { route ->
                    showMoreSheet = false
                    navController.navigate(route)
                }
            )
        }
    }
}

@Composable
private fun MoreSheetContent(onNavigate: (String) -> Unit) {
    val items = listOf(
        Triple(Icons.Outlined.Devices, "Assets", Screen.Assets.route),
        Triple(Icons.Outlined.Lock, "Credentials", Screen.Credentials.route),
        Triple(Icons.Outlined.RequestQuote, "Quotes", Screen.Quotes.route),
        Triple(Icons.Outlined.ReceiptLong, "Invoices", Screen.Invoices.route),
        Triple(Icons.Outlined.Receipt, "Expenses", Screen.Expenses.route),
        Triple(Icons.Outlined.Notifications, "Notifications", Screen.Notifications.route),
    )
    Column(Modifier.padding(16.dp)) {
        Text("More", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp))
        items.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (icon, label, route) ->
                    Surface(
                        onClick = { onNavigate(route) },
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            Icon(icon, label, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.height(8.dp))
                            Text(label, style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
