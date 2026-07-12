package com.foleyit.itflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Stored theme preference values (see AppPreferences.THEME_MODE). */
object ThemeMode {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"
}

// M3 tonal palette derived from ITFlow MSP seed #006875 (teal)
private val light_primary              = Color(0xFF006875)
private val light_onPrimary            = Color(0xFFFFFFFF)
private val light_primaryContainer     = Color(0xFF97F0FF)
private val light_onPrimaryContainer   = Color(0xFF001F24)
private val light_secondary            = Color(0xFF4A6267)
private val light_onSecondary          = Color(0xFFFFFFFF)
private val light_secondaryContainer   = Color(0xFFCCE7EC)
private val light_onSecondaryContainer = Color(0xFF051F23)
private val light_tertiary             = Color(0xFF525E7A)
private val light_onTertiary           = Color(0xFFFFFFFF)
private val light_tertiaryContainer    = Color(0xFFDAE2FF)
private val light_onTertiaryContainer  = Color(0xFF0D1B36)
private val light_error                = Color(0xFFBA1A1A)
private val light_onError              = Color(0xFFFFFFFF)
private val light_errorContainer       = Color(0xFFFFDAD6)
private val light_onErrorContainer     = Color(0xFF410002)
private val light_background           = Color(0xFFF5FAFA)
private val light_onBackground         = Color(0xFF191C1D)
private val light_surface              = Color(0xFFF5FAFA)
private val light_onSurface            = Color(0xFF191C1D)
private val light_surfaceVariant       = Color(0xFFDBE4E6)
private val light_onSurfaceVariant     = Color(0xFF3F484A)
private val light_outline              = Color(0xFF6F797B)
private val light_outlineVariant       = Color(0xFFBFC8CA)
private val light_inverseSurface       = Color(0xFF2D3132)
private val light_inverseOnSurface     = Color(0xFFEFF1F1)
private val light_inversePrimary       = Color(0xFF4FD8EB)

private val dark_primary               = Color(0xFF4FD8EB)
private val dark_onPrimary             = Color(0xFF00363D)
private val dark_primaryContainer      = Color(0xFF004E58)
private val dark_onPrimaryContainer    = Color(0xFF97F0FF)
private val dark_secondary             = Color(0xFFB1CBD0)
private val dark_onSecondary           = Color(0xFF1B343A)
private val dark_secondaryContainer    = Color(0xFF324B51)
private val dark_onSecondaryContainer  = Color(0xFFCCE7EC)
private val dark_tertiary              = Color(0xFFBAC6E8)
private val dark_onTertiary            = Color(0xFF232F4B)
private val dark_tertiaryContainer     = Color(0xFF3A4663)
private val dark_onTertiaryContainer   = Color(0xFFDAE2FF)
private val dark_error                 = Color(0xFFFFB4AB)
private val dark_onError               = Color(0xFF690005)
private val dark_errorContainer        = Color(0xFF93000A)
private val dark_onErrorContainer      = Color(0xFFFFDAD6)
private val dark_background            = Color(0xFF191C1D)
private val dark_onBackground          = Color(0xFFE1E3E3)
private val dark_surface               = Color(0xFF191C1D)
private val dark_onSurface             = Color(0xFFE1E3E3)
private val dark_surfaceVariant        = Color(0xFF3F484A)
private val dark_onSurfaceVariant      = Color(0xFFBFC8CA)
private val dark_outline               = Color(0xFF899294)
private val dark_outlineVariant        = Color(0xFF3F484A)
private val dark_inverseSurface        = Color(0xFFE1E3E3)
private val dark_inverseOnSurface      = Color(0xFF2D3132)
private val dark_inversePrimary        = Color(0xFF006875)

private val LightColors = lightColorScheme(
    primary = light_primary,
    onPrimary = light_onPrimary,
    primaryContainer = light_primaryContainer,
    onPrimaryContainer = light_onPrimaryContainer,
    secondary = light_secondary,
    onSecondary = light_onSecondary,
    secondaryContainer = light_secondaryContainer,
    onSecondaryContainer = light_onSecondaryContainer,
    tertiary = light_tertiary,
    onTertiary = light_onTertiary,
    tertiaryContainer = light_tertiaryContainer,
    onTertiaryContainer = light_onTertiaryContainer,
    error = light_error,
    onError = light_onError,
    errorContainer = light_errorContainer,
    onErrorContainer = light_onErrorContainer,
    background = light_background,
    onBackground = light_onBackground,
    surface = light_surface,
    onSurface = light_onSurface,
    surfaceVariant = light_surfaceVariant,
    onSurfaceVariant = light_onSurfaceVariant,
    outline = light_outline,
    outlineVariant = light_outlineVariant,
    inverseSurface = light_inverseSurface,
    inverseOnSurface = light_inverseOnSurface,
    inversePrimary = light_inversePrimary,
)

private val DarkColors = darkColorScheme(
    primary = dark_primary,
    onPrimary = dark_onPrimary,
    primaryContainer = dark_primaryContainer,
    onPrimaryContainer = dark_onPrimaryContainer,
    secondary = dark_secondary,
    onSecondary = dark_onSecondary,
    secondaryContainer = dark_secondaryContainer,
    onSecondaryContainer = dark_onSecondaryContainer,
    tertiary = dark_tertiary,
    onTertiary = dark_onTertiary,
    tertiaryContainer = dark_tertiaryContainer,
    onTertiaryContainer = dark_onTertiaryContainer,
    error = dark_error,
    onError = dark_onError,
    errorContainer = dark_errorContainer,
    onErrorContainer = dark_onErrorContainer,
    background = dark_background,
    onBackground = dark_onBackground,
    surface = dark_surface,
    onSurface = dark_onSurface,
    surfaceVariant = dark_surfaceVariant,
    onSurfaceVariant = dark_onSurfaceVariant,
    outline = dark_outline,
    outlineVariant = dark_outlineVariant,
    inverseSurface = dark_inverseSurface,
    inverseOnSurface = dark_inverseOnSurface,
    inversePrimary = dark_inversePrimary,
)

@Composable
fun ITFlowTheme(themeMode: String = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    // Always the branded ITFlow teal palette — no Material You dynamic/wallpaper-derived
    // color, so the app looks the same (and deliberately designed) on every device.
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(colorScheme = colorScheme, content = content)
}
