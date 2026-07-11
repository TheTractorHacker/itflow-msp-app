package com.foleyit.itflow.ui.screens.profile

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.foleyit.itflow.BuildConfig
import com.foleyit.itflow.data.api.*
import com.foleyit.itflow.data.local.AppPreferences
import com.foleyit.itflow.ui.navigation.Screen
import com.foleyit.itflow.ui.util.userMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, prefs: AppPreferences, onChangeServer: () -> Unit = {}) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var obscureCur by remember { mutableStateOf(true) }
    var obscureNew by remember { mutableStateOf(true) }
    var obscureConfirm by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showChangeServerConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        profile = runCatching { ApiClient.service().getProfile() }.getOrNull()
        profile?.let { name = it.name; email = it.email }
        loading = false
    }

    fun save() {
        if (name.isBlank() || email.isBlank()) { error = "Name and email are required"; return }
        if (newPass.isNotBlank() && newPass != confirmPass) { error = "New passwords don't match"; return }
        if (newPass.isNotBlank() && currentPass.isBlank()) { error = "Enter current password to change it"; return }
        saving = true; error = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ApiClient.service().updateProfile(UpdateProfileRequest(
                        name = name.trim(), email = email.trim(),
                        currentPassword = currentPass, newPassword = newPass
                    ))
                }
                val currentToken = prefs.authToken.first() ?: ""
                prefs.saveAuthData(currentToken,
                    UserInfo(profile?.id ?: 0, name.trim(), email.trim(), profile?.type ?: 1))
                currentPass = ""; newPass = ""; confirmPass = ""
                snackbar.showSnackbar("Profile updated successfully")
            } catch (e: Exception) {
                error = userMessage(e)
            } finally {
                saving = false
            }
        }
    }

    if (showChangeServerConfirm) {
        AlertDialog(
            onDismissRequest = { showChangeServerConfirm = false },
            title = { Text("Change Server?") },
            text = { Text("You'll be signed out of ${ApiClient.serverUrl} to connect to a different server.") },
            confirmButton = {
                TextButton(onClick = { showChangeServerConfirm = false; onChangeServer() }) { Text("Sign Out & Change") }
            },
            dismissButton = {
                TextButton(onClick = { showChangeServerConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Avatar header ────────────────────────────────────────────
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        name.ifBlank { "Your Name" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        email.ifBlank { "your@email.com" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // ── Account Info card ─────────────────────────────────────────
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Account Info",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = name, onValueChange = { name = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Outlined.Person, null) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        OutlinedTextField(
                            value = email, onValueChange = { email = it },
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Outlined.Email, null) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        Button(
                            onClick = ::save,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !saving
                        ) {
                            if (saving) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Save Profile")
                        }
                    }
                }

                // ── Change Password card ──────────────────────────────────────
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Lock, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Change Password",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold)
                                Text("Leave blank to keep your current password",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        HorizontalDivider()
                        OutlinedTextField(
                            value = currentPass, onValueChange = { currentPass = it },
                            label = { Text("Current Password") },
                            leadingIcon = { Icon(Icons.Outlined.LockOpen, null) },
                            trailingIcon = {
                                IconButton(onClick = { obscureCur = !obscureCur }) {
                                    Icon(if (obscureCur) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, if (obscureCur) "Show password" else "Hide password")
                                }
                            },
                            visualTransformation = if (obscureCur) PasswordVisualTransformation() else VisualTransformation.None,
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        OutlinedTextField(
                            value = newPass, onValueChange = { newPass = it },
                            label = { Text("New Password") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                            trailingIcon = {
                                IconButton(onClick = { obscureNew = !obscureNew }) {
                                    Icon(if (obscureNew) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, if (obscureNew) "Show password" else "Hide password")
                                }
                            },
                            visualTransformation = if (obscureNew) PasswordVisualTransformation() else VisualTransformation.None,
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        OutlinedTextField(
                            value = confirmPass, onValueChange = { confirmPass = it },
                            label = { Text("Confirm New Password") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                            trailingIcon = {
                                IconButton(onClick = { obscureConfirm = !obscureConfirm }) {
                                    Icon(if (obscureConfirm) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, if (obscureConfirm) "Show password" else "Hide password")
                                }
                            },
                            visualTransformation = if (obscureConfirm) PasswordVisualTransformation() else VisualTransformation.None,
                            isError = newPass.isNotBlank() && confirmPass.isNotBlank() && newPass != confirmPass,
                            supportingText = if (newPass.isNotBlank() && confirmPass.isNotBlank() && newPass != confirmPass) {
                                { Text("Passwords don't match") }
                            } else null,
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        Button(
                            onClick = ::save,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !saving && currentPass.isNotBlank() && newPass.isNotBlank()
                        ) {
                            Text("Change Password")
                        }
                    }
                }

                error?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ErrorOutline, null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // ── App Settings card ───────────────────────────────────────
                val biometricEnabled by prefs.biometricLock.collectAsState(initial = false)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                        Text("App Settings",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp))

                        // Biometric Lock row
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Fingerprint, null,
                                            Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Biometric Lock",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium)
                                    Text("Lock after 5 min in background",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline)
                                }
                                Switch(
                                    checked = biometricEnabled,
                                    onCheckedChange = { scope.launch { prefs.setBiometricLock(it) } }
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Reports row
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { navController.navigate(Screen.ReportsHub.route) }
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.BarChart, null,
                                            Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Reports",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium)
                                    Text("Tickets, financials, and more",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline)
                                }
                                Icon(Icons.Outlined.ChevronRight, null,
                                    tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        // Knowledge Base row
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { navController.navigate(Screen.KnowledgeBase.route) }
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.AutoMirrored.Outlined.Article, null,
                                            Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Knowledge Base",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium)
                                    Text("Browse help articles",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline)
                                }
                                Icon(Icons.Outlined.ChevronRight, null,
                                    tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        // Notifications row
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                })
                            }
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Notifications, null,
                                            Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Notifications",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium)
                                    Text("Manage push notification settings",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline)
                                }
                                Icon(Icons.AutoMirrored.Outlined.OpenInNew, null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        // Server row
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showChangeServerConfirm = true }
                        ) {
                            Row(
                                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Dns, null,
                                            Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Server",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium)
                                    Text(ApiClient.serverUrl,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1)
                                }
                                Text("Change", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "ITFlow MSP ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

