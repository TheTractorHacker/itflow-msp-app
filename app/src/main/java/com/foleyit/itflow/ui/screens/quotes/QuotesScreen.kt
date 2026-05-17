package com.foleyit.itflow.ui.screens.quotes

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
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotesScreen(navController: NavController) {
    var state by remember { mutableStateOf<Result<com.foleyit.itflow.data.api.QuotesResponse>?>(null) }
    val scope = rememberCoroutineScope()
    val currency = NumberFormat.getCurrencyInstance(Locale.US)
    fun load() { scope.launch { state = runCatching { ApiClient.service().getQuotes() } } }
    LaunchedEffect(Unit) { load() }

    Scaffold(topBar = { TopAppBar(title = { Text("Quotes") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) } }) }) { padding ->
        when {
            state == null -> LoadingScreen()
            state!!.isFailure -> ErrorScreen(state!!.exceptionOrNull()?.message ?: "", onRetry = ::load)
            else -> {
                val quotes = state!!.getOrThrow().data
                if (quotes.isEmpty()) EmptyScreen("No quotes", Icons.Outlined.RequestQuote)
                else LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quotes) { q ->
                        val statusColor = when (q.status) { "Accepted" -> Color(0xFF2E7D32); "Declined" -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.outline }
                        Card(modifier = Modifier.fillMaxWidth(), onClick = { navController.navigate(Screen.QuoteDetail.go(q.id)) }) {
                            ListItem(
                                headlineContent = { Text(q.subject ?: "", fontWeight = FontWeight.Medium) },
                                supportingContent = { Text("${q.client ?: ""} · ${q.date ?: ""}") },
                                trailingContent = {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(currency.format(q.total ?: 0.0), fontWeight = FontWeight.Bold)
                                        Surface(color = statusColor.copy(alpha = 0.12f), shape = MaterialTheme.shapes.extraSmall) { Text(q.status ?: "", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = statusColor) }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
