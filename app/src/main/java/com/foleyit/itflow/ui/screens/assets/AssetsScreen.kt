package com.foleyit.itflow.ui.screens.assets

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
import com.foleyit.itflow.data.api.AssetsResponse
import com.foleyit.itflow.ui.components.EmptyScreen
import com.foleyit.itflow.ui.components.ErrorScreen
import com.foleyit.itflow.ui.components.LoadingScreen
import com.foleyit.itflow.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(navController: NavController) {
    var search by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<Result<AssetsResponse>?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch { state = runCatching { ApiClient.service().getAssets(search = search) } }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assets") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search assets…") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { search = ""; load() }) {
                            Icon(Icons.Outlined.Clear, "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
            )
            when {
                state == null -> LoadingScreen()
                state!!.isFailure -> ErrorScreen(state!!.exceptionOrNull()?.message ?: "Error", onRetry = ::load)
                else -> {
                    val assets = state!!.getOrThrow().data
                    if (assets.isEmpty()) {
                        EmptyScreen("No assets found", Icons.Outlined.Devices)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(assets) { a ->
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
                        }
                    }
                }
            }
        }
    }
}
