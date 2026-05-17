package com.foleyit.itflow.ui.screens.tickets

import android.text.Html
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.foleyit.itflow.data.api.*
import com.foleyit.itflow.ui.components.ErrorScreen
import com.foleyit.itflow.ui.components.LoadingScreen
import com.foleyit.itflow.ui.components.PriorityBadge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun stripHtml(html: String?): String {
    if (html.isNullOrBlank()) return ""
    return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(id: Int, navController: NavController) {
    var state by remember { mutableStateOf<Result<TicketDetail>?>(null) }
    var statuses by remember { mutableStateOf<List<TicketStatus>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // Timer
    var timerRunning by remember { mutableStateOf(false) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var timerStart by remember { mutableLongStateOf(0L) }

    // Sheets
    var showReply by remember { mutableStateOf(false) }
    var replyType by remember { mutableStateOf("reply") }
    var showStatusPicker by remember { mutableStateOf(false) }

    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            timerStart = System.currentTimeMillis() - elapsed * 1000
            while (timerRunning) {
                elapsed = (System.currentTimeMillis() - timerStart) / 1000
                delay(1000)
            }
        }
    }

    val timerDisplay = remember(elapsed) {
        val h = elapsed / 3600; val m = (elapsed % 3600) / 60; val s = elapsed % 60
        "$h:${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}"
    }
    val timeWorkedString = remember(elapsed) {
        "${(elapsed/3600).toString().padStart(2,'0')}:${((elapsed%3600)/60).toString().padStart(2,'0')}:00"
    }

    fun load() {
        scope.launch {
            state = runCatching { ApiClient.service().getTicket(id) }
            if (statuses.isEmpty()) {
                statuses = runCatching { ApiClient.service().getTicketStatuses() }.getOrDefault(emptyList())
            }
        }
    }
    LaunchedEffect(Unit) { load() }

    if (showReply) {
        ReplySheet(
            type = replyType,
            defaultTimeWorked = if (elapsed > 0) timeWorkedString else "",
            onDismiss = { showReply = false },
            onSubmit = { reply, type, timeWorked ->
                scope.launch {
                    runCatching {
                        ApiClient.service().addReply(id, reply, type = type,
                            timeWorked = timeWorked.ifBlank { null })
                    }
                    load()
                    if (elapsed > 0) elapsed = 0L
                }
                showReply = false
            }
        )
    }

    if (showStatusPicker && statuses.isNotEmpty()) {
        ModalBottomSheet(onDismissRequest = { showStatusPicker = false }) {
            Column(Modifier.padding(16.dp).navigationBarsPadding()) {
                Text("Change Status", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                statuses.forEach { s ->
                    val isCurrentStatus = state?.getOrNull()?.status == s.name
                    Surface(
                        onClick = {
                            showStatusPicker = false
                            scope.launch {
                                runCatching { ApiClient.service().updateTicketStatus(id, mapOf("status_id" to s.id)) }
                                load()
                            }
                        },
                        color = if (isCurrentStatus) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = parseStatusColor(s.color),
                                shape = MaterialTheme.shapes.extraSmall,
                                modifier = Modifier.size(12.dp)) {}
                            Spacer(Modifier.width(12.dp))
                            Text(s.name, style = MaterialTheme.typography.bodyMedium)
                            if (isCurrentStatus) {
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Outlined.Check, null, Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { state?.getOrNull()?.let { Text("#${it.number}") } ?: Text("Ticket") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Outlined.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Timer toggle
                    IconButton(onClick = { timerRunning = !timerRunning }) {
                        Icon(
                            if (timerRunning) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle,
                            "Timer",
                            tint = if (timerRunning) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Change status
                    if (statuses.isNotEmpty()) {
                        IconButton(onClick = { showStatusPicker = true }) {
                            Icon(Icons.Outlined.SwapVert, "Change Status")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { replyType = "note"; showReply = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.StickyNote2, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("Note")
                    }
                    Button(
                        onClick = { replyType = "reply"; showReply = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Reply, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("Reply")
                    }
                }
            }
        }
    ) { padding ->
        when {
            state == null -> LoadingScreen()
            state!!.isFailure -> ErrorScreen(state!!.exceptionOrNull()?.message ?: "Error")
            else -> {
                val ticket = state!!.getOrThrow()
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Timer bar
                    if (elapsed > 0 || timerRunning) {
                        item {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.medium) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Timer, null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(Modifier.width(8.dp))
                                    Text(timerDisplay, fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(Modifier.weight(1f))
                                    TextButton(onClick = { replyType = "reply"; showReply = true }) {
                                        Text("Log & Reply")
                                    }
                                }
                            }
                        }
                    }

                    // Header card
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                // Status + Priority row
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    PriorityBadge(ticket.priority)
                                    ticket.status?.let { status ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = MaterialTheme.shapes.small,
                                            onClick = { showStatusPicker = true }
                                        ) {
                                            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically) {
                                                Text(status, style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                Spacer(Modifier.width(4.dp))
                                                Icon(Icons.Outlined.ArrowDropDown, null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    if (ticket.billable) {
                                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer,
                                            shape = MaterialTheme.shapes.small) {
                                            Text("Billable", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(ticket.subject, style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(12.dp))
                                ticket.client?.let { InfoRow(Icons.Outlined.Business, it) }
                                ticket.assignedTo?.let { InfoRow(Icons.Outlined.PersonOutline, "Assigned: $it") }
                                ticket.contactName?.let { InfoRow(Icons.Outlined.ContactPage, it) }
                                ticket.contactPhone?.let { InfoRow(Icons.Outlined.Phone, it) }
                                ticket.dueAt?.let { InfoRow(Icons.Outlined.Schedule, "Due: $it") }
                                ticket.createdAt?.let { InfoRow(Icons.Outlined.CalendarToday, "Opened: $it") }
                            }
                        }
                    }

                    // Description
                    if (!ticket.details.isNullOrBlank()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Description", style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(8.dp))
                                    Text(stripHtml(ticket.details))
                                }
                            }
                        }
                    }

                    // Reply count header
                    if (ticket.replies.isNotEmpty()) {
                        item {
                            Text("${ticket.replies.size} ${if (ticket.replies.size == 1) "reply" else "replies"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }

                    // Replies
                    items(ticket.replies) { reply -> ReplyCard(reply) }
                }
            }
        }
    }
}

fun parseStatusColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
} catch (_: Exception) { Color.Gray }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReplySheet(
    type: String,
    defaultTimeWorked: String,
    onDismiss: () -> Unit,
    onSubmit: (reply: String, type: String, timeWorked: String) -> Unit
) {
    var reply by remember { mutableStateOf("") }
    var timeWorked by remember { mutableStateOf(defaultTimeWorked) }
    var selectedType by remember { mutableStateOf(type) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
            Text(if (selectedType == "note") "Add Internal Note" else "Add Reply",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            // Toggle reply type
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selectedType == "reply",
                    onClick = { selectedType = "reply" }, label = { Text("Public Reply") })
                FilterChip(selected = selectedType == "note",
                    onClick = { selectedType = "note" }, label = { Text("Internal Note") })
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = reply, onValueChange = { reply = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Message") },
                minLines = 4, maxLines = 8
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = timeWorked, onValueChange = { timeWorked = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Time worked (HH:MM:SS)") },
                leadingIcon = { Icon(Icons.Outlined.Timer, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { if (reply.isNotBlank()) onSubmit(reply, selectedType, timeWorked) },
                    enabled = reply.isNotBlank()
                ) { Text(if (selectedType == "note") "Add Note" else "Send Reply") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(modifier = Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ReplyCard(reply: TicketReply) {
    val isNote = reply.type == "note"
    Surface(
        color = if (isNote) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isNote) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(reply.by?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(reply.by ?: "", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(reply.createdAt ?: "", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                }
                if (isNote) {
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.extraSmall) {
                        Text("Note", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
                reply.timeWorked?.let { tw ->
                    Spacer(Modifier.width(4.dp))
                    Surface(color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.extraSmall) {
                        Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Timer, null, Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(2.dp))
                            Text(tw, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(stripHtml(reply.body))
        }
    }
}
