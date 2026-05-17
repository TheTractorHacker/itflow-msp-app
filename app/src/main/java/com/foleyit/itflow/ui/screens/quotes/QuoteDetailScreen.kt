package com.foleyit.itflow.ui.screens.quotes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.data.api.QuoteDetail
import com.foleyit.itflow.ui.components.ErrorScreen
import com.foleyit.itflow.ui.components.LoadingScreen
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDetailScreen(id: Int) {
    var state by remember { mutableStateOf<Result<QuoteDetail>?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currency = NumberFormat.getCurrencyInstance(Locale.US)
    LaunchedEffect(Unit) { scope.launch { state = runCatching { ApiClient.service().getQuote(id) } } }

    when {
        state == null -> LoadingScreen()
        state!!.isFailure -> ErrorScreen(state!!.exceptionOrNull()?.message ?: "")
        else -> {
            val q = state!!.getOrThrow()
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(q.subject ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                q.guestUrl?.let {
                                    val url = "${ApiClient.serverUrl}$it"
                                    IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }) { Icon(Icons.Outlined.OpenInBrowser, null) }
                                }
                            }
                            Text(q.client ?: "", color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) { Text("${q.date ?: ""}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall) }
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) { Text(q.status ?: "", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Line Items", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            (q.items).forEach { item ->
                                Row(Modifier.padding(bottom = 12.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text(item.description ?: "", fontWeight = FontWeight.Medium)
                                        Text("${item.quantity} × ${currency.format(item.unitPrice ?: 0.0)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Text(currency.format(item.total ?: 0.0), fontWeight = FontWeight.Bold)
                                }
                            }
                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))
                            listOf("Subtotal" to q.subtotal, "Tax" to q.tax, "Total" to q.total).forEach { (l, v) ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(l, fontWeight = if (l == "Total") FontWeight.Bold else FontWeight.Normal)
                                    Text(currency.format(v ?: 0.0), fontWeight = if (l == "Total") FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
