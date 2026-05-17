package com.foleyit.itflow.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val itflowBlue = Color(0xFF0D6EFD)

private val LightColors = lightColorScheme(primary = itflowBlue)
private val DarkColors  = darkColorScheme(primary = itflowBlue)

@Composable
fun ITFlowTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()

    // Material You dynamic color on Android 12+, fallback to ITFlow blue
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else      -> LightColors
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
