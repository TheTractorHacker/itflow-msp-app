package com.foleyit.itflow.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.data.api.LoginRequest
import com.foleyit.itflow.data.local.AppPreferences
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    prefs: AppPreferences,
    onLoggedIn: () -> Unit,
    onChangeServer: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var totpCode by remember { mutableStateOf("") }
    var obscure by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var requires2fa by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val passRef = remember { FocusRequester() }
    val totpRef = remember { FocusRequester() }
    var serverUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { serverUrl = prefs.serverUrl.first() }

    // Auto-focus TOTP field when 2FA step appears
    LaunchedEffect(requires2fa) {
        if (requires2fa) totpRef.requestFocus()
    }

    fun login() {
        if (username.isBlank() || password.isBlank()) return
        if (requires2fa && totpCode.isBlank()) return
        loading = true; error = null

        scope.launch {
            try {
                val body = mutableMapOf<String, Any>(
                    "username" to username.trim(),
                    "password" to password,
                    "device_name" to "ITFlow MSP Android"
                )
                if (totpCode.isNotBlank()) body["totp_code"] = totpCode.trim()

                val resp = withContext(Dispatchers.IO) {
                    ApiClient.service().login(
                        LoginRequest(
                            username = username.trim(),
                            password = password,
                            device_name = "ITFlow MSP Android",
                            totp_code = if (totpCode.isNotBlank()) totpCode.trim() else null
                        )
                    )
                }

                if (resp.requires2fa == true) {
                    requires2fa = true
                    loading = false
                    return@launch
                }

                val token = resp.token ?: run {
                    error = "Login failed — no token received"
                    loading = false
                    return@launch
                }

                prefs.saveAuthData(token, resp.user!!)
                ApiClient.setToken(token)

                try {
                    val fcm = withContext(Dispatchers.IO) {
                        FirebaseMessaging.getInstance().token.await()
                    }
                    withContext(Dispatchers.IO) {
                        ApiClient.service().updateFcmToken(mapOf("fcm_token" to fcm))
                    }
                } catch (_: Exception) {}

                onLoggedIn()
            } catch (e: Exception) {
                error = when {
                    e.message?.contains("401") == true -> if (requires2fa) "Invalid 2FA code" else "Invalid username or password"
                    else -> "Login failed: ${e.message}"
                }
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                Icon(Icons.Outlined.SyncAlt, null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Sign in", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(serverUrl, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(32.dp))

        if (!requires2fa) {
            // Step 1: username + password
            OutlinedTextField(
                value = username, onValueChange = { username = it },
                label = { Text("Username or email") },
                leadingIcon = { Icon(Icons.Outlined.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { passRef.requestFocus() })
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { obscure = !obscure }) {
                        Icon(if (obscure) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, null)
                    }
                },
                visualTransformation = if (obscure) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.fillMaxWidth().focusRequester(passRef),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { login() })
            )
        } else {
            // Step 2: TOTP code
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Security, null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Text("Enter your authenticator code",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = totpCode,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) totpCode = it },
                label = { Text("6-digit code") },
                leadingIcon = { Icon(Icons.Outlined.Key, null) },
                modifier = Modifier.fillMaxWidth().focusRequester(totpRef),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { login() })
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { requires2fa = false; totpCode = "" }) {
                Text("← Back")
            }
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = ::login,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !loading
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(if (requires2fa) "Verify" else "Sign in")
        }

        if (!requires2fa) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onChangeServer, modifier = Modifier.fillMaxWidth()) {
                Text("Change server")
            }
        }
    }
}
