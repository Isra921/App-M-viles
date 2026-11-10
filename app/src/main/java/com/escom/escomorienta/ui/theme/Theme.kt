package com.escom.escomorienta.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.escom.escomorienta.data.settings.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = EscomBlue,
    onPrimary = White,
    primaryContainer = EscomBlueLight,
    onPrimaryContainer = EscomBlueDeep,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer
)

@Immutable
data class AppExtendedColors(
    val mapCanvas: Color,
    val mapHighlight: Color,
    val mapHighlightStroke: Color,
    val loadingContainer: Color,
    val linkColor: Color
)

private val LightExtendedColors = AppExtendedColors(
    mapCanvas = LightMapCanvas,
    mapHighlight = LightMapHighlight,
    mapHighlightStroke = LightMapHighlightStroke,
    loadingContainer = EscomBlueLight,
    linkColor = EscomBlueDark
)

private val DarkExtendedColors = AppExtendedColors(
    mapCanvas = DarkMapCanvas,
    mapHighlight = DarkMapHighlight,
    mapHighlightStroke = DarkMapHighlightStroke,
    loadingContainer = Color(0xFF1E293B),
    linkColor = Color(0xFF60A5FA)
)

private val LocalAppExtendedColors = staticCompositionLocalOf {
    LightExtendedColors
}

val MaterialTheme.appExtendedColors: AppExtendedColors
    @Composable get() = LocalAppExtendedColors.current

@Composable
fun EscomAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemIsDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemIsDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val targetScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
    val animatedScheme = animateColorScheme(targetScheme)
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = animatedScheme.background.toArgb()
            window.navigationBarColor = animatedScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalAppExtendedColors provides if (useDarkTheme) DarkExtendedColors else LightExtendedColors
    ) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val animation = tween<Color>(durationMillis = 200)

    return target.copy(
        primary = animateColorAsState(target.primary, animation, label = "primary").value,
        onPrimary = animateColorAsState(target.onPrimary, animation, label = "onPrimary").value,
        primaryContainer = animateColorAsState(
            target.primaryContainer,
            animation,
            label = "primaryContainer"
        ).value,
        onPrimaryContainer = animateColorAsState(
            target.onPrimaryContainer,
            animation,
            label = "onPrimaryContainer"
        ).value,
        secondary = animateColorAsState(target.secondary, animation, label = "secondary").value,
        onSecondary = animateColorAsState(
            target.onSecondary,
            animation,
            label = "onSecondary"
        ).value,
        secondaryContainer = animateColorAsState(
            target.secondaryContainer,
            animation,
            label = "secondaryContainer"
        ).value,
        onSecondaryContainer = animateColorAsState(
            target.onSecondaryContainer,
            animation,
            label = "onSecondaryContainer"
        ).value,
        tertiary = animateColorAsState(target.tertiary, animation, label = "tertiary").value,
        onTertiary = animateColorAsState(target.onTertiary, animation, label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(
            target.tertiaryContainer,
            animation,
            label = "tertiaryContainer"
        ).value,
        onTertiaryContainer = animateColorAsState(
            target.onTertiaryContainer,
            animation,
            label = "onTertiaryContainer"
        ).value,
        background = animateColorAsState(target.background, animation, label = "background").value,
        onBackground = animateColorAsState(
            target.onBackground,
            animation,
            label = "onBackground"
        ).value,
        surface = animateColorAsState(target.surface, animation, label = "surface").value,
        onSurface = animateColorAsState(target.onSurface, animation, label = "onSurface").value,
        surfaceVariant = animateColorAsState(
            target.surfaceVariant,
            animation,
            label = "surfaceVariant"
        ).value,
        onSurfaceVariant = animateColorAsState(
            target.onSurfaceVariant,
            animation,
            label = "onSurfaceVariant"
        ).value,
        outline = animateColorAsState(target.outline, animation, label = "outline").value,
        outlineVariant = animateColorAsState(
            target.outlineVariant,
            animation,
            label = "outlineVariant"
        ).value,
        error = animateColorAsState(target.error, animation, label = "error").value,
        onError = animateColorAsState(target.onError, animation, label = "onError").value,
        errorContainer = animateColorAsState(
            target.errorContainer,
            animation,
            label = "errorContainer"
        ).value,
        onErrorContainer = animateColorAsState(
            target.onErrorContainer,
            animation,
            label = "onErrorContainer"
        ).value
    )
}
