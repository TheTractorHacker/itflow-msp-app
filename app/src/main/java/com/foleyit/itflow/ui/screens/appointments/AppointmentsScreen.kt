package com.foleyit.itflow.ui.screens.appointments

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
import com.foleyit.itflow.ui.util.addAppointmentToCalendar
import com.foleyit.itflow.ui.util.fmtDate
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.data.api.Appointment
import com.foleyit.itflow.ui.components.EmptyScreen
import com.foleyit.itflow.ui.components.ErrorScreen
import com.foleyit.itflow.ui.components.LoadingScreen
import com.foleyit.itflow.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(navController: NavController) {
    var when_ by remember { mutableStateOf("future") }
    var mineOnly by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf<Result<List<Appointment>>?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            state = runCatching { ApiClient.service().getAppointments(when_, if (mineOnly) 1 else 0) }
        }
    }
    LaunchedEffect(when_, mineOnly) { load() }

    Column(Modifier.fillMaxSize()) {
        // When filters
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("past" to "Past", "today" to "Today", "future" to "Upcoming").forEach { (key, label) ->
                FilterChip(selected = when_ == key, onClick = { when_ = key }, label = { Text(label) })
            }
            Spacer(Modifier.weight(1f))
            FilterChip(selected = mineOnly, onClick = { mineOnly = !mineOnly }, label = { Text("Mine") })
        }

        when {
            state == null -> LoadingScreen()
            state!!.isFailure -> ErrorScreen(state!!.exceptionOrNull()?.message ?: "Error", onRetry = ::load)
            else -> {
                val appts = state!!.getOrThrow()
                if (appts.isEmpty()) {
                    EmptyScreen(
                        message = when {
                            mineOnly && when_ == "today" -> "No appointments scheduled for you today"
                            when_ == "today" -> "No appointments scheduled today"
                            when_ == "past" -> "No past appointments"
                            else -> "No upcoming appointments"
                        },
                        icon = Icons.Outlined.CalendarMonth
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(appts) { appt ->
                            AppointmentCard(appt) {
                                navController.navigate(Screen.TicketDetail.go(appt.id))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(appt: Appointment, onClick: () -> Unit) {
    val priorityColor = when (appt.priority?.lowercase()) {
        "critical" -> MaterialTheme.colorScheme.error
        "high" -> Color(0xFFE65100)
        "medium" -> Color(0xFFF9A825)
        else -> MaterialTheme.colorScheme.outline
    }
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(16.dp)) {
            // Color strip
            Surface(color = priorityColor, shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.width(4.dp).height(72.dp)) {}
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                // Date/time
                appt.schedule?.let { schedule ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (appt.onsite) Icons.Outlined.LocationOn else Icons.Outlined.VideoCall,
                            null, modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            fmtDate(schedule) + (appt.scheduleEnd?.let { " – ${fmtDate(it)}" } ?: ""),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Text(appt.subject, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    appt.client?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Business, null, Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.width(3.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    appt.assignedTo?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Person, null, Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.width(3.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
                appt.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    Spacer(Modifier.height(4.dp))
                    Text(notes, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                appt.status?.let {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small) {
                        Text(it, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                val ctx = LocalContext.current
                if (appt.schedule != null) {
                    FilledTonalIconButton(onClick = { addAppointmentToCalendar(ctx, appt) },
                        modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.CalendarMonth, "Add to Calendar", Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

