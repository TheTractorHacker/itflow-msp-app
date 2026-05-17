package com.foleyit.itflow.ui.screens.tickets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    var state by remember { mutableStateOf<Result<TicketsResponse>?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            state = runCatching {
                ApiClient.service().getTickets(
                    status = if (selectedTab == 0) "open" else "closed",
                    mine = if (mineOnly) 1 else 0,
                    search = search
                )
            }
        }
    }

    LaunchedEffect(selectedTab, mineOnly) { load() }

    Column(Modifier.fillMaxSize()) {
        // Search + Mine filter
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search tickets…") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { search = ""; load() }) {
                            Icon(Icons.Outlined.Clear, "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = mineOnly,
                onClick = { mineOnly = !mineOnly },
                label = { Text("Mine") }
            )
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0; load() },
                text = { Text("Open") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1; load() },
                text = { Text("Closed") })
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
                        Text(due, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}
