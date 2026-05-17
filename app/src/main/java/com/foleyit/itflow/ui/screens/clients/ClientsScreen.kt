package com.foleyit.itflow.ui.screens.clients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.ui.components.*
import com.foleyit.itflow.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(navController: NavController) {
    var search by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<Result<com.foleyit.itflow.data.api.ClientsResponse>?>(null) }
    val scope = rememberCoroutineScope()

    fun load() { scope.launch { state = runCatching { ApiClient.service().getClients(search = search) } } }
    LaunchedEffect(search) { load() }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search clients…") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = {
                if (search.isNotEmpty()) IconButton(onClick = { search = ""; load() }) { Icon(Icons.Outlined.Clear, null) }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
        )
        when {
            state == null -> LoadingScreen()
            state!!.isFailure -> ErrorScreen(state!!.exceptionOrNull()?.message ?: "", onRetry = ::load)
            else -> {
                val clients = state!!.getOrThrow().data
                if (clients.isEmpty()) EmptyScreen("No clients found", Icons.Outlined.Business)
                else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(clients) { c ->
                        Card(modifier = Modifier.fillMaxWidth(), onClick = { navController.navigate(Screen.ClientDetail.go(c.id)) }) {
                            ListItem(
                                headlineContent = { Text(c.name, fontWeight = FontWeight.Medium) },
                                supportingContent = if (c.city != null) {{ Text("${c.city}, ${c.state ?: ""}") }} else null,
                                leadingContent = {
                                    Surface(shape = MaterialTheme.shapes.extraLarge,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(40.dp)) {
                                        Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                            Text(c.name.first().uppercaseChar().toString(),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
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
