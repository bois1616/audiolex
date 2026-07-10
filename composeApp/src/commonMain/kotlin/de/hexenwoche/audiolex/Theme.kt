package de.hexenwoche.audiolex

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    background = Color(0xFFFAF8F5),
    surface = Color(0xFFF2EEE9),
    onBackground = Color(0xFF2B2724),
    onSurface = Color(0xFF2B2724),
    primary = Color(0xFF3A7D74),
    onPrimary = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    background = Color(0xFF1C1A18),
    surface = Color(0xFF26231F),
    onBackground = Color(0xFFEDE8E2),
    onSurface = Color(0xFFEDE8E2),
    primary = Color(0xFF5FB3A6),
    onPrimary = Color(0xFF10201C),
)

/**
 * DESIGN.md "Zielwort dominant, Sekundäres tritt deutlich zurück": progress/
 * badges get a small, visually receding style instead of Material's default
 * bodyMedium, so they don't compete with the (separately auto-sized) target
 * word. Only the styles the app actually references are overridden.
 */
private val AudioLexTypography = Typography(
    labelMedium = TextStyle(fontSize = 13.sp, letterSpacing = 0.3.sp),
)

/**
 * AudioLex-Theme (DESIGN.md "Visuelle Prinzipien"): warm-neutral base, a
 * muted teal accent that carries meaning (active elements), following the
 * system's light/dark setting -- not a user-facing toggle. The target word
 * itself stays neutral (onSurface/onBackground), the accent is reserved for
 * buttons and other active elements, never decoration.
 */
@Composable
fun AudioLexTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, typography = AudioLexTypography, content = content)
}
