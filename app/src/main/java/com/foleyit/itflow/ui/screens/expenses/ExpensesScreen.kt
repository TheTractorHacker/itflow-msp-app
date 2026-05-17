package com.foleyit.itflow.ui.screens.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun ExpensesScreen(navController: NavController) {
    var state by remember { mutableStateOf<Result<com.foleyit.itflow.data.api.ExpensesResponse>?>(null) }
    val scope = rememberCoroutineScope()
    val currency = NumberFormat.getCurrencyInstance(Locale.US)
    fun load() { scope.launch { state = runCatching { ApiClient.service().getExpenses() } } }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Expenses") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Outlined.ArrowBack, null) } }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.AddExpense.route) },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("Add Expense") }
            )
        }
    ) { padding ->
        when {
            state == null -> LoadingScreen()
            state!!.isFailure -> ErrorScreen(state!!.exceptionOrNull()?.message ?: "", onRetry = ::load)
            else -> {
                val expenses = state!!.getOrThrow().data
                if (expenses.isEmpty()) EmptyScreen("No expenses. Tap + to add one.", Icons.Outlined.Receipt)
                else LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(expenses) { e ->
                        Card(Modifier.fillMaxWidth()) {
                            ListItem(
                                headlineContent = { Text(e.description ?: "", fontWeight = FontWeight.Medium) },
                                supportingContent = { Text("${e.date ?: ""} · ${e.paymentMethod ?: ""}") },
                                leadingContent = {
                                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(40.dp)) {
                                        Box(contentAlignment = Alignment.Center) { Icon(if (e.hasReceipt) Icons.Outlined.ReceiptLong else Icons.Outlined.Receipt, null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(22.dp)) }
                                    }
                                },
                                trailingContent = {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(currency.format(e.amount ?: 0.0), fontWeight = FontWeight.Bold)
                                        e.client?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) }
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
