package com.foleyit.itflow.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*

sealed class Screen(val route: String) {
    object Setup       : Screen("setup")
    object Login       : Screen("login")
    object Dashboard   : Screen("dashboard")
    object Tickets     : Screen("tickets")
    object TicketDetail: Screen("tickets/{id}") { fun go(id: Int) = "tickets/$id" }
    object Clients     : Screen("clients")
    object ClientDetail: Screen("clients/{id}") { fun go(id: Int) = "clients/$id" }
    object Assets      : Screen("assets")
    object AssetDetail : Screen("assets/{id}") { fun go(id: Int) = "assets/$id" }
    object Credentials : Screen("credentials")
    object CredDetail  : Screen("credentials/{id}") { fun go(id: Int) = "credentials/$id" }
    object Quotes      : Screen("quotes")
    object QuoteDetail : Screen("quotes/{id}") { fun go(id: Int) = "quotes/$id" }
    object Invoices    : Screen("invoices")
    object InvoiceDetail: Screen("invoices/{id}") { fun go(id: Int) = "invoices/$id" }
    object Expenses    : Screen("expenses")
    object AddExpense  : Screen("expenses/add")
    object Notifications: Screen("notifications")
    object Appointments: Screen("appointments")
    object SignWorksheet: Screen("worksheets/{id}/sign") { fun go(id: Int) = "worksheets/$id/sign" }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Dashboard", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
    BottomNavItem(Screen.Tickets, "Tickets", Icons.Outlined.ConfirmationNumber, Icons.Filled.ConfirmationNumber),
    BottomNavItem(Screen.Clients, "Clients", Icons.Outlined.Business, Icons.Filled.Business),
)
