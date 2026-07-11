package com.foleyit.itflow.ui.screens.tickets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.data.api.ClientsResponse
import com.foleyit.itflow.data.api.CreateTicketRequest
import com.foleyit.itflow.data.api.TicketCategory
import com.foleyit.itflow.ui.util.userMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTicketScreen(navController: NavController) {
    var subject by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("low") }
    var selectedClientId by remember { mutableStateOf<Int?>(null) }
    var selectedClientName by remember { mutableStateOf("") }
    var clients by remember { mutableStateOf<ClientsResponse?>(null) }
    var showClientPicker by remember { mutableStateOf(false) }
    var showPriorityMenu by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<TicketCategory>>(emptyList()) }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        // Load all pages until we have enough clients for the picker
        clients = runCatching { ApiClient.service().getClients(search = "", page = 1) }.getOrNull()
        categories = runCatching { ApiClient.service().getTicketCategories() }.getOrDefault(emptyList())
    }

    if (showClientPicker) {
        ModalBottomSheet(onDismissRequest = { showClientPicker = false }) {
            Column(Modifier.padding(16.dp).navigationBarsPadding()) {
                Text("Select Client", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Surface(onClick = { selectedClientId = null; selectedClientName = ""; showClientPicker = false },
                    modifier = Modifier.fillMaxWidth()) {
                    Text("— No Client —", Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.outline)
                }
                HorizontalDivider()
                clients?.data?.forEach { c ->
                    Surface(onClick = {
                        selectedClientId = c.id; selectedClientName = c.name; showClientPicker = false
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(c.name, Modifier.padding(16.dp))
                    }
                    HorizontalDivider()
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
                title = { Text("New Ticket") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (subject.isBlank()) { error = "Subject required"; return@TextButton }
                        saving = true; error = null
                        scope.launch {
                            try {
                                val result = ApiClient.service().createTicket(
                                    CreateTicketRequest(
                                        subject = subject.trim(),
                                        details = details.trim(),
                                        clientId = selectedClientId,
                                        priority = priority,
                                        categoryId = selectedCategoryId
                                    )
                                )
                                navController.popBackStack()
                            } catch (e: Exception) {
                                error = userMessage(e)
                            } finally { saving = false }
                        }
                    }, enabled = !saving) {
                        if (saving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("Create", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = subject, onValueChange = { subject = it },
                label = { Text("Subject *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = details, onValueChange = { details = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 8
            )
            // Client picker
            OutlinedCard(modifier = Modifier.fillMaxWidth(),
                onClick = { showClientPicker = true }) {
                Row(Modifier.padding(16.dp)) {
                    Icon(Icons.Outlined.Business, null, tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(12.dp))
                    Text(selectedClientName.ifBlank { "Select client (optional)" },
                        color = if (selectedClientName.isBlank()) MaterialTheme.colorScheme.outline
                                else MaterialTheme.colorScheme.onSurface)
                }
            }
            // Category picker
            if (categories.isNotEmpty()) {
                val selectedCategoryName = categories.firstOrNull { it.id == selectedCategoryId }?.name
                OutlinedCard(modifier = Modifier.fillMaxWidth(),
                    onClick = { showCategoryPicker = true }) {
                    Row(Modifier.padding(16.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.Label, null, tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.width(12.dp))
                        Text(selectedCategoryName ?: "Select category (optional)",
                            color = if (selectedCategoryName == null) MaterialTheme.colorScheme.outline
                                    else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            // Priority
            Text("Priority", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("low" to "Low", "medium" to "Medium",
                       "high" to "High", "critical" to "Critical").forEach { (v, label) ->
                    FilterChip(
                        selected = priority == v,
                        onClick = { priority = v },
                        label = { Text(label) }
                    )
                }
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
