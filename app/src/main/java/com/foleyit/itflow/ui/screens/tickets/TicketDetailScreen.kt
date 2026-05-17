package com.foleyit.itflow.ui.screens.tickets

import android.text.Html
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
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
import com.foleyit.itflow.ui.navigation.Screen
import com.foleyit.itflow.ui.util.fmtDate
import java.text.NumberFormat
import java.util.Locale
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
    var showAddCharge by remember { mutableStateOf(false) }
    var showAddWorksheet by remember { mutableStateOf(false) }
    var showAddOuttake by remember { mutableStateOf(false) }
    var charges by remember { mutableStateOf<ChargesResponse?>(null) }
    var worksheets by remember { mutableStateOf<List<WorksheetSummary>>(emptyList()) }
    var outtakes by remember { mutableStateOf<List<OuttakeSummary>>(emptyList()) }

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
            charges = runCatching { ApiClient.service().getTicketCharges(id) }.getOrNull()
            worksheets = runCatching { ApiClient.service().getTicketWorksheets(id) }.getOrDefault(emptyList())
            outtakes = runCatching { ApiClient.service().getTicketOuttakes(id) }.getOrDefault(emptyList())
            if (statuses.isEmpty()) {
                statuses = runCatching { ApiClient.service().getTicketStatuses() }.getOrDefault(emptyList())
            }
        }
    }
    LaunchedEffect(Unit) { load() }

    if (showAddCharge) {
        AddChargeSheet(onDismiss = { showAddCharge = false }, onSave = { name, desc, qty, price ->
            scope.launch {
                runCatching { ApiClient.service().addCharge(id, AddChargeRequest(name, desc, qty, price)) }
                load()
            }
            showAddCharge = false
        })
    }

    if (showAddWorksheet) {
        SelectTemplateSheet(
            title = "New Worksheet",
            onDismiss = { showAddWorksheet = false },
            onCreate = { templateId ->
                scope.launch {
                    val ws = runCatching {
                        ApiClient.service().createWorksheet(id, CreateWorksheetRequest(templateId, 0))
                    }.getOrNull()
                    load()
                    ws?.get("id")?.let { wsId -> navController.navigate(Screen.FillWorksheet.go(wsId)) }
                }
                showAddWorksheet = false
            }
        )
    }

    if (showAddOuttake) {
        OuttakeSheet(
            onDismiss = { showAddOuttake = false },
            onCreate = {
                scope.launch {
                    val form = runCatching {
                        ApiClient.service().createOuttake(id, CreateWorksheetRequest())
                    }.getOrNull()
                    load()
                    form?.get("id")?.let { formId -> navController.navigate(Screen.OuttakeSign.go(formId)) }
                }
                showAddOuttake = false
            }
        )
    }

    if (showReply) {
        ReplySheet(
            type = replyType,
            defaultTimeWorked = if (elapsed > 0) timeWorkedString else "",
            onDismiss = { showReply = false },
            onSubmit = { reply, type, timeWorked, onsite ->
                scope.launch {
                    runCatching {
                        ApiClient.service().addReply(id, reply, type = type,
                            timeWorked = timeWorked.ifBlank { null }, onsite = onsite)
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
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
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
                        Icon(Icons.AutoMirrored.Outlined.StickyNote2, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("Note")
                    }
                    Button(
                        onClick = { replyType = "reply"; showReply = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Reply, null, Modifier.size(18.dp))
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
                                ticket.dueAt?.let { InfoRow(Icons.Outlined.Schedule, "Due: ${fmtDate(it)}") }
                                ticket.createdAt?.let { InfoRow(Icons.Outlined.CalendarToday, "Opened: ${fmtDate(it)}") }
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

                    // Charges
                    item { ChargesCard(charges, onAddCharge = { showAddCharge = true }) }

                    // Worksheets + Outtake Forms (separate sections)
                    item {
                        WorksheetsCard(
                            worksheets = worksheets,
                            outtakes = outtakes,
                            navController = navController,
                            onAddWorksheet = { showAddWorksheet = true },
                            onAddOuttake = { showAddOuttake = true },
                            onDeleteWorksheet = { wsId ->
                                scope.launch {
                                    runCatching { ApiClient.service().deleteWorksheet(wsId) }
                                    load()
                                }
                            },
                            onDeleteOuttake = { otId ->
                                scope.launch {
                                    runCatching { ApiClient.service().deleteOuttake(otId) }
                                    load()
                                }
                            }
                        )
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
    onSubmit: (reply: String, type: String, timeWorked: String, onsite: Boolean) -> Unit
) {
    var reply by remember { mutableStateOf("") }
    var timeWorked by remember { mutableStateOf(defaultTimeWorked) }
    var selectedType by remember { mutableStateOf(type) }
    var onsite by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()) {
            Text(if (selectedType == "note") "Add Internal Note" else "Add Reply",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            // Reply type toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selectedType == "reply",
                    onClick = { selectedType = "reply" }, label = { Text("Public Reply") })
                FilterChip(selected = selectedType == "note",
                    onClick = { selectedType = "note" }, label = { Text("Internal Note") })
            }
            Spacer(Modifier.height(8.dp))
            // Remote / On-Site toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !onsite,
                    onClick = { onsite = false },
                    label = { Text("Remote") },
                    leadingIcon = { Icon(Icons.Outlined.Wifi, null, Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = onsite,
                    onClick = { onsite = true },
                    label = { Text("On-Site") },
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, null, Modifier.size(16.dp)) }
                )
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
                    onClick = { if (reply.isNotBlank()) onSubmit(reply, selectedType, timeWorked, onsite) },
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
                    Text(fmtDate(reply.createdAt), style = MaterialTheme.typography.bodySmall,
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
                reply.onsite?.let { ons ->
                    Spacer(Modifier.width(4.dp))
                    Surface(
                        color = if (ons) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (ons) Icons.Outlined.LocationOn else Icons.Outlined.Wifi,
                                null, Modifier.size(11.dp),
                                tint = if (ons) MaterialTheme.colorScheme.onSecondaryContainer
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                if (ons) "On-Site" else "Remote",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ons) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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

@Composable
private fun ChargesCard(cr: ChargesResponse?, onAddCharge: (() -> Unit)? = null) {
    val currency = NumberFormat.getCurrencyInstance(Locale.US)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Charges", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (cr != null && cr.charges.isNotEmpty()) {
                        Text(currency.format(cr.total), fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                    }
                    onAddCharge?.let { action ->
                        FilledTonalIconButton(onClick = action, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.Add, "Add Charge", Modifier.size(16.dp))
                        }
                    }
                }
            }
            if (cr == null || cr.charges.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("No charges on this ticket.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
                return@Column
            }
            Spacer(Modifier.height(12.dp))
            cr.charges.forEachIndexed { i, charge ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(charge.name, fontWeight = FontWeight.Medium)
                        if (!charge.description.isNullOrBlank()) {
                            Text(charge.description, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${charge.quantity} × ${currency.format(charge.unitPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(currency.format(charge.total), fontWeight = FontWeight.Medium)
                        if (charge.invoiced) {
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.extraSmall) {
                                Text("Invoiced", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
                if (i < cr.charges.size - 1) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun WorksheetsCard(
    worksheets: List<WorksheetSummary>,
    outtakes: List<OuttakeSummary>,
    navController: NavController,
    onAddWorksheet: (() -> Unit)? = null,
    onAddOuttake: (() -> Unit)? = null,
    onDeleteWorksheet: ((Int) -> Unit)? = null,
    onDeleteOuttake: ((Int) -> Unit)? = null
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            // ── Worksheets ───────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Worksheets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                onAddWorksheet?.let { action ->
                    FilledTonalIconButton(onClick = action, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Add, "Add Worksheet", Modifier.size(16.dp))
                    }
                }
            }
            if (worksheets.isEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("No worksheets.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            } else {
                Spacer(Modifier.height(8.dp))
                WorksheetItems(worksheets, navController, onDeleteWorksheet)
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // ── Outtake Forms ────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Outtake Forms", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Client signs on pickup", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                }
                onAddOuttake?.let { action ->
                    FilledTonalIconButton(onClick = action, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Add, "Add Outtake Form", Modifier.size(16.dp))
                    }
                }
            }
            if (outtakes.isEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("No outtake forms.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            } else {
                Spacer(Modifier.height(8.dp))
                OuttakeItems(outtakes, navController, onDeleteOuttake)
            }
        }
    }
}

@Composable
private fun WorksheetItems(
    entries: List<WorksheetSummary>,
    navController: NavController,
    onDelete: ((Int) -> Unit)? = null
) {
    var deleteTarget by remember { mutableStateOf<WorksheetSummary?>(null) }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Worksheet?") },
            text = { Text("\"${deleteTarget!!.templateName ?: "Worksheet"}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete?.invoke(deleteTarget!!.id)
                    deleteTarget = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    entries.forEachIndexed { i, ws ->
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ws.signed) Icons.Outlined.CheckCircle else Icons.AutoMirrored.Outlined.Assignment,
                null, modifier = Modifier.size(20.dp),
                tint = if (ws.signed) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(ws.templateName ?: "Worksheet", fontWeight = FontWeight.Medium)
                Text(
                    if (ws.signed) "Signed by ${ws.signedName}"
                    else "Not signed · by ${ws.createdBy ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ws.signed) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                )
            }
            if (!ws.signed) {
                FilledTonalButton(
                    onClick = { navController.navigate(Screen.SignWorksheet.go(ws.id)) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Outlined.Draw, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sign", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = { deleteTarget = ws }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.DeleteOutline, "Delete",
                    Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
            }
        }
        if (i < entries.size - 1) HorizontalDivider()
    }
}

@Composable
private fun OuttakeItems(
    entries: List<OuttakeSummary>,
    navController: NavController,
    onDelete: ((Int) -> Unit)? = null
) {
    var deleteTarget by remember { mutableStateOf<OuttakeSummary?>(null) }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Outtake Form?") },
            text = { Text("This outtake form will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete?.invoke(deleteTarget!!.id)
                    deleteTarget = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    entries.forEachIndexed { i, ot ->
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ot.signed) Icons.Outlined.CheckCircle else Icons.Outlined.Draw,
                null, modifier = Modifier.size(20.dp),
                tint = if (ot.signed) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Outtake Form", fontWeight = FontWeight.Medium)
                Text(
                    if (ot.signed) "Signed by ${ot.signedName}"
                    else "Not signed · by ${ot.createdBy ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ot.signed) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                )
            }
            if (!ot.signed) {
                FilledTonalButton(
                    onClick = { navController.navigate(Screen.OuttakeSign.go(ot.id)) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Outlined.Draw, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sign", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = { deleteTarget = ot }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.DeleteOutline, "Delete",
                    Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
            }
        }
        if (i < entries.size - 1) HorizontalDivider()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddChargeSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String, qty: Double, price: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf("") }
    var productSearch by remember { mutableStateOf("") }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    val total = remember(qty, price) { (qty.toDoubleOrNull() ?: 0.0) * (price.toDoubleOrNull() ?: 0.0) }
    val currency = NumberFormat.getCurrencyInstance(Locale.US)

    LaunchedEffect(Unit) {
        products = runCatching { ApiClient.service().getProducts() }.getOrDefault(emptyList())
    }

    val filteredProducts = remember(productSearch, products) {
        if (productSearch.isBlank()) emptyList()
        else products.filter {
            it.name.contains(productSearch, ignoreCase = true) ||
            it.description?.contains(productSearch, ignoreCase = true) == true
        }.take(5)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Text("Add Charge", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            // Product catalog search
            if (products.isNotEmpty()) {
                Text("From Catalog", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = productSearch,
                    onValueChange = { productSearch = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search products & services…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        if (productSearch.isNotEmpty()) {
                            IconButton(onClick = { productSearch = "" }) {
                                Icon(Icons.Outlined.Clear, null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge
                )
                if (filteredProducts.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    filteredProducts.forEach { product ->
                        Surface(
                            onClick = {
                                name = product.name
                                desc = product.description ?: ""
                                price = product.price.toString()
                                productSearch = ""
                            },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.bodyMedium)
                                    product.type?.let {
                                        Text(it, style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                                Text(currency.format(product.price), fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
            }

            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Item Name *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = desc, onValueChange = { desc = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = qty, onValueChange = { qty = it }, label = { Text("Qty") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = price, onValueChange = { price = it }, label = { Text("Unit Price") },
                    leadingIcon = { Text("$") }, modifier = Modifier.weight(2f), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            if (total > 0) {
                Spacer(Modifier.height(8.dp))
                Text("Total: ${currency.format(total)}", fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) onSave(name, desc, qty.toDoubleOrNull() ?: 1.0, price.toDoubleOrNull() ?: 0.0)
                    },
                    enabled = name.isNotBlank()
                ) { Text("Add Charge") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OuttakeSheet(onDismiss: () -> Unit, onCreate: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 24.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Draw, null, Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Outtake Form", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Creates a sign-off form so the client can sign when picking up their device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Draw, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create & Sign")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectTemplateSheet(
    title: String,
    onDismiss: () -> Unit,
    onCreate: (templateId: Int) -> Unit
) {
    var templates by remember { mutableStateOf<List<WorksheetTemplate>>(emptyList()) }

    LaunchedEffect(Unit) {
        templates = runCatching { ApiClient.service().getWorksheetTemplates() }.getOrDefault(emptyList())
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            if (templates.isEmpty()) {
                Text("No worksheet templates available.", color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Select Template", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                templates.forEach { t ->
                    Surface(
                        onClick = { onCreate(t.id) },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(t.name, fontWeight = FontWeight.Medium)
                            t.description?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            Spacer(Modifier.height(8.dp))
        }
    }
}
