package io.github.andrewtran1995.liftlog.ui.chrome

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.andrewtran1995.liftlog.ui.theme.Spy

/**
 * The lab bench: a blueprint grid lit by two soft lamps. It gives the glass chrome something to
 * refract. The grid paths and gradients are built once per size (drawWithCache), so each frame
 * that Haze re-captures the source only replays two paths instead of laying out ~100 lines.
 */
@Composable
fun BlueprintBackdrop(modifier: Modifier = Modifier) {
    Spacer(
        modifier.drawWithCache {
            val step = 24.dp.toPx()
            val minor = Path()
            val major = Path()
            var i = 0
            var x = 0f
            while (x <= size.width) {
                (if (i % 4 == 0) major else minor).apply { moveTo(x, 0f); lineTo(x, size.height) }
                x += step; i++
            }
            i = 0
            var y = 0f
            while (y <= size.height) {
                (if (i % 4 == 0) major else minor).apply { moveTo(0f, y); lineTo(size.width, y) }
                y += step; i++
            }
            val stroke = Stroke(width = 1.dp.toPx())
            val cyanLamp = Brush.radialGradient(
                listOf(Spy.LampCyan.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(size.width * 0.92f, size.height * 0.06f),
                radius = size.maxDimension * 0.55f,
            )
            val iceLamp = Brush.radialGradient(
                listOf(Spy.LampIce.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width * 0.05f, size.height * 0.88f),
                radius = size.maxDimension * 0.6f,
            )
            onDrawBehind {
                drawRect(Spy.Ink)
                drawRect(cyanLamp)
                drawRect(iceLamp)
                drawPath(minor, Spy.GridMinor, style = stroke)
                drawPath(major, Spy.GridMajor, style = stroke)
            }
        },
    )
}
