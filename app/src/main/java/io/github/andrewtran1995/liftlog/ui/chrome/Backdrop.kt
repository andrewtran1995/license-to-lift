package io.github.andrewtran1995.liftlog.ui.chrome

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.andrewtran1995.liftlog.ui.theme.Spy

/**
 * The lab bench: a blueprint grid lit by two soft lamps. Static and cheap to draw, it gives the
 * glass chrome something to refract, and shows faintly through the translucent panels.
 */
@Composable
fun BlueprintBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Spy.Ink)
        drawRect(
            Brush.radialGradient(
                listOf(Spy.LampCyan.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(size.width * 0.92f, size.height * 0.06f),
                radius = size.maxDimension * 0.55f,
            ),
        )
        drawRect(
            Brush.radialGradient(
                listOf(Spy.LampIce.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width * 0.05f, size.height * 0.88f),
                radius = size.maxDimension * 0.6f,
            ),
        )
        val step = 24.dp.toPx()
        val stroke = 1.dp.toPx()
        var i = 0
        var x = 0f
        while (x <= size.width) {
            drawLine(if (i % 4 == 0) Spy.GridMajor else Spy.GridMinor, Offset(x, 0f), Offset(x, size.height), stroke)
            x += step; i++
        }
        i = 0
        var y = 0f
        while (y <= size.height) {
            drawLine(if (i % 4 == 0) Spy.GridMajor else Spy.GridMinor, Offset(0f, y), Offset(size.width, y), stroke)
            y += step; i++
        }
    }
}
