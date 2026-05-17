package com.foleyit.itflow.ui.screens.invoices

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
import androidx.navigation.NavController
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.ui.components.*
import com.foleyit.itflow.ui.navigation.Screen
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(navController: NavController) {
    var state by remember { mutableStateOf<Result<com.foleyit.itflow.data.api.InvoicesResponse>?>(null) }
    val scope = rememberCoroutineScope()
    val currency = NumberFormat.getCurrencyInstance(Locale.US)
    fun load() { scope.launch { state = runCatching { ApiClient.service().getInvoices() } } }
    LaunchedEffect(Unit) { load() }

    Scaffold(topBar = { TopAppBar(title = { Text("Invoices") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Outlined.ArrowBack, null) } }) }) { padding ->
        when {
            state == null -> LoadingScreen()
            state!!.isFailure -> ErrorScreen(state!!.exceptionOrNull()?.message ?: "", onRetry = ::load)
            else -> {
                val invoices = state!!.getOrThrow().data
                if (invoices.isEmpty()) EmptyScreen("No invoices", Icons.Outlined.ReceiptLong)
                else LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(invoices) { inv ->
                        val statusColor: Color = when (inv.status) { "Paid" -> Color(0xFF2E7D32); "Overdue" -> MaterialTheme.colorScheme.error; "Partial" -> Color(0xFFE65100); else -> MaterialTheme.colorScheme.outline }
                        Card(Modifier.fillMaxWidth(), onClick = { navController.navigate(Screen.InvoiceDetail.go(inv.id)) }) {
                            ListItem(
                                headlineContent = { Text("Invoice #${inv.number}", fontWeight = FontWeight.Medium) },
                                supportingContent = { Text("${inv.client ?: ""} · Due ${inv.dueDate ?: ""}") },
                                leadingContent = { Surface(shape = MaterialTheme.shapes.medium, color = statusColor.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.ReceiptLong, null, tint = statusColor, modifier = Modifier.size(22.dp)) } } },
                                trailingContent = { Column(horizontalAlignment = Alignment.End) { Text(currency.format(inv.total ?: 0.0), fontWeight = FontWeight.Bold); Text(inv.status ?: "", style = MaterialTheme.typography.labelSmall, color = statusColor) } }
                            )
                        }
                    }
                }
            }
        }
    }
}
