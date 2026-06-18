package com.foleyit.itflow.ui.screens.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.foleyit.itflow.data.api.AlertActionRequest
import com.foleyit.itflow.data.api.AlertItem
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.ui.components.EmptyScreen
import com.foleyit.itflow.ui.components.ErrorScreen
import com.foleyit.itflow.ui.components.LoadingScreen
import com.foleyit.itflow.ui.navigation.Screen
import kotlinx.coroutines.launch

private val STATUS_TABS = listOf("new" to "New", "acknowledged" to "Acked", "resolved" to "Resolved", "all" to "All")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(navController: NavController) {
    var status by remember { mutableStateOf("new") }
    var state by remember { mutableStateOf<Result<List<AlertItem>>?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            state = runCatching { ApiClient.service().getAlerts(status = status).data }
        }
    }
    LaunchedEffect(status) { load() }

    fun act(alert: AlertItem, action: String) {
        scope.launch {
            runCatching { ApiClient.service().actOnAlert(AlertActionRequest(alert.source, alert.id, action)) }
            load()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Alerts") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = STATUS_TABS.indexOfFirst { it.first == status }.coerceAtLeast(0),
                edgePadding = 12.dp
            ) {
                STATUS_TABS.forEach { (key, label) ->
                    Tab(selected = status == key, onClick = { status = key }, text = { Text(label) })
                }
            }

            when {
                state == null -> LoadingScreen()
                state!!.isFailure -> ErrorScreen(state!!.exceptionOrNull()?.message ?: "", onRetry = ::load)
                else -> {
                    val alerts = state!!.getOrThrow()
                    if (alerts.isEmpty()) {
                        EmptyScreen("No alerts here", Icons.Outlined.CheckCircle)
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(alerts, key = { it.source + it.id }) { alert ->
                                AlertRow(
                                    alert = alert,
                                    onAcknowledge = { act(alert, "acknowledge") },
                                    onResolve = { act(alert, "resolve") },
                                    onViewTicket = {
                                        alert.ticketId?.let { navController.navigate(Screen.TicketDetail.go(it)) }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun severityColor(severity: String): Color = when (severity) {
    "critical" -> Color(0xFFD32F2F)
    "error"    -> Color(0xFFD32F2F)
    "warning"  -> Color(0xFFF57C00)
    else       -> Color(0xFF1976D2)
}

private fun statusColor(status: String): Color = when (status) {
    "new"          -> Color(0xFFD32F2F)
    "acknowledged" -> Color(0xFFF57C00)
    "resolved"     -> Color(0xFF388E3C)
    else           -> Color.Gray
}

@Composable
private fun AlertRow(
    alert: AlertItem,
    onAcknowledge: () -> Unit,
    onResolve: () -> Unit,
    onViewTicket: () -> Unit
) {
    ListItem(
        leadingContent = {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = severityColor(alert.severity).copy(alpha = 0.15f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = if (alert.source == "backup") Icons.Outlined.CloudUpload else Icons.Outlined.Dns
                    Icon(icon, null, tint = severityColor(alert.severity), modifier = Modifier.size(20.dp))
                }
            }
        },
        headlineContent = { Text(alert.message ?: "", style = MaterialTheme.typography.bodyMedium, maxLines = 2) },
        supportingContent = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = severityColor(alert.severity), shape = MaterialTheme.shapes.extraSmall) {
                        Text(alert.severity, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(color = statusColor(alert.status).copy(alpha = 0.15f), shape = MaterialTheme.shapes.extraSmall) {
                        Text(alert.status, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall, color = statusColor(alert.status))
                    }
                }
                Spacer(Modifier.height(2.dp))
                val subtitle = listOfNotNull(alert.subject, alert.clientName).joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.padding(top = 6.dp)) {
                    if (alert.status != "resolved") {
                        if (alert.status == "new") {
                            TextButton(onClick = onAcknowledge, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Acknowledge") }
                        }
                        TextButton(onClick = onResolve, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Resolve") }
                    }
                    if (alert.ticketId != null) {
                        TextButton(onClick = onViewTicket, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text(alert.ticketLabel?.takeIf { it.isNotBlank() } ?: "View Ticket")
                        }
                    }
                }
            }
        }
    )
}
