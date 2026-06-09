package com.foleyit.itflow

import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.data.local.AppPreferences
import com.foleyit.itflow.ui.navigation.Screen
import com.foleyit.itflow.ui.screens.auth.LoginScreen
import com.foleyit.itflow.ui.screens.auth.ServerSetupScreen
import com.foleyit.itflow.ui.screens.main.MainScreen
import com.foleyit.itflow.ui.theme.ITFlowTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class MainActivity : FragmentActivity() {

    private var backgroundedAt = 0L

    override fun onPause() {
        super.onPause()
        backgroundedAt = System.currentTimeMillis()
    }

    override fun onDestroy() {
        super.onDestroy()
        ApiClient.onUnauthorized = null
    }

    private fun isRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su"
        )
        return paths.any { File(it).exists() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRooted()) android.util.Log.w("ITFlow", "Device appears rooted.")
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()

        val prefs = (application as ITFlowApplication).prefs

        val startDestination = runBlocking {
            val url   = prefs.serverUrl.first()
            val token = prefs.authToken.first()
            when {
                url.isBlank() -> Screen.Setup.route
                token == null -> Screen.Login.route
                else          -> Screen.Dashboard.route
            }
        }

        setContent {
            ITFlowTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    var isLocked by remember { mutableStateOf(false) }
                    val biometricEnabled by prefs.biometricLock.collectAsState(initial = false)

                    // Wire 401 auto-logout
                    LaunchedEffect(Unit) {
                        ApiClient.onUnauthorized = {
                            MainScope().launch {
                                prefs.clearAuth()
                                ApiClient.clearToken()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }

                    // App lock — re-lock after 5 minutes in background
                    DisposableEffect(biometricEnabled) {
                        val resumeCallback = Runnable {
                            if (biometricEnabled && backgroundedAt > 0 &&
                                System.currentTimeMillis() - backgroundedAt > 5 * 60_000L) {
                                isLocked = true
                            }
                        }
                        onDispose {}
                    }

                    if (isLocked) {
                        BiometricLockScreen(prefs) { isLocked = false }
                        return@Surface
                    }

                    NavHost(navController, startDestination = startDestination) {
                        composable(Screen.Setup.route) {
                            ServerSetupScreen(prefs = prefs, onDone = {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Setup.route) { inclusive = true }
                                }
                            })
                        }
                        composable(Screen.Login.route) {
                            LoginScreen(prefs = prefs,
                                onLoggedIn = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                },
                                onChangeServer = {
                                    navController.navigate(Screen.Setup.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                })
                        }
                        composable(Screen.Dashboard.route) {
                            MainScreen(prefs = prefs, onLoggedOut = {
                                ApiClient.clearToken()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BiometricLockScreen(prefs: AppPreferences, onUnlocked: () -> Unit) {
    val activity = androidx.compose.ui.platform.LocalContext.current as FragmentActivity
    val scope = rememberCoroutineScope()

    fun authenticate() {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onUnlocked()
            }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("ITFlow MSP")
                .setSubtitle("Authenticate to continue")
                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("Sign out")
                .build()
        )
    }

    LaunchedEffect(Unit) { authenticate() }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Outlined.Fingerprint, null, Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary)
            Text("ITFlow MSP", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Text("Authentication required", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { authenticate() }) { Text("Unlock") }
            TextButton(onClick = {
                scope.launch { prefs.clearAuth() }
                ApiClient.clearToken()
            }) { Text("Sign out") }
        }
    }
}
