package com.foleyit.itflow.ui.screens.tickets

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(id: Int, navController: NavController) {
    var state by remember { mutableStateOf<Result<TicketDetail>?>(null) }
    val scope = rememberCoroutineScope()

    // Timer
    var timerRunning by remember { mutableStateOf(false) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var timerStart by remember { mutableLongStateOf(0L) }

    // Reply sheet
    var showReply by remember { mutableStateOf(false) }
    var replyType by remember { mutableStateOf("reply") }

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
        val h = elapsed / 3600
        val m = (elapsed % 3600) / 60
        val s = elapsed % 60
        "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }

    val timeWorkedString = remember(elapsed) {
        val h = elapsed / 3600
        val m = (elapsed % 3600) / 60
        "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:00"
    }

    fun load() { scope.launch { state = runCatching { ApiClient.service().getTicket(id) } } }
    LaunchedEffect(Unit) { load() }

    // Reply bottom sheet
    if (showReply) {
        ReplySheet(
            type = replyType,
            defaultTimeWorked = if (elapsed > 0) timeWorkedString else "",
            onDismiss = { showReply = false },
            onSubmit = { reply, type, timeWorked ->
                scope.launch {
                    runCatching { ApiClient.service().addReply(id, reply, type = type, timeWorked = timeWorked.ifBlank { null }) }
                    load()
                    if (elapsed > 0) elapsed = 0L
                }
                showReply = false
            }
        )
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
                    IconButton(onClick = { timerRunning = !timerRunning }) {
                        Icon(
                            if (timerRunning) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle,
                            "Timer",
                            tint = if (timerRunning) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
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
                        Spacer(Modifier.width(6.dp))
                        Text("Note")
                    }
                    Button(
                        onClick = { replyType = "reply"; showReply = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Reply, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reply")
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
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Timer, null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        timerDisplay,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.weight(1f))
                                    TextButton(onClick = { replyType = "reply"; showReply = true }) {
                                        Text("Log time")
                                    }
                                }
                            }
                        }
                    }

                    // Header
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Row {
                                    PriorityBadge(ticket.priority)
                                    Spacer(Modifier.width(8.dp))
                                    ticket.status?.let { status ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                status,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(ticket.subject, style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(12.dp))
                                ticket.client?.let { InfoRow(Icons.Outlined.Business, it) }
                                ticket.assignedTo?.let { InfoRow(Icons.Outlined.Person, it) }
                                ticket.contactName?.let { InfoRow(Icons.Outlined.ContactPage, it) }
                                ticket.dueAt?.let { InfoRow(Icons.Outlined.Schedule, "Due: $it") }
                            }
                        }
                    }

                    // Details
                    if (!ticket.details.isNullOrBlank()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Description",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(8.dp))
                                    Text(ticket.details)
                                }
                            }
                        }
                    }

                    items(ticket.replies) { reply -> ReplyCard(reply) }
                }
            }
        }
    }
}

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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
            Text(
                if (type == "note") "Add Internal Note" else "Add Reply",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = reply, onValueChange = { reply = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Message") },
                minLines = 3, maxLines = 6
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { if (reply.isNotBlank()) onSubmit(reply, type, timeWorked) },
                    enabled = reply.isNotBlank()
                ) { Text("Post") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(modifier = Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ReplyCard(reply: TicketReply) {
    val isNote = reply.type == "note"
    val borderColor = if (isNote) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                      else MaterialTheme.colorScheme.outlineVariant
    Surface(
        color = if (isNote) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            reply.by?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(reply.by ?: "", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(reply.createdAt ?: "", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                }
                if (isNote) {
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.extraSmall) {
                        Text("Note",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
                reply.timeWorked?.let { tw ->
                    Spacer(Modifier.width(4.dp))
                    Surface(color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.extraSmall) {
                        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Timer, null, Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(2.dp))
                            Text(tw, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(reply.body)
        }
    }
}
