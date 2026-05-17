package com.foleyit.itflow.ui.screens.tickets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.data.api.TicketSummary
import com.foleyit.itflow.data.api.TicketsResponse
import com.foleyit.itflow.ui.components.EmptyScreen
import com.foleyit.itflow.ui.components.ErrorScreen
import com.foleyit.itflow.ui.components.LoadingScreen
import com.foleyit.itflow.ui.navigation.Screen
import com.foleyit.itflow.ui.util.fmtDate
import kotlinx.coroutines.launch

private fun ticketStatusColor(hex: String?): Color = try {
    Color(android.graphics.Color.parseColor(if (hex?.startsWith("#") == true) hex else "#${hex}"))
} catch (_: Exception) { Color.Gray }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsScreen(navController: NavController) {
    var search by remember { mutableStateOf("") }
    var mineOnly by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var priorityFilter by remember { mutableStateOf<String?>(null) }
    var onsiteFilter by remember { mutableStateOf<Int?>(null) }  // null=all, 1=onsite, 0=remote
    var state by remember { mutableStateOf<Result<TicketsResponse>?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            state = runCatching {
                ApiClient.service().getTickets(
                    status = if (selectedTab == 0) "open" else "closed",
                    mine = if (mineOnly) 1 else 0,
                    search = search,
                    priority = priorityFilter?.takeIf { it.isNotBlank() },
                    onsite = onsiteFilter
                )
            }
        }
    }

    LaunchedEffect(selectedTab, mineOnly) { load() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.CreateTicket.route) }) {
                Icon(Icons.Outlined.Add, "New Ticket")
            }
        }
    ) { scaffoldPadding ->
    // Only use bottom padding — top is handled by the outer MainScreen Scaffold
    Column(Modifier.fillMaxSize().padding(bottom = scaffoldPadding.calculateBottomPadding())) {
        // Compact search + Mine filter
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.weight(1f).height(48.dp),
                placeholder = { Text("Search tickets…", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(18.dp)) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { search = ""; load() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Outlined.Clear, "Clear", Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                textStyle = MaterialTheme.typography.bodyMedium
            )
            FilterChip(
                selected = mineOnly,
                onClick = { mineOnly = !mineOnly },
                label = { Text("Mine", style = MaterialTheme.typography.labelMedium) }
            )
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0; load() },
                text = { Text("Open") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1; load() },
                text = { Text("Closed") })
        }

        // Scrollable filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Priority filters
            listOf(null to "All", "critical" to "Critical", "high" to "High",
                   "medium" to "Medium", "low" to "Low").forEach { (value, label) ->
                FilterChip(
                    selected = priorityFilter == value,
                    onClick = { priorityFilter = if (priorityFilter == value && value != null) null else value; load() },
                    label = { Text(label) }
                )
            }
            VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = 4.dp))
            FilterChip(
                selected = onsiteFilter == 1,
                onClick = { onsiteFilter = if (onsiteFilter == 1) null else 1; load() },
                label = { Text("On-Site") },
                leadingIcon = { Icon(Icons.Outlined.LocationOn, null, Modifier.size(14.dp)) }
            )
            FilterChip(
                selected = onsiteFilter == 0,
                onClick = { onsiteFilter = if (onsiteFilter == 0) null else 0; load() },
                label = { Text("Remote") },
                leadingIcon = { Icon(Icons.Outlined.Wifi, null, Modifier.size(14.dp)) }
            )
        }

        when {
            state == null -> LoadingScreen()
            state!!.isFailure -> ErrorScreen(state!!.exceptionOrNull()?.message ?: "Error", onRetry = ::load)
            else -> {
                val tickets = state!!.getOrThrow().data
                if (tickets.isEmpty()) {
                    EmptyScreen("No tickets found", Icons.Outlined.ConfirmationNumber)
                } else if (selectedTab == 0) {
                    // Open tickets — grouped by status
                    val statusOrder = tickets.map { it.status ?: "Unknown" }.distinct()
                    val grouped = tickets.groupBy { it.status ?: "Unknown" }
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                        statusOrder.forEach { status ->
                            val group = grouped[status] ?: return@forEach
                            val color = group.firstOrNull()?.statusColor
                            item(key = "header_$status") {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .padding(top = 12.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = ticketStatusColor(color),
                                        shape = MaterialTheme.shapes.extraSmall,
                                        modifier = Modifier.size(10.dp)
                                    ) {}
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "$status  ·  ${group.size}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            items(group, key = { it.id }) { t ->
                                TicketCard(t) { navController.navigate(Screen.TicketDetail.go(t.id)) }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                } else {
                    // Closed tickets — simple flat list
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tickets, key = { it.id }) { t ->
                            TicketCard(t) { navController.navigate(Screen.TicketDetail.go(t.id)) }
                        }
                    }
                }
            }
        }
    }
    } // end Scaffold
}

@Composable
fun TicketCard(ticket: TicketSummary, onClick: () -> Unit) {
    val priorityColor = when (ticket.priority?.lowercase()) {
        "critical" -> MaterialTheme.colorScheme.error
        "high"     -> Color(0xFFE65100)
        "medium"   -> Color(0xFFF9A825)
        else       -> MaterialTheme.colorScheme.outline
    }
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.padding(16.dp)) {
            Surface(
                color = priorityColor,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.width(4.dp).height(64.dp)
            ) {}
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${ticket.number}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                    ticket.assignedTo?.let { assigned ->
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.Person, null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.width(2.dp))
                        Text(assigned, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(ticket.subject,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Business, null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(4.dp))
                    Text(ticket.client ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                    ticket.dueAt?.let { due ->
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.Schedule, null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.width(2.dp))
                        Text(fmtDate(due), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}
