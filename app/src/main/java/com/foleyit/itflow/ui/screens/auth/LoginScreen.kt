package com.foleyit.itflow.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.NoCredentialException
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.data.api.FcmTokenRequest
import com.foleyit.itflow.data.api.LoginRequest
import com.foleyit.itflow.data.api.PasskeyCompleteRequest
import com.foleyit.itflow.data.local.AppPreferences
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(prefs: AppPreferences, onLoggedIn: () -> Unit, onChangeServer: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var totpCode by remember { mutableStateOf("") }
    var obscure by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var requires2fa by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var serverUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { serverUrl = prefs.serverUrl.first() }

    fun finishLogin(resp: com.foleyit.itflow.data.api.LoginResponse) {
        val token = resp.token ?: run { error = "No token received"; loading = false; return }
        scope.launch {
            prefs.saveAuthData(token, resp.user!!)
            ApiClient.setToken(token)
            try {
                val fcmToken = FirebaseMessaging.getInstance().token.await()
                ApiClient.service().registerFcmToken(FcmTokenRequest(fcmToken))
            } catch (_: Exception) { }
            onLoggedIn()
        }
    }

    fun login() {
        if (username.isBlank() || password.isBlank()) return
        if (requires2fa && totpCode.isBlank()) return
        loading = true; error = null
        scope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.service().login(LoginRequest(
                        username = username.trim(), password = password,
                        device_name = "ITFlow MSP Android",
                        totp_code = if (totpCode.isNotBlank()) totpCode.trim() else null
                    ))
                }
                if (resp.requires2fa == true) { requires2fa = true; loading = false; return@launch }
                finishLogin(resp)
            } catch (e: Exception) {
                error = if (requires2fa) "Invalid 2FA code" else "Invalid username or password"
            } finally { loading = false }
        }
    }

    fun loginWithPasskey() {
        loading = true; error = null
        scope.launch {
            try {
                // 1. Get challenge from server
                val beginResp = withContext(Dispatchers.IO) { ApiClient.service().passkeyBegin() }

                // 2. Build options JSON in WebAuthn format
                val optionsJson = """
                    {
                      "challenge": "${beginResp.challenge}",
                      "timeout": ${beginResp.timeout},
                      "rpId": "${beginResp.rpId}",
                      "userVerification": "${beginResp.userVerification}",
                      "allowCredentials": []
                    }
                """.trimIndent()

                // 3. Invoke OS passkey picker
                val credentialManager = CredentialManager.create(context)
                val request = GetCredentialRequest(listOf(
                    GetPublicKeyCredentialOption(requestJson = optionsJson)
                ))
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential !is PublicKeyCredential) {
                    error = "Unexpected credential type"
                    loading = false
                    return@launch
                }

                // 4. Parse assertion JSON from Android
                @Suppress("UNCHECKED_CAST")
                val assertionMap = Gson().fromJson(credential.authenticationResponseJson, Map::class.java)
                    as Map<String, Any>

                // 5. Send to server, get API token
                val completeResp = withContext(Dispatchers.IO) {
                    ApiClient.service().passkeyComplete(PasskeyCompleteRequest(
                        challengeToken = beginResp.challengeToken,
                        passkeyResponse = assertionMap
                    ))
                }
                finishLogin(completeResp)
            } catch (e: NoCredentialException) {
                error = "No passkeys found for this server. Sign in with your password first."
            } catch (e: Exception) {
                val msg = e.message ?: ""
                error = when {
                    msg.contains("cancel", ignoreCase = true) -> null
                    else -> "Passkey sign-in failed"
                }
            } finally { loading = false }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .windowInsetsPadding(WindowInsets.systemBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))

        Surface(shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.SyncAlt, null, modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Sign in", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(serverUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(40.dp))

        if (!requires2fa) {
            OutlinedTextField(value = username, onValueChange = { username = it },
                label = { Text("Username or email") }, leadingIcon = { Icon(Icons.Outlined.Person, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = password, onValueChange = { password = it },
                label = { Text("Password") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                trailingIcon = { IconButton(onClick = { obscure = !obscure }) {
                    Icon(if (obscure) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, null)
                }},
                visualTransformation = if (obscure) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { login() }))
        } else {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Security, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Text("Enter your authenticator code",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = totpCode,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) totpCode = it },
                label = { Text("6-digit code") }, leadingIcon = { Icon(Icons.Outlined.Key, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { login() }))
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { requires2fa = false; totpCode = "" }) { Text("← Back") }
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = ::login, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = !loading) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(if (requires2fa) "Verify" else "Sign in")
        }

        if (!requires2fa) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = ::loginWithPasskey,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !loading
            ) {
                Icon(Icons.Outlined.Fingerprint, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign in with passkey", fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onChangeServer, modifier = Modifier.fillMaxWidth()) { Text("Change server") }
        }
        Spacer(Modifier.height(32.dp))
    }
}
