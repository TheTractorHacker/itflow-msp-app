package com.foleyit.itflow.ui.screens.main

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.*
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.data.local.AppPreferences
import com.foleyit.itflow.data.local.WorkClock
import com.foleyit.itflow.ui.components.AppDrawerContent
import com.foleyit.itflow.ui.components.BrandMark
import com.foleyit.itflow.ui.components.FloatingBottomNavBar
import com.foleyit.itflow.ui.components.OfflineBanner
import com.foleyit.itflow.ui.components.UnreadDot
import com.foleyit.itflow.ui.components.WorkClockBar
import com.foleyit.itflow.ui.navigation.*
import com.foleyit.itflow.ui.theme.Motion
import com.foleyit.itflow.ui.theme.ThemeMode
import com.foleyit.itflow.ui.util.rememberIsOnline
import com.foleyit.itflow.ui.screens.appointments.AppointmentsScreen
import com.foleyit.itflow.ui.screens.worksheets.FillWorksheetScreen
import com.foleyit.itflow.ui.screens.worksheets.OuttakeSignScreen
import com.foleyit.itflow.ui.screens.profile.ProfileScreen
import com.foleyit.itflow.ui.screens.assets.*
import com.foleyit.itflow.ui.screens.projects.*
import com.foleyit.itflow.ui.screens.contracts.*
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

// Routes that show the main ITFlow MSP AppBar. Screens that draw their own TopAppBar (with a
// Back button) — Credentials, Quotes, Invoices, Expenses, Notifications — must not be listed
// here, or they stack a second bar beneath the brand bar.
private val ROOT_ROUTES = setOf(
    Screen.Dashboard.route, Screen.Tickets.route, Screen.Clients.route,
    Screen.Assets.route, Screen.Projects.route, Screen.Contracts.route, Screen.Appointments.route,
    Screen.Alerts.route
)

