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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(
    prefs: AppPreferences,
    onLoggedIn: () -> Unit,
    onChangeServer: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var obscure by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val passRef = remember { FocusRequester() }
    var serverUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { serverUrl = prefs.serverUrl.first() }

    fun login() {
        if (username.isBlank() || password.isBlank()) return
        loading = true; error = null
        scope.launch {
            try {
                val resp = ApiClient.service().login(LoginRequest(username.trim(), password, "ITFlow MSP Android"))
                prefs.saveAuthData(resp.token, resp.user)
                ApiClient.setToken(resp.token)
                // Register FCM token
                try {
                    val fcm = FirebaseMessaging.getInstance().token.await()
                    ApiClient.service().updateFcmToken(mapOf("fcm_token" to fcm))
                } catch (_: Exception) {}
                onLoggedIn()
            } catch (e: Exception) {
                error = "Invalid username or password"
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
            else Text("Sign in")
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onChangeServer, modifier = Modifier.fillMaxWidth()) {
            Text("Change server")
        }
    }
}
