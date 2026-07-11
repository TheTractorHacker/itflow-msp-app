package com.foleyit.itflow.ui.screens.invoices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
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
import com.foleyit.itflow.ui.util.rememberPagedList
import com.foleyit.itflow.ui.util.userMessage
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(navController: NavController) {
    val currency = NumberFormat.getCurrencyInstance(Locale.US)
    val list = rememberPagedList<com.foleyit.itflow.data.api.InvoiceSummary> { page, _ ->
        ApiClient.service().getInvoices(page = page)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Invoices") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }) }) { padding ->
        val ls = list.state
        when {
            ls.isRefreshing -> LoadingScreen()
            ls.error != null -> ErrorScreen(userMessage(ls.error), onRetry = list::retry)
            ls.items.isEmpty() -> EmptyScreen("No invoices", Icons.AutoMirrored.Outlined.ReceiptLong)
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ls.items, key = { it.id }) { inv ->
                        val statusColor: Color = when (inv.status) { "Paid" -> Color(0xFF2E7D32); "Overdue" -> MaterialTheme.colorScheme.error; "Partial" -> Color(0xFFE65100); else -> MaterialTheme.colorScheme.outline }
                        Card(modifier = Modifier.fillMaxWidth(), onClick = { navController.navigate(Screen.InvoiceDetail.go(inv.id)) }) {
                            ListItem(
                                headlineContent = { Text("Invoice #${inv.number}", fontWeight = FontWeight.Medium) },
                                supportingContent = { Text("${inv.client ?: ""} · Due ${inv.dueDate ?: ""}") },
                                leadingContent = { Surface(shape = MaterialTheme.shapes.medium, color = statusColor.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Outlined.ReceiptLong, null, tint = statusColor, modifier = Modifier.size(22.dp)) } } },
                                trailingContent = { Column(horizontalAlignment = Alignment.End) { Text(currency.format(inv.total ?: 0.0), fontWeight = FontWeight.Bold); Text(inv.status ?: "", style = MaterialTheme.typography.labelSmall, color = statusColor) } }
                            )
                        }
                    }
                    if (ls.hasMore) {
                        item(key = "load_more") { LoadMoreRow(ls.isLoadingMore, list::loadMore) }
                    }
                }
            }
        }
    }
}