// The 5 true root screens — only these show the floating bottom nav (the rest of ROOT_ROUTES
// gets the brand top bar but is reached via the drawer, not a persistent tab).
private val BOTTOM_NAV_ROUTES = bottomNavItems.map { it.screen.route }.toSet()

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
    val appContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) { WorkClock.attach(appContext) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var hasUnreadNotifications by remember { mutableStateOf(false) }

    var showSignOutConfirm by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Android 13+ requires the runtime POST_NOTIFICATIONS grant before push alerts are shown.
    // Ask once, right after sign-in lands here; if it is declined, say what that costs and offer
    // a shortcut to the system notification settings (the system won't re-prompt after a deny).
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) scope.launch {
            val result = snackbar.showSnackbar(
                message = "Notifications are off, so you won't get ticket alerts.",
                actionLabel = "Settings",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                })
            }
        }
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !prefs.notificationPrompted.first()
        ) {
            prefs.setNotificationPrompted()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Back should dismiss an open navigation drawer before it is allowed to leave the app.
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    val themeMode by prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val isDarkMode = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> isSystemInDarkTheme()
    }

    LaunchedEffect(Unit) {
        userName = prefs.userName.first() ?: ""
        userEmail = prefs.userEmail.first() ?: ""
        try {
            hasUnreadNotifications = ApiClient.service().getDashboard().unread > 0
        } catch (_: Exception) {
            // Chrome badge only — a failed fetch here shouldn't block rendering the screen.
        }
    }

    fun closeDrawerAndNavigate(route: String) {
        scope.launch { drawerState.close() }
        navController.navigate(route) { launchSingleTop = true }
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

    fun signOut() {
        scope.launch {
            // Best-effort server revocation, bounded so an unreachable server cannot block sign-out.
            withContext(Dispatchers.IO) {
                kotlinx.coroutines.withTimeoutOrNull(8_000L) {
                    try { ApiClient.service().registerFcmToken(com.foleyit.itflow.data.api.FcmTokenRequest("")) } catch (_: Exception) {}
                    try { ApiClient.service().logout() } catch (_: Exception) {}
                }
            }
            // Local state is always cleared, even if the server call failed.
            try { prefs.clearAuth() } finally {
                WorkClock.clear()
                ApiClient.clearToken()
                onLoggedOut()
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign Out?") },
            text = { Text("You'll need to sign in again to access your account.") },
            confirmButton = {
                TextButton(onClick = { showSignOutConfirm = false; signOut() }) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showSignOutConfirm = false }) { Text("Cancel") } }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Edge-swipe opens the drawer only on top-level screens; on detail screens (articles, tickets) it
        // competes with scrolling and horizontal swipes, and Back is the way out there anyway.
        gesturesEnabled = isRootScreen || drawerState.isOpen,
        drawerContent = {
            AppDrawerContent(
                userName = userName,
                userEmail = userEmail,
                hasUnreadNotifications = hasUnreadNotifications,
                isDarkMode = isDarkMode,
                onToggleDarkMode = { dark -> scope.launch { prefs.setThemeMode(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) } },
                onNavigate = ::closeDrawerAndNavigate,
                onSignOut = { scope.launch { drawerState.close() }; showSignOutConfirm = true },
            )
        }
    ) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (isRootScreen) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Outlined.Menu, "Menu")
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val brand = com.foleyit.itflow.ui.branding.LocalCompanyBranding.current
                            // A server without an uploaded logo gets its name only (no duplicate text mark).
                            if (brand.bitmap != null || !brand.fromServer) {
                                BrandMark(size = 28.dp)
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(brand.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                    },
                    actions = {
                        // Search lives in the drawer and on Home; dropping it here gives the
                        // company name room instead of truncating to "Northwind Ma…".
                        IconButton(onClick = { navController.navigate(Screen.Alerts.route) }) {
                            Icon(Icons.Outlined.Warning, "Alerts")
                        }
                        IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                            Box {
                                Icon(Icons.Outlined.Notifications, "Notifications")
                                if (hasUnreadNotifications) {
                                    UnreadDot(Modifier.align(Alignment.TopEnd))
                                }
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            Column {
                WorkClockBar(
                    hideOnTicketId = if (currentRoute == Screen.TicketDetail.route)
                        currentDest?.arguments?.getString("id")?.toIntOrNull() else null,
                    onOpenTicket = { id ->
                        navController.navigate(Screen.TicketDetail.go(id)) { launchSingleTop = true }
                    },
                )
                if (currentRoute in BOTTOM_NAV_ROUTES) {
                    val hierarchy = currentDest?.destination?.hierarchy
                    FloatingBottomNavBar(
                        items = bottomNavItems,
                        isSelected = { item ->
                            hierarchy?.any { it.route == item.screen.route } == true || currentRoute == item.screen.route
                        },
                        onSelect = ::navigateToTab,
                    )
                }
            }
        }
    ) { padding ->
        val isOnline by rememberIsOnline()
        // consumeWindowInsets: inner screens' own Scaffold/TopAppBar would otherwise add the
        // status-bar inset a second time on top of this Scaffold's padding (a ~136px gap).
        Column(Modifier.padding(padding).consumeWindowInsets(padding)) {
        if (!isOnline) {
            OfflineBanner()
        }
        NavHost(
            navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.weight(1f),
            // Fade + subtle rise (design reference's springy screen-transition motion) applied
            // once here, at the NavHost level, so every one of the ~35 destinations below gets
            // it uniformly without each composable() call needing its own animation params.
            enterTransition = {
                fadeIn(animationSpec = Motion.medium()) +
                    slideInVertically(animationSpec = Motion.medium()) { it / 20 }
            },
            exitTransition = { fadeOut(animationSpec = Motion.fast()) },
            popEnterTransition = {
                fadeIn(animationSpec = Motion.medium()) +
                    slideInVertically(animationSpec = Motion.medium()) { -it / 20 }
            },
            popExitTransition = { fadeOut(animationSpec = Motion.fast()) },
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
            composable(Screen.Projects.route) { ProjectsScreen(navController) }
            composable(Screen.ProjectDetail.route) {
                ProjectDetailScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0, navController)
            }
            composable(Screen.Contracts.route) { ContractsScreen(navController) }
            composable(Screen.ContractDetail.route) {
                ContractDetailScreen(it.arguments?.getString("id")?.toIntOrNull() ?: 0, navController)
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
}
