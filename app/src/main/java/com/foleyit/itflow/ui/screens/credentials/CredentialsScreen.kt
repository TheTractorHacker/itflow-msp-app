package com.foleyit.itflow.ui.screens.credentials

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.ui.components.*
import com.foleyit.itflow.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialsScreen(navController: NavController) {
    var authenticated by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<Result<com.foleyit.itflow.data.api.CredentialsResponse>?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun authenticate() {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                authenticated = true
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verify identity")
            .setSubtitle("Access credentials")
            .setNegativeButtonText("Cancel")
            .build()
        prompt.authenticate(info)
    }

    fun load() { scope.launch { state = runCatching { ApiClient.service().getCredentials(search = search) } } }

    if (!authenticated) {
        Scaffold(topBar = { TopAppBar(title = { Text("Credentials") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Outlined.ArrowBack, null) } }) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Fingerprint, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(24.dp))
                    Text("Authentication Required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Verify your identity to view credentials", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = ::authenticate) { Icon(Icons.Outlined.Fingerprint, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Authenticate") }
                }
            }
        }
        return
    }

    LaunchedEffect(search) { load() }

    Scaffold(topBar = { TopAppBar(title = { Text("Credentials") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Outlined.ArrowBack, null) } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search credentials…") },
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
                    val creds = state!!.getOrThrow().data
                    if (creds.isEmpty()) EmptyScreen("No credentials found", Icons.Outlined.Lock)
                    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(creds) { c ->
                            Card(modifier = Modifier.fillMaxWidth(), onClick = { navController.navigate(Screen.CredDetail.go(c.id)) }) {
                                ListItem(
                                    headlineContent = { Text(c.name, fontWeight = FontWeight.Medium) },
                                    supportingContent = { Text(c.username ?: "") },
                                    leadingContent = {
                                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp)) }
                                        }
                                    },
                                    trailingContent = { Column(horizontalAlignment = Alignment.End) { Text(c.client ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline); Icon(Icons.Outlined.ChevronRight, null) } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
