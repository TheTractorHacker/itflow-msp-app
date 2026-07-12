package com.foleyit.itflow.ui.screens.assets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.ui.components.EmptyScreen
import com.foleyit.itflow.ui.components.ErrorScreen
import com.foleyit.itflow.ui.components.LoadMoreRow
import com.foleyit.itflow.ui.components.LoadingScreen
import com.foleyit.itflow.ui.navigation.Screen
import com.foleyit.itflow.ui.util.rememberPagedList
import com.foleyit.itflow.ui.util.userMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(navController: NavController) {
    var search by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var types by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        types = runCatching { ApiClient.service().getAssetTypes() }.getOrDefault(emptyList())
    }

    val list = rememberPagedList<com.foleyit.itflow.data.api.AssetSummary>(selectedType) { page, q ->
        ApiClient.service().getAssets(search = q, page = page, type = selectedType ?: "")
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.ScanBarcode.route) }) {
                Icon(Icons.Outlined.DocumentScanner, "Scan Barcode")
            }
        }
    ) { scaffoldPadding ->
        Column(Modifier.fillMaxSize().padding(bottom = scaffoldPadding.calculateBottomPadding())) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it; list.onSearchChanged(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).height(48.dp),
                placeholder = { Text("Search assets…", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(18.dp)) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { search = ""; list.onSearchChanged("") }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Outlined.Clear, "Clear search", Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                textStyle = MaterialTheme.typography.bodyMedium
            )
            if (types.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedType == null,
                            onClick = { selectedType = null },
                            label = { Text("All") }
                        )
                    }
                    items(types) { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = if (selectedType == t) null else t },
                            label = { Text(t) }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            val ls = list.state
            when {
                ls.isRefreshing -> LoadingScreen()
                ls.error != null -> ErrorScreen(userMessage(ls.error), onRetry = list::retry)
                ls.items.isEmpty() -> EmptyScreen("No assets found", Icons.Outlined.Devices)
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ls.items, key = { it.id }) { a ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { navController.navigate(Screen.AssetDetail.go(a.id)) }
                            ) {
                                ListItem(
                                    headlineContent = { Text(a.name, fontWeight = FontWeight.Medium) },
                                    supportingContent = { Text(listOfNotNull(a.make, a.model).joinToString(" ")) },
                                    trailingContent = {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(a.client ?: "", style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline)
                                            Icon(Icons.Outlined.ChevronRight, null)
                                        }
                                    }
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
}
