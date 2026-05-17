package com.foleyit.itflow.ui.screens.dashboard

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
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.data.api.DashboardResponse
import com.foleyit.itflow.data.api.TicketSummary
import com.foleyit.itflow.ui.components.ErrorScreen
import com.foleyit.itflow.ui.components.LoadingScreen
import com.foleyit.itflow.ui.navigation.Screen
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(navController: NavController) {
    var state by remember { mutableStateOf<Result<DashboardResponse>?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun load() {
        scope.launch {
            val result = runCatching { ApiClient.service().getDashboard() }
            state = result
            // Update widget cache
            result.getOrNull()?.let { dash ->
                context.getSharedPreferences("widget_cache", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putInt("open", dash.allOpen)
                    .putInt("overdue", dash.overdue)
                    .apply()
                // Request widget update
                val intent = android.content.Intent("android.appwidget.action.APPWIDGET_UPDATE")
                context.sendBroadcast(intent)
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    when {
        state == null -> LoadingScreen()
        state!!.isFailure -> ErrorScreen(state!!.exceptionOrNull()?.message ?: "Error", onRetry = ::load)
        else -> {
            val dash = state!!.getOrThrow()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Dashboard", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("My Open", dash.myOpen.toString(), Icons.Outlined.Person,
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f))
                        StatCard("All Open", dash.allOpen.toString(), Icons.Outlined.ConfirmationNumber,
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f))
                        StatCard("Overdue", dash.overdue.toString(), Icons.Outlined.Warning,
                            if (dash.overdue > 0) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            if (dash.overdue > 0) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("My Queue", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { navController.navigate(Screen.Tickets.route) }) {
                            Text("See all")
                        }
                    }
                }
                if (dash.queue.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(32.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Outlined.CheckCircle, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("All caught up!", style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                } else {
                    items(dash.queue) { ticket ->
                        QueueTicketCard(ticket) { navController.navigate(Screen.TicketDetail.go(ticket.id)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                     bg: Color, fg: Color, modifier: Modifier) {
    Surface(color = bg, shape = MaterialTheme.shapes.large, modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = fg)
            Text(label, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun QueueTicketCard(ticket: TicketSummary, onClick: () -> Unit) {
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
                modifier = Modifier.width(4.dp).height(52.dp)
            ) {}
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ticket.subject, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text(ticket.client ?: "", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
            ticket.status?.let {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small) {
                    Text(it, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}
