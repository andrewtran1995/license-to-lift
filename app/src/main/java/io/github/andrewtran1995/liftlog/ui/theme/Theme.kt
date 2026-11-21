package io.github.andrewtran1995.liftlog.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
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
 * "Q-Lab" theme: a lab bench at night. Blueprint-grid backdrop lit by two soft lamps, glass
 * chrome that refracts it, translucent instrument panels, one signal cyan for anything live,
 * a classified red for destructive actions, and a monospaced face so numbers read like
 * instruments. Always dark. Motion follows Material 3 Expressive springs.
 */
object Spy {
    val Ink = Color(0xFF05080D)        // page, under the blueprint grid
    val Panel = Color(0xB80D131B)      // translucent cards: the grid shows faintly through
    val PanelHi = Color(0xCC141C26)    // inputs, raised
    val Wire = Color(0x1FFFFFFF)       // hairlines and borders
    val GridMinor = Color(0x0CFFFFFF)
    val GridMajor = Color(0x16FFFFFF)
    val LampCyan = Color(0xFF1FB8A3)
    val LampIce = Color(0xFF3A6BD6)
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
    secondaryContainer = Color(0x293FE0C5),
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

/** Softer, lab-equipment corners to suit the glass. */
private val Corners = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LiftLogTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = Scheme,
        motionScheme = MotionScheme.expressive(),
        shapes = Corners,
        typography = Type,
        content = content,
    )
}
