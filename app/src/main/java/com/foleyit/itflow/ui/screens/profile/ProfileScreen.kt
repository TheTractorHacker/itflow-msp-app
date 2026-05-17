package com.foleyit.itflow.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.foleyit.itflow.data.api.*
import com.foleyit.itflow.data.local.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, prefs: AppPreferences) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var obscureCur by remember { mutableStateOf(true) }
    var obscureNew by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        profile = runCatching { ApiClient.service().getProfile() }.getOrNull()
        profile?.let { name = it.name; email = it.email }
        loading = false
    }

    fun save() {
        if (name.isBlank() || email.isBlank()) { error = "Name and email required"; return }
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
                prefs.saveAuthData(token = prefs.serverUrl.let {
                    kotlinx.coroutines.flow.first(prefs.authToken) ?: "" },
                    user = UserInfo(profile?.id ?: 0, name.trim(), email.trim(), profile?.type ?: 1))
                currentPass = ""; newPass = ""; confirmPass = ""
                snackbar.showSnackbar("Profile updated")
            } catch (e: Exception) { error = e.message } finally { saving = false }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) } },
                actions = { TextButton(onClick = ::save, enabled = !saving) {
                    if (saving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Save", fontWeight = FontWeight.SemiBold)
                }}
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (loading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return@Scaffold }
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Avatar
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(80.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Account Info", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Outlined.Person, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") },
                leadingIcon = { Icon(Icons.Outlined.Email, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            Spacer(Modifier.height(4.dp))
            Text("Change Password", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Leave blank to keep current password", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            OutlinedTextField(value = currentPass, onValueChange = { currentPass = it }, label = { Text("Current Password") },
                leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                trailingIcon = { IconButton(onClick = { obscureCur = !obscureCur }) { Icon(if (obscureCur) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, null) } },
                visualTransformation = if (obscureCur) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            OutlinedTextField(value = newPass, onValueChange = { newPass = it }, label = { Text("New Password") },
                leadingIcon = { Icon(Icons.Outlined.LockOpen, null) },
                trailingIcon = { IconButton(onClick = { obscureNew = !obscureNew }) { Icon(if (obscureNew) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, null) } },
                visualTransformation = if (obscureNew) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            OutlinedTextField(value = confirmPass, onValueChange = { confirmPass = it }, label = { Text("Confirm New Password") },
                leadingIcon = { Icon(Icons.Outlined.LockOpen, null) },
                visualTransformation = PasswordVisualTransformation(),
                isError = newPass.isNotBlank() && confirmPass.isNotBlank() && newPass != confirmPass,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(4.dp))
            Button(onClick = ::save, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = !saving) {
                if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Save Changes")
            }
        }
    }
}
