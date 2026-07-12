package com.foleyit.itflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
// internal (not private): reused by StatusColors.kt so semantic tokens stay
// perfectly in sync with the ColorScheme roles they alias (secondary/tertiary/error).
internal val light_secondary            = Color(0xFF4A6267)
private val light_onSecondary          = Color(0xFFFFFFFF)
private val light_secondaryContainer   = Color(0xFFCCE7EC)
private val light_onSecondaryContainer = Color(0xFF051F23)
internal val light_tertiary             = Color(0xFF525E7A)
private val light_onTertiary           = Color(0xFFFFFFFF)
private val light_tertiaryContainer    = Color(0xFFDAE2FF)
private val light_onTertiaryContainer  = Color(0xFF0D1B36)
internal val light_error                = Color(0xFFBA1A1A)
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

// Teal-neutral surface container ladder (M3 tonal roles), hue/sat-matched to
// light_background/light_surface above. Without these, Compose falls back to
// the stock M3 baseline (purple-seeded) neutral grays for Card/Sheet/Menu
// containers — a subtly-off, "generic template" mismatch sitting right next
// to our teal surface that is a real, if easy-to-miss, contributor to the
// app reading as undesigned. Values derived from the same hue as the
// existing background/surfaceVariant tokens, stepped across the standard M3
// tone stops (100/98/96/94/92/90/87).
private val light_surfaceDim            = Color(0xFFD3E9E9)
private val light_surfaceBright         = Color(0xFFF8FCFC)
private val light_surfaceContainerLowest  = Color(0xFFFFFFFF)
private val light_surfaceContainerLow     = Color(0xFFF1F8F8)
private val light_surfaceContainer        = Color(0xFFEBF5F5)
private val light_surfaceContainerHigh    = Color(0xFFE4F1F1)
private val light_surfaceContainerHighest = Color(0xFFDDEEEE)

private val dark_primary               = Color(0xFF4FD8EB)
private val dark_onPrimary             = Color(0xFF00363D)
private val dark_primaryContainer      = Color(0xFF004E58)
private val dark_onPrimaryContainer    = Color(0xFF97F0FF)
internal val dark_secondary             = Color(0xFFB1CBD0)
private val dark_onSecondary           = Color(0xFF1B343A)
private val dark_secondaryContainer    = Color(0xFF324B51)
private val dark_onSecondaryContainer  = Color(0xFFCCE7EC)
internal val dark_tertiary              = Color(0xFFBAC6E8)
private val dark_onTertiary            = Color(0xFF232F4B)
private val dark_tertiaryContainer     = Color(0xFF3A4663)
private val dark_onTertiaryContainer   = Color(0xFFDAE2FF)
internal val dark_error                 = Color(0xFFFFB4AB)
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

// Teal-neutral surface container ladder for dark mode — same rationale as the
// light-mode block above, stepped across the M3 dark tone stops (4/6/10/12/17/22/24).
private val dark_surfaceDim              = Color(0xFF0E1010)
private val dark_surfaceBright           = Color(0xFF393F42)
private val dark_surfaceContainerLowest  = Color(0xFF090B0B)
private val dark_surfaceContainerLow     = Color(0xFF181A1B)
private val dark_surfaceContainer        = Color(0xFF1C2021)
private val dark_surfaceContainerHigh    = Color(0xFF282D2F)
private val dark_surfaceContainerHighest = Color(0xFF343A3C)

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
    surfaceDim = light_surfaceDim,
    surfaceBright = light_surfaceBright,
    surfaceContainerLowest = light_surfaceContainerLowest,
    surfaceContainerLow = light_surfaceContainerLow,
    surfaceContainer = light_surfaceContainer,
    surfaceContainerHigh = light_surfaceContainerHigh,
    surfaceContainerHighest = light_surfaceContainerHighest,
    // surfaceTint defaults to `primary` — already teal, no override needed.
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
    surfaceDim = dark_surfaceDim,
    surfaceBright = dark_surfaceBright,
    surfaceContainerLowest = dark_surfaceContainerLowest,
    surfaceContainerLow = dark_surfaceContainerLow,
    surfaceContainer = dark_surfaceContainer,
    surfaceContainerHigh = dark_surfaceContainerHigh,
    surfaceContainerHighest = dark_surfaceContainerHighest,
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
    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors

    CompositionLocalProvider(LocalStatusColors provides statusColors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
