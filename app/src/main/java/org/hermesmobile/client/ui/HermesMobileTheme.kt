package org.hermesmobile.client.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

data class HermesThemePreset(
    val name: String,
    val label: String,
    val description: String,
    val scheme: ColorScheme
)

private val NousScheme = darkColorScheme(
    primary = Color(0xFFFFE6CB),
    onPrimary = Color(0xFF0D2F86),
    secondary = Color(0xFFB5C7F3),
    onSecondary = Color(0xFF09286F),
    tertiary = Color(0xFF75C894),
    onTertiary = Color(0xFF09286F),
    background = Color(0xFF0D2F86),
    onBackground = Color(0xFFFFE6CB),
    surface = Color(0xFF12378F),
    onSurface = Color(0xFFFFE6CB),
    surfaceVariant = Color(0xFF1B45A4),
    onSurfaceVariant = Color(0xFFB5C7F3),
    error = Color(0xFFC0473A),
    onError = Color(0xFFFEF2F2),
    outline = Color(0xFF3158AD),
    outlineVariant = Color(0xFF234A9C)
)

private val MidnightScheme = darkColorScheme(
    primary = Color(0xFFDDD6FF),
    onPrimary = Color(0xFF08081C),
    secondary = Color(0xFFC4BFF0),
    onSecondary = Color(0xFF08081C),
    tertiary = Color(0xFF8B80E8),
    onTertiary = Color(0xFF08081C),
    background = Color(0xFF08081C),
    onBackground = Color(0xFFDDD6FF),
    surface = Color(0xFF0D0D28),
    onSurface = Color(0xFFDDD6FF),
    surfaceVariant = Color(0xFF1A1A4A),
    onSurfaceVariant = Color(0xFF7C7AB0),
    error = Color(0xFFB03060),
    onError = Color(0xFFFEF2F2),
    outline = Color(0xFF1E1E52),
    outlineVariant = Color(0xFF12123A)
)

private val EmberScheme = darkColorScheme(
    primary = Color(0xFFFFD8B0),
    onPrimary = Color(0xFF160800),
    secondary = Color(0xFFF0C090),
    onSecondary = Color(0xFF160800),
    tertiary = Color(0xFFD97316),
    onTertiary = Color(0xFF160800),
    background = Color(0xFF160800),
    onBackground = Color(0xFFFFD8B0),
    surface = Color(0xFF1E0E04),
    onSurface = Color(0xFFFFD8B0),
    surfaceVariant = Color(0xFF341800),
    onSurfaceVariant = Color(0xFFAA7A56),
    error = Color(0xFFC43010),
    onError = Color(0xFFFEF2F2),
    outline = Color(0xFF3A1C08),
    outlineVariant = Color(0xFF2A1004)
)

private val MonoScheme = darkColorScheme(
    primary = Color(0xFFEAEAEA),
    onPrimary = Color(0xFF0E0E0E),
    secondary = Color(0xFFC8C8C8),
    onSecondary = Color(0xFF0E0E0E),
    tertiary = Color(0xFF9A9A9A),
    onTertiary = Color(0xFF0E0E0E),
    background = Color(0xFF0E0E0E),
    onBackground = Color(0xFFEAEAEA),
    surface = Color(0xFF141414),
    onSurface = Color(0xFFEAEAEA),
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = Color(0xFF808080),
    error = Color(0xFFA84040),
    onError = Color(0xFFFEF2F2),
    outline = Color(0xFF2A2A2A),
    outlineVariant = Color(0xFF202020)
)

private val CyberpunkScheme = darkColorScheme(
    primary = Color(0xFF00FF41),
    onPrimary = Color(0xFF000A00),
    secondary = Color(0xFF00CC34),
    onSecondary = Color(0xFF000A00),
    tertiary = Color(0xFF00E038),
    onTertiary = Color(0xFF000A00),
    background = Color(0xFF000A00),
    onBackground = Color(0xFF00FF41),
    surface = Color(0xFF001200),
    onSurface = Color(0xFF00FF41),
    surfaceVariant = Color(0xFF002800),
    onSurfaceVariant = Color(0xFF1A8A30),
    error = Color(0xFFFF003C),
    onError = Color(0xFF000A00),
    outline = Color(0xFF003000),
    outlineVariant = Color(0xFF001800)
)

private val SlateScheme = darkColorScheme(
    primary = Color(0xFFC9D1D9),
    onPrimary = Color(0xFF0D1117),
    secondary = Color(0xFFADB5BF),
    onSecondary = Color(0xFF0D1117),
    tertiary = Color(0xFF58A6FF),
    onTertiary = Color(0xFF0D1117),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFC9D1D9),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFC9D1D9),
    surfaceVariant = Color(0xFF2A3038),
    onSurfaceVariant = Color(0xFF8B949E),
    error = Color(0xFFCF4848),
    onError = Color(0xFFFEF2F2),
    outline = Color(0xFF30363D),
    outlineVariant = Color(0xFF202830)
)

private val NousLightScheme = lightColorScheme(
    primary = Color(0xFF0053FD),
    onPrimary = Color(0xFFFCFCFC),
    secondary = Color(0xFF242432),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF1540B1),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAFF),
    onBackground = Color(0xFF17171A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17171A),
    surfaceVariant = Color(0xFFF3F7FF),
    onSurfaceVariant = Color(0xFF666678),
    error = Color(0xFFC72E4D),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFFB8C9FF),
    outlineVariant = Color(0xFFDCE6FF)
)

val HermesThemePresets = listOf(
    HermesThemePreset("nous", "Nous", "Canonical Hermes Desktop identity", NousScheme),
    HermesThemePreset("nous-light", "Nous Light", "Bright desktop blue and glass", NousLightScheme),
    HermesThemePreset("midnight", "Midnight", "Deep blue-violet with cool accents", MidnightScheme),
    HermesThemePreset("ember", "Ember", "Warm crimson and bronze forge skin", EmberScheme),
    HermesThemePreset("mono", "Mono", "Clean grayscale focus skin", MonoScheme),
    HermesThemePreset("cyberpunk", "Cyberpunk", "Neon terminal skin", CyberpunkScheme),
    HermesThemePreset("slate", "Slate", "Cool developer slate skin", SlateScheme)
)

fun hermesThemePreset(name: String?): HermesThemePreset {
    return HermesThemePresets.firstOrNull { it.name == name } ?: HermesThemePresets.first()
}

private val BaseTypography = Typography()
private val HermesTypography = BaseTypography.copy(
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
)

@Composable
fun HermesMobileTheme(themeName: String = "nous", content: @Composable () -> Unit) {
    val preset = hermesThemePreset(themeName)
    MaterialTheme(
        colorScheme = preset.scheme,
        typography = HermesTypography,
        content = content
    )
}
