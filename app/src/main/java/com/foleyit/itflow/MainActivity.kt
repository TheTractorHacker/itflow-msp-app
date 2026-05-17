package com.foleyit.itflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.ui.navigation.Screen
import com.foleyit.itflow.ui.screens.auth.LoginScreen
import com.foleyit.itflow.ui.screens.auth.ServerSetupScreen
import com.foleyit.itflow.ui.screens.main.MainScreen
import com.foleyit.itflow.ui.theme.ITFlowTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    NavHost(navController, startDestination = startDestination) {
                        composable(Screen.Setup.route) {
                            ServerSetupScreen(
                                prefs = prefs,
                                onDone = {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Setup.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.Login.route) {
                            LoginScreen(
                                prefs = prefs,
                                onLoggedIn = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                },
                                onChangeServer = {
                                    navController.navigate(Screen.Setup.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.Dashboard.route) {
                            MainScreen(
                                prefs = prefs,
                                onLoggedOut = {
                                    ApiClient.clearToken()
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
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
