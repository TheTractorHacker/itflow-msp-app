package com.foleyit.itflow.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.data.api.SearchResult
import com.foleyit.itflow.ui.navigation.Screen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    var query by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<SearchResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) { focus.requestFocus() }

    fun search(q: String) {
        searchJob?.cancel()
        error = null
        if (q.length < 2) { result = null; return }
        searchJob = scope.launch {
            delay(300)
            loading = true
            val r = runCatching { ApiClient.service().search(q) }
            result = r.getOrNull()
            error = r.exceptionOrNull()?.localizedMessage
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; search(it) },
                        modifier = Modifier.fillMaxWidth().height(56.dp).focusRequester(focus),
                        placeholder = { Text("Search…", style = MaterialTheme.typography.bodyMedium) },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = ""; result = null; error = null },
                                    modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Outlined.Clear, null, Modifier.size(16.dp))
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error)
                        Text("Search failed", style = MaterialTheme.typography.titleSmall)
                        Text(error ?: "", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            query.length < 2 -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Type at least 2 characters to search",
                        color = MaterialTheme.colorScheme.outline)
                }
            }
            result == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No results for \"$query\"", color = MaterialTheme.colorScheme.outline)
                }
            }
            else -> {
                val r = result!!
                val hasResults = r.tickets.isNotEmpty() || r.clients.isNotEmpty() || r.assets.isNotEmpty()
                if (!hasResults) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("No results for \"$query\"", color = MaterialTheme.colorScheme.outline)
                    }
                    return@Scaffold
                }
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (r.tickets.isNotEmpty()) {
                        item { SectionHeader("Tickets", Icons.Outlined.ConfirmationNumber) }
                        items(r.tickets) { t ->
                            Card(modifier = Modifier.fillMaxWidth(),
                                onClick = { navController.navigate(Screen.TicketDetail.go(t.id)) }) {
                                ListItem(
                                    headlineContent = { Text("#${t.number} ${t.subject}", maxLines = 1) },
                                    supportingContent = { Text("${t.status ?: ""} · ${t.client ?: ""}") },
                                    trailingContent = { Icon(Icons.Outlined.ChevronRight, null) }
                                )
                            }
                        }
                    }
                    if (r.clients.isNotEmpty()) {
                        item { SectionHeader("Clients", Icons.Outlined.Business) }
                        items(r.clients) { c ->
                            Card(modifier = Modifier.fillMaxWidth(),
                                onClick = { navController.navigate(Screen.ClientDetail.go(c.id)) }) {
                                ListItem(
                                    headlineContent = { Text(c.name) },
                                    trailingContent = { Icon(Icons.Outlined.ChevronRight, null) }
                                )
                            }
                        }
                    }
                    if (r.assets.isNotEmpty()) {
                        item { SectionHeader("Assets", Icons.Outlined.Devices) }
                        items(r.assets) { a ->
                            Card(modifier = Modifier.fillMaxWidth(),
                                onClick = { navController.navigate(Screen.AssetDetail.go(a.id)) }) {
                                ListItem(
                                    headlineContent = { Text(a.name) },
                                    supportingContent = {
                                        Text("${listOfNotNull(a.make, a.model).joinToString(" ")} · ${a.client ?: ""}")
                                    },
                                    trailingContent = { Icon(Icons.Outlined.ChevronRight, null) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(title, style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}
