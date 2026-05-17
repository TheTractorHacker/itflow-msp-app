package com.foleyit.itflow.ui.screens.clients

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.ui.components.ErrorScreen
import com.foleyit.itflow.ui.components.LoadingScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(id: Int, navController: NavController) {
    var state by remember { mutableStateOf<Result<com.foleyit.itflow.data.api.ClientDetail>?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    fun load() { scope.launch { state = runCatching { ApiClient.service().getClient(id) } } }
    LaunchedEffect(Unit) { load() }

    Scaffold(topBar = {
        TopAppBar(
            title = { state?.getOrNull()?.let { Text(it.name) } ?: Text("Client") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Outlined.ArrowBack, null) } }
        )
    }) { padding ->
        when {
            state == null -> LoadingScreen()
            state!!.isFailure -> ErrorScreen(state!!.exceptionOrNull()?.message ?: "")
            else -> {
                val c = state!!.getOrThrow()
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Text(c.name.first().uppercaseChar().toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(c.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    c.city?.let { Text("$it, ${c.state ?: ""}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    Spacer(Modifier.height(8.dp))
                                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                                        Text("${c.openTickets} open tickets", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                        }
                    }
                    if (c.phone != null) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.phone}"))) }, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Phone, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Call") }
                                c.website?.let { OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Language, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Website") } }
                            }
                        }
                    }
                    if (c.contacts.isNotEmpty()) {
                        item { Text("Contacts", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                c.contacts.forEachIndexed { i, contact ->
                                    ListItem(
                                        headlineContent = { Text(contact.name) },
                                        supportingContent = contact.title?.let { { Text(it) } },
                                        trailingContent = contact.phone?.let { { IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it"))) }) { Icon(Icons.Outlined.Phone, null) } } }
                                    )
                                    if (i < c.contacts.size - 1) HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
