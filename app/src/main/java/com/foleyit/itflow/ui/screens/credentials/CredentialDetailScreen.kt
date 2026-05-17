package com.foleyit.itflow.ui.screens.credentials

import android.content.Intent
import android.net.Uri
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.data.api.CredentialDetail
import com.foleyit.itflow.ui.components.ErrorScreen
import com.foleyit.itflow.ui.components.LoadingScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialDetailScreen(id: Int) {
    var authenticated by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf<Result<CredentialDetail>?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }

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
            .setSubtitle("Access credential details")
            .setNegativeButtonText("Cancel")
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(authenticated) {
        if (authenticated) {
            scope.launch { state = runCatching { ApiClient.service().getCredential(id) } }
        }
    }

    fun copy(value: String, label: String) {
        scope.launch {
            clipboard.setText(AnnotatedString(value))
            snackbarHost.showSnackbar("$label copied")
        }
    }

    if (!authenticated) {
        Scaffold { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Fingerprint, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(24.dp))
                    Text("Authentication Required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Verify your identity to view this credential", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = ::authenticate) {
                        Icon(Icons.Outlined.Fingerprint, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Authenticate")
                    }
                }
            }
        }
        return
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) { padding ->
        when {
            state == null -> LoadingScreen()
            state!!.isFailure -> ErrorScreen(state!!.exceptionOrNull()?.message ?: "")
            else -> {
                val c = state!!.getOrThrow()
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(c.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        c.client?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.outline) }
                                    }
                                }
                                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                                c.username?.takeIf { it.isNotBlank() }?.let { CredField("Username", it, Icons.Outlined.Person, onCopy = { copy(it, "Username") }) }
                                c.password?.takeIf { it.isNotBlank() }?.let { pwd ->
                                    CredField(
                                        "Password",
                                        if (showPassword) pwd else "••••••••••",
                                        Icons.Outlined.Lock,
                                        onCopy = { copy(pwd, "Password") },
                                        trailingIcon = { IconButton(onClick = { showPassword = !showPassword }, modifier = Modifier.size(32.dp)) { Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, Modifier.size(18.dp)) } }
                                    )
                                }
                                c.uri?.takeIf { it.isNotBlank() }?.let { url ->
                                    CredField("URL", url, Icons.Outlined.Link, onCopy = { copy(url, "URL") }, onTap = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) })
                                }
                                c.note?.takeIf { it.isNotBlank() }?.let {
                                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                    Text("Notes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(4.dp))
                                    Text(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CredField(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                      onCopy: (() -> Unit)? = null, onTap: (() -> Unit)? = null,
                      trailingIcon: (@Composable () -> Unit)? = null) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Surface(color = androidx.compose.ui.graphics.Color.Transparent, onClick = { onTap?.invoke() }, enabled = onTap != null) { Text(value, style = MaterialTheme.typography.bodyMedium) }
            }
            trailingIcon?.invoke()
            onCopy?.let { IconButton(onClick = it, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.ContentCopy, "Copy", Modifier.size(18.dp)) } }
        }
    }
}
