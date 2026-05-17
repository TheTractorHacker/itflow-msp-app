package com.foleyit.itflow.ui.screens.tickets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    LaunchedEffect(selectedTab, mineOnly, search) { load() }

    Column(Modifier.fillMaxSize()) {
        // Search
        SearchBar(
            query = search,
            onQueryChange = { search = it },
            onSearch = { load() },
            active = false,
            onActiveChange = {},
            placeholder = { Text("Search tickets…") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingContent = {
                FilterChip(selected = mineOnly, onClick = { mineOnly = !mineOnly },
                    label = { Text("Mine") })
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {}

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Open") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Closed") })
        }

        when {
            state == null -> LoadingScreen()
            state!!.isFailure -> ErrorScreen(state!!.exceptionOrNull()?.message ?: "", onRetry = ::load)
            else -> {
                val tickets = state!!.getOrThrow().data
                if (tickets.isEmpty()) {
                    EmptyScreen("No tickets found", Icons.Outlined.ConfirmationNumber)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tickets) { t ->
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
                Row {
                    Text("#${ticket.number}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    ticket.status?.let {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraSmall) {
                            Text(it, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(ticket.subject, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Row {
                    Icon(Icons.Outlined.Business, null, modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(4.dp))
                    Text(ticket.client ?: "", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                    ticket.assignedTo?.let {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.Person, null, modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.width(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            Icon(Icons.Outlined.ChevronRight, null,
                tint = MaterialTheme.colorScheme.outline)
        }
    }
}
