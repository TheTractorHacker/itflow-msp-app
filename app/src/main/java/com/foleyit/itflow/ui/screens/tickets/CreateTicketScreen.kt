package com.foleyit.itflow.ui.screens.tickets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.data.api.ClientsResponse
import com.foleyit.itflow.data.api.Contact
import com.foleyit.itflow.data.api.CreateTicketRequest
import com.foleyit.itflow.data.api.TicketCategory
import com.foleyit.itflow.ui.components.SectionLabel
import com.foleyit.itflow.ui.navigation.Screen
import com.foleyit.itflow.ui.theme.forPriority
import com.foleyit.itflow.ui.theme.statusColors
import com.foleyit.itflow.ui.util.userMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTicketScreen(navController: NavController) {
    var subject by rememberSaveable { mutableStateOf("") }
    var details by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf("low") }
    var selectedClientId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedClientName by rememberSaveable { mutableStateOf("") }
    var clients by remember { mutableStateOf<ClientsResponse?>(null) }
    var showClientPicker by rememberSaveable { mutableStateOf(false) }
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var selectedContactId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showContactPicker by rememberSaveable { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<TicketCategory>>(emptyList()) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showCategoryPicker by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    // A failed load must not look like "no clients / no categories": say so and offer Retry.
    var loadFailed by remember { mutableStateOf(false) }
    var contactsFailed by remember { mutableStateOf(false) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(loadAttempt) {
        val clientsResult = runCatching { ApiClient.service().getClients(search = "", page = 1) }
        val categoriesResult = runCatching { ApiClient.service().getTicketCategories() }
        clients = clientsResult.getOrNull()
        categories = categoriesResult.getOrDefault(emptyList())
        loadFailed = clientsResult.isFailure || categoriesResult.isFailure
    }

    // A contact belongs to one client: reload the list (and forget the old pick) whenever the client changes.
    // The saved pick survives rotation because the id is only cleared if it is not in the new client's list.
    LaunchedEffect(selectedClientId) {
        val clientId = selectedClientId
        if (clientId == null) { contacts = emptyList(); selectedContactId = null; return@LaunchedEffect }
        val contactsResult = runCatching { ApiClient.service().getContacts(clientId).data }
        contactsFailed = contactsResult.isFailure
        contacts = contactsResult.getOrDefault(emptyList())
        if (contacts.none { it.id == selectedContactId }) selectedContactId = null
    }

    fun submit() {
        if (subject.isBlank()) { error = SUBJECT_REQUIRED; return }
        saving = true; error = null
        scope.launch {
            try {
                val created = ApiClient.service().createTicket(
                    CreateTicketRequest(
                        subject = subject.trim(),
                        details = details.trim(),
                        clientId = selectedClientId,
                        priority = priority,
                        categoryId = selectedCategoryId,
                        contactId = selectedContactId.takeIf { selectedClientId != null }
                    )
                )
                // Open the new ticket in place of this form (Back then returns to where the
                // user started); fall back to just closing the form if the id is missing.
                val newId = created["id"]
                if (newId != null) {
                    navController.navigate(Screen.TicketDetail.go(newId)) {
                        popUpTo(Screen.CreateTicket.route) { inclusive = true }
                    }
                } else navController.popBackStack()
            } catch (e: Exception) {
                error = userMessage(e)
            } finally { saving = false }
        }
    }

    if (showClientPicker) {
        var query by remember { mutableStateOf("") }
        var results by remember { mutableStateOf(clients?.data.orEmpty()) }
        // Same as the web form's searchable client select: ask the server, so clients beyond the
        // first page are reachable.
        LaunchedEffect(query) {
            if (query.isNotEmpty()) kotlinx.coroutines.delay(300)
            results = runCatching { ApiClient.service().getClients(search = query.trim(), page = 1).data }
                .getOrDefault(clients?.data.orEmpty())
        }
        ModalBottomSheet(onDismissRequest = { showClientPicker = false }) {
            Column(Modifier.padding(16.dp).navigationBarsPadding()) {
                Text("Select Client", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Search clients…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) }
                )
                Spacer(Modifier.height(8.dp))
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Surface(onClick = { selectedClientId = null; selectedClientName = ""; showClientPicker = false },
                        modifier = Modifier.fillMaxWidth()) {
                        Text("— No Client —", Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.outline)
                    }
                    HorizontalDivider()
                    results.forEach { c ->
                        Surface(onClick = {
                            selectedClientId = c.id; selectedClientName = c.name; showClientPicker = false
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(c.name, Modifier.padding(16.dp))
                        }
                        HorizontalDivider()
                    }
                    if (results.isEmpty()) Text("No matching clients", Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }

    if (showContactPicker) {
        var query by remember { mutableStateOf("") }
        val q = query.trim()
        val shown = if (q.isEmpty()) contacts else contacts.filter {
            it.name.contains(q, ignoreCase = true) || it.email?.contains(q, ignoreCase = true) == true ||
                it.title?.contains(q, ignoreCase = true) == true
        }
        ModalBottomSheet(onDismissRequest = { showContactPicker = false }) {
            Column(Modifier.padding(16.dp).navigationBarsPadding()) {
                Text("Select Contact", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Search contacts…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) }
                )
                Spacer(Modifier.height(8.dp))
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Surface(onClick = { selectedContactId = null; showContactPicker = false },
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Primary contact (automatic)", Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.outline)
                }
                HorizontalDivider()
                shown.forEach { c ->
                    Surface(onClick = { selectedContactId = c.id; showContactPicker = false },
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(c.name)
                            val sub = listOfNotNull(c.title, c.email).filter { it.isNotBlank() }.joinToString(" · ")
                            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    HorizontalDivider()
                }
                if (shown.isEmpty()) Text("No matching contacts", Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }

    if (showCategoryPicker) {
        ModalBottomSheet(onDismissRequest = { showCategoryPicker = false }) {
            Column(Modifier.padding(16.dp).navigationBarsPadding()) {
                Text("Select Category", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Surface(onClick = { selectedCategoryId = null; showCategoryPicker = false },
                    modifier = Modifier.fillMaxWidth()) {
                    Text("— No Category —", Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.outline)
                }
                HorizontalDivider()
                categories.forEach { cat ->
                    Surface(onClick = {
                        selectedCategoryId = cat.id; showCategoryPicker = false
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(cat.name, Modifier.padding(16.dp))
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                    // Shown beside the button (not at the end of the scrolling form) so a
                    // validation or network error is visible without scrolling.
                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Button(
                        onClick = ::submit,
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create Ticket", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Intro row
            Row(verticalAlignment = Alignment.CenterVertically) {
                val gradient = Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.inversePrimary),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(gradient, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.ConfirmationNumber, null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Log a new ticket",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Capture the details so your team can jump on it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Details card
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Details")
                    OutlinedTextField(
                        value = subject, onValueChange = { subject = it; if (error != null) error = null },
                        label = { Text("Subject *") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        isError = error == SUBJECT_REQUIRED,
                        supportingText = if (error == SUBJECT_REQUIRED) {{ Text(SUBJECT_REQUIRED) }} else null
                    )
                    OutlinedTextField(
                        value = details, onValueChange = { details = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 8
                    )
                }
            }

            if (loadFailed) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Couldn't load clients or categories.", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        TextButton(onClick = { loadAttempt++ }) { Text("Retry") }
                    }
                }
            }

            // Client & category card
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel("Client")
                        Surface(
                            onClick = { showClientPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Business, null, tint = MaterialTheme.colorScheme.outline)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    selectedClientName.ifBlank { "Select client (optional)" },
                                    color = if (selectedClientName.isBlank()) MaterialTheme.colorScheme.outline
                                            else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                    if (selectedClientId != null && contactsFailed) {
                        Text("Couldn't load contacts; the client's primary contact will be used.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
                    }
                    if (selectedClientId != null && contacts.isNotEmpty()) {
                        val selectedContactName = contacts.firstOrNull { it.id == selectedContactId }?.name
                        Column(
                            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SectionLabel("Contact")
                            Surface(
                                onClick = { showContactPicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.outline)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        selectedContactName ?: "Primary contact (automatic)",
                                        color = if (selectedContactName == null) MaterialTheme.colorScheme.outline
                                                else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                    if (categories.isNotEmpty()) {
                        val selectedCategoryName = categories.firstOrNull { it.id == selectedCategoryId }?.name
                        Column(
                            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SectionLabel("Category")
                            Surface(
                                onClick = { showCategoryPicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Row(
                                    Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.Label, null, tint = MaterialTheme.colorScheme.outline)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        selectedCategoryName ?: "Select category (optional)",
                                        color = if (selectedCategoryName == null) MaterialTheme.colorScheme.outline
                                                else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }

            // Priority card
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Priority")
                    val priorities = listOf("low" to "Low", "medium" to "Medium",
                                             "high" to "High", "critical" to "Critical")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        priorities.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (value, label) ->
                                    PriorityTile(
                                        label = label,
                                        selected = priority == value,
                                        color = MaterialTheme.statusColors.forPriority(value),
                                        onClick = { priority = value },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val SUBJECT_REQUIRED = "Subject required"

@Composable
private fun PriorityTile(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) color.copy(alpha = 0.12f) else Color.Transparent,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) color else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
