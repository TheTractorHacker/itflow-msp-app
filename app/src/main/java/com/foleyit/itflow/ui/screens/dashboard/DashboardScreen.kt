package com.foleyit.itflow.ui.screens.dashboard

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import java.util.Calendar

@Composable
fun DashboardScreen(navController: NavController) {
    var state by remember { mutableStateOf<Result<DashboardResponse>?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun load() {
        scope.launch {
            val result = runCatching { ApiClient.service().getDashboard() }
            state = result
            result.getOrNull()?.let { dash ->
                context.getSharedPreferences("widget_cache", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putInt("open", dash.allOpen)
                    .putInt("overdue", dash.overdue)
                    .apply()
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
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when (hour) {
                in 5..11  -> "Good morning"
                in 12..16 -> "Good afternoon"
                in 17..20 -> "Good evening"
                else      -> "Working late?"
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Greeting header
                item {
                    Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                        Text(
                            greeting,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(3.dp))
                        if (dash.unread > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Notifications, null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "${dash.unread} unread notification${if (dash.unread != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Text(
                                "All caught up",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Primary stats
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            label = "My Open",
                            value = dash.myOpen.toString(),
                            icon = Icons.Outlined.Person,
                            bg = MaterialTheme.colorScheme.primaryContainer,
                            fg = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate(Screen.Tickets.route) }
                        )
                        StatCard(
                            label = "All Open",
                            value = dash.allOpen.toString(),
                            icon = Icons.Outlined.ConfirmationNumber,
                            bg = MaterialTheme.colorScheme.secondaryContainer,
                            fg = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate(Screen.Tickets.route) }
                        )
                        StatCard(
                            label = "Overdue",
                            value = dash.overdue.toString(),
                            icon = Icons.Outlined.Warning,
                            bg = if (dash.overdue > 0) MaterialTheme.colorScheme.errorContainer
                                 else MaterialTheme.colorScheme.surfaceVariant,
                            fg = if (dash.overdue > 0) MaterialTheme.colorScheme.onErrorContainer
                                 else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Secondary stats (optional fields)
                if (dash.dueToday != null || dash.onsiteOpen != null) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            dash.dueToday?.let { dueToday ->
                                StatCard(
                                    label = "Due Today",
                                    value = dueToday.toString(),
                                    icon = Icons.Outlined.Schedule,
                                    bg = if (dueToday > 0) MaterialTheme.colorScheme.tertiaryContainer
                                         else MaterialTheme.colorScheme.surfaceVariant,
                                    fg = if (dueToday > 0) MaterialTheme.colorScheme.onTertiaryContainer
                                         else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    onClick = { navController.navigate(Screen.Tickets.route) }
                                )
                            }
                            dash.onsiteOpen?.let { onsiteOpen ->
                                StatCard(
                                    label = "On-Site",
                                    value = onsiteOpen.toString(),
                                    icon = Icons.Outlined.LocationOn,
                                    bg = MaterialTheme.colorScheme.surfaceVariant,
                                    fg = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    onClick = { navController.navigate(Screen.Tickets.route) }
                                )
                            }
                            // Balance row when only one secondary stat exists
                            if ((dash.dueToday == null) xor (dash.onsiteOpen == null)) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                // My Queue section header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "My Queue",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (dash.queue.isNotEmpty()) {
                                Text(
                                    "${dash.queue.size} ticket${if (dash.queue.size != 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
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
                                Icon(
                                    Icons.Outlined.CheckCircle, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Queue is clear!", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "No tickets assigned to you",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    items(dash.queue) { ticket ->
                        QueueTicketCard(ticket) {
                            navController.navigate(Screen.TicketDetail.go(ticket.id))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    bg: Color,
    fg: Color,
    modifier: Modifier,
    onClick: (() -> Unit)? = null
) {
    val base = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Surface(color = bg, shape = MaterialTheme.shapes.large, modifier = base) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, null, tint = fg.copy(alpha = 0.65f), modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = fg
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.75f))
        }
    }
}

@Composable
private fun QueueTicketCard(ticket: TicketSummary, onClick: () -> Unit) {
    val priorityColor = when (ticket.priority?.lowercase()) {
        "critical" -> MaterialTheme.colorScheme.error
        "high"     -> Color(0xFFBF360C)
        "medium"   -> Color(0xFFF57C00)
        else       -> MaterialTheme.colorScheme.outline
    }
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = priorityColor,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.width(4.dp).height(56.dp)
            ) {}
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "#${ticket.number}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    ticket.assignedTo?.let { assigned ->
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.Person, null, Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.width(2.dp))
                        Text(assigned, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    ticket.subject,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Business, null, Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(4.dp))
                    Text(ticket.client ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
            Spacer(Modifier.width(8.dp))
            ticket.status?.let { status ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
