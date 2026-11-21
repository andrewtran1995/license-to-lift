package io.github.andrewtran1995.liftlog.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.andrewtran1995.liftlog.R

/*
 * "Field terminal" theme: a night-time ops screen. Graphite blacks, one cool signal cyan
 * for anything live or actionable, a classified red kept for destructive actions, and a
 * monospaced face throughout so numbers line up like a readout. Always dark.
 */
object Spy {
    val Ink = Color(0xFF07090C)        // page
    val Panel = Color(0xFF0F1318)      // cards
    val PanelHi = Color(0xFF161C23)    // inputs, raised
    val Wire = Color(0xFF26303B)       // hairlines and borders
    val Text = Color(0xFFE4E8EC)
    val Muted = Color(0xFF7D8896)
    val Signal = Color(0xFF3FE0C5)     // primary accent
    val Classified = Color(0xFFE5484D) // destructive
}

val Mono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
    Font(R.font.jetbrains_mono_extrabold, FontWeight.ExtraBold),
)

private val Scheme = darkColorScheme(
    primary = Spy.Signal,
    onPrimary = Spy.Ink,
    secondary = Spy.Muted,
    background = Spy.Ink,
    onBackground = Spy.Text,
    surface = Spy.Ink,
    onSurface = Spy.Text,
    surfaceVariant = Spy.PanelHi,
    onSurfaceVariant = Spy.Muted,
    surfaceContainer = Spy.Panel,
    surfaceContainerLow = Spy.Panel,
    surfaceContainerHigh = Spy.PanelHi,
    surfaceContainerHighest = Spy.PanelHi,
    outline = Spy.Wire,
    outlineVariant = Spy.Wire,
    error = Spy.Classified,
    secondaryContainer = Color(0xFF0E2A27), // nav-bar selection pill
    onSecondaryContainer = Spy.Signal,
)

private fun mono(size: Int, weight: FontWeight = FontWeight.Normal, spacing: Double = 0.0, line: Int = (size * 1.4).toInt()) =
    TextStyle(fontFamily = Mono, fontSize = size.sp, fontWeight = weight, letterSpacing = spacing.sp, lineHeight = line.sp)

private val Type = Typography(
    headlineLarge = mono(28, FontWeight.ExtraBold, 1.0, 32),
    headlineSmall = mono(22, FontWeight.Bold, 0.5, 28),
    titleLarge = mono(18, FontWeight.Bold, 0.5),
    titleMedium = mono(15, FontWeight.Bold, 0.5),
    bodyLarge = mono(16),
    bodyMedium = mono(14),
    bodySmall = mono(12),
    labelLarge = mono(14, FontWeight.Bold, 1.5),
    labelMedium = mono(12, FontWeight.Medium, 1.5),
    labelSmall = mono(11, FontWeight.Medium, 1.5),
)

/** Machined corners: 2–4dp, never pill-shaped. */
private val Corners = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(3.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(6.dp),
)

@Composable
fun LiftLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Type, shapes = Corners, content = content)
}
