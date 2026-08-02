package com.foleyit.itflow.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.*
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.data.local.AppPreferences
import com.foleyit.itflow.ui.components.OfflineBanner
import com.foleyit.itflow.ui.navigation.*
import com.foleyit.itflow.ui.util.rememberIsOnline
import com.foleyit.itflow.ui.screens.appointments.AppointmentsScreen
import com.foleyit.itflow.ui.screens.worksheets.FillWorksheetScreen
import com.foleyit.itflow.ui.screens.worksheets.OuttakeSignScreen
import com.foleyit.itflow.ui.screens.profile.ProfileScreen
import com.foleyit.itflow.ui.screens.assets.*
import com.foleyit.itflow.ui.screens.clients.*
import com.foleyit.itflow.ui.screens.credentials.*
import com.foleyit.itflow.ui.screens.dashboard.DashboardScreen
import com.foleyit.itflow.ui.screens.expenses.*
import com.foleyit.itflow.ui.screens.invoices.*
import com.foleyit.itflow.ui.screens.kb.KbArticleDetailScreen
import com.foleyit.itflow.ui.screens.kb.KnowledgeBaseScreen
import com.foleyit.itflow.ui.screens.notifications.NotificationsScreen
import com.foleyit.itflow.ui.screens.alerts.AlertsScreen
import com.foleyit.itflow.ui.screens.quotes.*
import com.foleyit.itflow.ui.screens.tickets.*
import com.foleyit.itflow.ui.screens.search.SearchScreen
import com.foleyit.itflow.ui.screens.reports.*
import com.foleyit.itflow.ui.screens.scan.ScanBarcodeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Routes that show the main ITFlow MSP AppBar
private val ROOT_ROUTES = setOf(
    Screen.Dashboard.route, Screen.Tickets.route, Screen.Clients.route,
    Screen.Assets.route, Screen.Appointments.route,
    Screen.Credentials.route, Screen.Quotes.route,
    Screen.Invoices.route, Screen.Expenses.route,
    Screen.Notifications.route, Screen.Alerts.route
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    prefs: AppPreferences,
    onLoggedOut: () -> Unit,
    deepLinkRoute: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
    onChangeServer: () -> Unit = {}
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var showUserMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        userName = prefs.userName.first() ?: ""
        userEmail = prefs.userEmail.first() ?: ""
    }

    LaunchedEffect(deepLinkRoute) {
        deepLinkRoute?.let {
            if (DeepLinks.ALLOWED_ROUTE.matches(it)) {
                try {
                    navController.navigate(it) { launchSingleTop = true }
                } catch (_: IllegalArgumentException) {
                    // Unknown route — silently ignore rather than crash
                }
            }
            onDeepLinkConsumed()
        }
    }

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
                        IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                            Icon(Icons.Outlined.Search, "Search")
                        }
                        IconButton(onClick = { navController.navigate(Screen.Alerts.route) }) {
                            Icon(Icons.Outlined.Warning, "Alerts")
                        }
                        IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                            Icon(Icons.Outlined.Notifications, "Notifications")
                        }
                        Box {
                            IconButton(onClick = { showUserMenu = true }) {
                                Icon(Icons.Outlined.AccountCircle, "Account")
                            }
                            DropdownMenu(
                                expanded = showUserMenu,
                                onDismissRequest = { showUserMenu = false },
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.width(280.dp)
                            ) {
                                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = MaterialTheme.shapes.extraLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(userName.ifBlank { "Account" }, style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold, maxLines = 1)
                                            Text(userEmail.ifBlank { "Signed in" }, style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline, maxLines = 1)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                DropdownMenuItem(
                                    text = { Text("View Profile") },
                                    onClick = { showUserMenu = false; navController.navigate(Screen.Profile.route) },
                                    leadingIcon = { Icon(Icons.Outlined.Person, null) }
                                )
                                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = { Text("Sign out", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showUserMenu = false
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                try { ApiClient.service().registerFcmToken(com.foleyit.itflow.data.api.FcmTokenRequest("")) } catch (_: Exception) {}
                                                try { ApiClient.service().logout() } catch (_: Exception) {}
                                            }
                                            prefs.clearAuth()
                                            ApiClient.clearToken()
                                            onLoggedOut()
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Outlined.Logout, null,
                                            tint = MaterialTheme.colorScheme.error)
                                    }
                                )
                                Spacer(Modifier.height(4.dp))
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
        val isOnline by rememberIsOnline()
        Column(if (isRootScreen) Modifier.padding(padding) else Modifier.padding(bottom = padding.calculateBottomPadding())) {
        if (!isOnline) {
            OfflineBanner()
        }
        NavHost(
            navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.weight(1f)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(navController) }
            composable(Screen.Tickets.route) { TicketsScreen(navController) }
            composable(Screen.TicketDetail.route) {
                TicketDetailScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0, navController)
            }
            composable(Screen.TicketChat.route) {
                TicketChatScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0, navController)
            }
            composable(Screen.Clients.route) { ClientsScreen(navController) }
            composable(Screen.ClientDetail.route) {
                ClientDetailScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0, navController)
            }
            composable(Screen.Assets.route) { AssetsScreen(navController) }
            composable(Screen.AssetDetail.route) {
                AssetDetailScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0, navController)
            }
            composable(Screen.Appointments.route) { AppointmentsScreen(navController) }
            composable(Screen.Credentials.route) { CredentialsScreen(navController) }
            composable(Screen.CredDetail.route) {
                CredentialDetailScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0, navController)
            }
            composable(Screen.Quotes.route) { QuotesScreen(navController) }
            composable(Screen.QuoteDetail.route) {
                QuoteDetailScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0, navController)
            }
            composable(Screen.Invoices.route) { InvoicesScreen(navController) }
            composable(Screen.InvoiceDetail.route) {
                InvoiceDetailScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0, navController)
            }
            composable(Screen.Expenses.route) { ExpensesScreen(navController) }
            composable(Screen.AddExpense.route) { AddExpenseScreen { navController.popBackStack() } }
            composable(Screen.Notifications.route) { NotificationsScreen() }
            composable(Screen.Alerts.route) { AlertsScreen(navController) }
            composable(Screen.Profile.route) { ProfileScreen(navController, prefs, onChangeServer, onLoggedOut) }
            composable(Screen.KnowledgeBase.route) { KnowledgeBaseScreen(navController) }
            composable(Screen.KbArticleDetail.route) {
                KbArticleDetailScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0, navController)
            }
            composable(Screen.FillWorksheet.route) {
                FillWorksheetScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0, navController)
            }
            composable(Screen.OuttakeSign.route) {
                OuttakeSignScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0, navController)
            }
            composable(Screen.CreateTicket.route) { CreateTicketScreen(navController) }
            composable(Screen.Search.route) { SearchScreen(navController) }
            composable(Screen.TimeReport.route) { TimeSummaryScreen(navController) }
            composable(Screen.ReportsHub.route) { ReportsHubScreen(navController) }
            composable(Screen.TicketVolumeReport.route) { TicketVolumeReportScreen(navController) }
            composable(Screen.TicketsByClientReport.route) { TicketsByClientReportScreen(navController) }
            composable(Screen.TimeByTechReport.route) { TimeByTechReportScreen(navController) }
            composable(Screen.TechPerformanceReport.route) { TechPerformanceReportScreen(navController) }
            composable(Screen.OverviewReport.route) { OverviewReportScreen(navController) }
            composable(Screen.UnbilledTicketsReport.route) { UnbilledTicketsReportScreen(navController) }
            composable(Screen.ClientsWithBalanceReport.route) { ClientsWithBalanceReportScreen(navController) }
            composable(Screen.IncomeSummaryReport.route) { IncomeSummaryReportScreen(navController) }
            composable(Screen.ExpenseSummaryReport.route) { ExpenseSummaryReportScreen(navController) }
            composable(Screen.ProfitLossReport.route) { ProfitLossReportScreen(navController) }
            composable(Screen.ExpiringReport.route) { ExpiringReportScreen(navController) }
            composable(Screen.CsatReport.route) { CsatReportScreen(navController) }
            composable(Screen.RmmHealthReport.route) { RmmHealthReportScreen(navController) }
            composable(Screen.ServiceDeskReport.route) { ServiceDeskReportScreen(navController) }
            composable(Screen.TechUtilizationReport.route) { TechnicianUtilizationReportScreen(navController) }
            composable(Screen.ScanBarcode.route) { ScanBarcodeScreen(navController) }
        }
        }
    }
}
