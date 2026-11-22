package io.github.andrewtran1995.liftlog.ui.figures

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Exercises that have a drawn figure. Anything else gets the generic barbell. */
enum class FigureKind { Adductor, Abduction, KneeLift, LegCurl, ChestPress, ShoulderPress, Pulldown, Butterfly, SeatedDip, LateralRaise }

/** Matches a catalog name to a figure by keyword, so "Seated leg curl" still finds one. */
fun figureFor(name: String): FigureKind? {
    val n = name.lowercase()
    return when {
        "adduct" in n -> FigureKind.Adductor
        "abduct" in n -> FigureKind.Abduction
        "knee" in n -> FigureKind.KneeLift
        "curl" in n && "leg" in n -> FigureKind.LegCurl
        // Before "fly" and "shoulder": a delt machine is drawn as a lateral raise.
        "delt" in n || "lateral" in n -> FigureKind.LateralRaise
        "butterfly" in n || "pec deck" in n || "fly" in n -> FigureKind.Butterfly
        "pulldown" in n || "pull down" in n || "pull-down" in n -> FigureKind.Pulldown
        "dip" in n -> FigureKind.SeatedDip
        "chest" in n || "bench" in n -> FigureKind.ChestPress
        "shoulder" in n || "overhead" in n -> FigureKind.ShoulderPress
        else -> null
    }
}

private val Body = Color(0xFF3FE0C5)
private val Facet = Color(0xFF2BB59F)
private val Machine = Color(0xFF3A4654)
private val MachineDark = Color(0xFF232C37)
private val Pad = Color(0xFF3A6BD6)

private val Ease = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
private const val RepMillis = 2400

/**
 * A low-poly figure for [name]. When [animated], it performs two slow reps each time [playKey]
 * changes (and when tapped), ending in the pose it started from. It doesn't loop: a looping
 * figure would redraw every frame, and the glass nav bar would re-capture the screen with it.
 * Static figures (icons) are drawn once, at the top of the movement.
 */
@Composable
fun ExerciseFigure(name: String, modifier: Modifier = Modifier, animated: Boolean = false, playKey: Any? = null) {
    val kind = figureFor(name)
    var taps by remember { mutableIntStateOf(0) }
    val progress = remember(kind) { Animatable(if (animated) 0f else 0.5f) } // 0.5 = top of the movement

    if (animated) {
        LaunchedEffect(kind, playKey, taps) {
            // Raw progress 0 and 1 are the same pose (the start), so wrapping from 1 back to 0 is
            // invisible. If a replay interrupts a rep, finish that rep first instead of jumping.
            val v = progress.value
            if (v > 0f && v < 1f) progress.animateTo(1f, tween(((1f - v) * RepMillis).toInt(), easing = LinearEasing))
            repeat(2) {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(RepMillis, easing = LinearEasing))
            }
            // Ends at raw 1 = the start pose, exactly where it began.
        }
    }

    val m = if (animated) {
        modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { taps++ }
    } else modifier
    Canvas(m.semantics { contentDescription = "$name figure" }) {
        val p = repPhase(progress.value)
        val s = minOf(size.width / 160f, size.height / 140f)
        translate((size.width - 160f * s) / 2f, (size.height - 140f * s) / 2f) {
            scale(s, s, pivot = Offset.Zero) {
                when (kind) {
                    FigureKind.Adductor -> seatedLegs(lerp(26f, 3f, p), padInside = true)
                    FigureKind.Abduction -> seatedLegs(lerp(3f, 26f, p), padInside = false)
                    FigureKind.KneeLift -> kneeLift(p)
                    FigureKind.LegCurl -> legCurl(p)
                    FigureKind.ChestPress -> chestPress(p)
                    FigureKind.ShoulderPress -> shoulderPress(p)
                    FigureKind.Pulldown -> pulldown(p)
                    FigureKind.Butterfly -> butterfly(p)
                    FigureKind.SeatedDip -> seatedDip(p)
                    FigureKind.LateralRaise -> lateralRaise(p)
                    null -> barbell()
                }
            }
        }
    }
}

/**
 * Maps raw rep progress (0..1) to a pose phase: 0 = start pose, 1 = peak. There-and-back and
 * eased at both ends, so repPhase(0) == repPhase(1) == 0 with zero speed: reps join seamlessly.
 * Every pose is a pure function of this phase; that is what makes a figure loop perfectly.
 */
internal fun repPhase(raw: Float): Float {
    val r = raw.coerceIn(0f, 1f)
    return Ease.transform(if (r < 0.5f) r * 2f else (1f - r) * 2f)
}

internal fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

// ---- drawing primitives, in the 160×140 design space ----

private fun DrawScope.poly(color: Color, vararg pts: Float) {
    val path = Path().apply {
        moveTo(pts[0], pts[1])
        var i = 2
        while (i < pts.size) { lineTo(pts[i], pts[i + 1]); i += 2 }
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.limb(color: Color, width: Float, vararg pts: Float) {
    val path = Path().apply {
        moveTo(pts[0], pts[1])
        var i = 2
        while (i < pts.size) { lineTo(pts[i], pts[i + 1]); i += 2 }
    }
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.box(color: Color, x: Float, y: Float, w: Float, h: Float, r: Float = 2f) =
    drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(r, r))

/** Hexagonal head with a darker facet on its right half. */
private fun DrawScope.head(cx: Float, cy: Float, r: Float) {
    val h = r * 0.866f
    poly(Body, cx + r, cy, cx + r / 2, cy - h, cx - r / 2, cy - h, cx - r, cy, cx - r / 2, cy + h, cx + r / 2, cy + h)
    poly(Facet, cx, cy - h, cx + r / 2, cy - h, cx + r, cy, cx + r / 2, cy + h, cx, cy + h)
}

private fun DrawScope.frontTorso() {
    poly(Body, 64f, 44f, 96f, 44f, 90f, 84f, 70f, 84f)
    poly(Facet, 80f, 44f, 96f, 44f, 90f, 84f, 80f, 84f)
}

private fun DrawScope.seat() {
    box(MachineDark, 60f, 40f, 40f, 44f, 3f)
    box(Machine, 54f, 84f, 52f, 6f)
}

private inline fun DrawScope.mirrored(block: DrawScope.() -> Unit) =
    scale(-1f, 1f, pivot = Offset(80f, 0f)) { block() }

// ---- figures ----

private fun DrawScope.seatedLegs(angle: Float, padInside: Boolean) {
    seat()
    val leg: DrawScope.(Color) -> Unit = { c ->
        rotate(angle, pivot = Offset(73f, 86f)) {
            limb(c, 8f, 73f, 86f, 73f, 106f, 73f, 128f)
            box(Pad, if (padInside) 77f else 63f, 98f, 5f, 13f, 1.5f)
        }
    }
    leg(Body)
    mirrored { leg(Facet) }
    frontTorso()
    limb(Facet, 6f, 65f, 47f, 58f, 80f)
    limb(Facet, 6f, 95f, 47f, 102f, 80f)
    head(80f, 28f, 10f)
}

private fun DrawScope.kneeLift(p: Float) {
    box(Machine, 40f, 14f, 80f, 4f)
    limb(Facet, 6f, 81f, 48f, 80f, 18f)
    rotate(lerp(0f, -95f, p), pivot = Offset(80f, 86f)) {
        limb(Facet, 9f, 80f, 86f, 80f, 110f)
        rotate(lerp(0f, 95f, p), pivot = Offset(80f, 110f)) { limb(Body, 8f, 80f, 110f, 80f, 132f) }
    }
    poly(Body, 74f, 46f, 88f, 46f, 85f, 88f, 75f, 88f)
    poly(Facet, 81f, 46f, 88f, 46f, 85f, 88f, 80f, 88f)
    head(84f, 34f, 9f)
}

private fun DrawScope.legCurl(p: Float) {
    box(Machine, 14f, 96f, 130f, 7f)
    limb(MachineDark, 5f, 26f, 103f, 26f, 126f)
    limb(MachineDark, 5f, 132f, 103f, 132f, 126f)
    limb(Facet, 6f, 46f, 86f, 24f, 99f)
    poly(Body, 44f, 79f, 96f, 82f, 96f, 94f, 44f, 94f)
    poly(Facet, 44f, 86f, 96f, 88f, 96f, 94f, 44f, 94f)
    limb(Body, 9f, 96f, 89f, 122f, 91f)
    rotate(lerp(0f, -105f, p), pivot = Offset(122f, 91f)) {
        limb(Facet, 8f, 122f, 91f, 144f, 91f)
        drawCircle(Pad, 5f, Offset(145f, 84f))
    }
    head(32f, 84f, 9f)
}

private fun DrawScope.chestPress(p: Float) {
    box(Machine, 40f, 40f, 6f, 62f)
    box(Machine, 44f, 98f, 44f, 6f)
    box(MachineDark, 128f, 30f, 6f, 100f)
    limb(Facet, 8f, 58f, 98f, 86f, 98f, 88f, 126f)
    poly(Body, 50f, 46f, 66f, 46f, 64f, 98f, 52f, 98f)
    poly(Facet, 58f, 46f, 66f, 46f, 64f, 98f, 58f, 98f)
    rotate(lerp(70f, 0f, p), pivot = Offset(60f, 52f)) {
        limb(Body, 7f, 60f, 52f, 82f, 52f)
        rotate(lerp(-140f, 0f, p), pivot = Offset(82f, 52f)) {
            limb(Body, 6f, 82f, 52f, 104f, 52f)
            box(Pad, 102f, 44f, 5f, 16f, 1.5f)
        }
    }
    head(60f, 32f, 9f)
}

private fun DrawScope.shoulderPress(p: Float) {
    seat()
    limb(Facet, 8f, 73f, 86f, 70f, 110f, 70f, 130f)
    limb(Facet, 8f, 87f, 86f, 90f, 110f, 90f, 130f)
    frontTorso()
    val arm: DrawScope.(Color) -> Unit = { c ->
        rotate(lerp(0f, -75f, p), pivot = Offset(96f, 48f)) {
            limb(c, 7f, 96f, 48f, 118f, 48f)
            rotate(lerp(0f, 75f, p), pivot = Offset(118f, 48f)) {
                limb(c, 6f, 118f, 48f, 118f, 28f)
                box(Pad, 110f, 22f, 16f, 4f, 1.5f)
                box(Machine, 109f, 19f, 4f, 10f, 1f)
                box(Machine, 123f, 19f, 4f, 10f, 1f)
            }
        }
    }
    arm(Body)
    mirrored { arm(Facet) }
    head(80f, 28f, 10f)
}

/** Front view: bar pulled from overhead down to the chin; forearms stay vertical. */
private fun DrawScope.pulldown(p: Float) {
    limb(Machine, 2f, 80f, 2f, 80f, 8f)
    translate(0f, lerp(0f, 21f, p)) {
        limb(Machine, 1.5f, 80f, 8f, 80f, 0f)
        box(Pad, 36f, 4f, 88f, 4f)
    }
    seat()
    box(Machine, 62f, 92f, 36f, 6f) // knee pad
    limb(Facet, 8f, 73f, 86f, 70f, 110f, 70f, 130f)
    limb(Facet, 8f, 87f, 86f, 90f, 110f, 90f, 130f)
    frontTorso()
    val arm: DrawScope.(Color) -> Unit = { c ->
        rotate(lerp(-75f, 0f, p), pivot = Offset(96f, 48f)) {
            limb(c, 7f, 96f, 48f, 118f, 48f)
            rotate(lerp(75f, 0f, p), pivot = Offset(118f, 48f)) { limb(c, 6f, 118f, 48f, 118f, 28f) }
        }
    }
    arm(Body)
    mirrored { arm(Facet) }
    head(80f, 28f, 10f)
}

/** Front view pec deck: elbows swing in from the sides until the pads meet in front. */
private fun DrawScope.butterfly(p: Float) {
    seat()
    limb(Facet, 8f, 73f, 86f, 70f, 110f, 70f, 130f)
    limb(Facet, 8f, 87f, 86f, 90f, 110f, 90f, 130f)
    frontTorso()
    val arm: DrawScope.(Color) -> Unit = { c ->
        val elbow = lerp(122f, 88f, p)
        limb(c, 7f, 96f, 50f, elbow, 50f)
        limb(c, 6f, elbow, 50f, elbow, 28f)
        box(Pad, elbow + 2f, 26f, 5f, 26f, 1.5f)
    }
    mirrored { arm(Facet) }
    arm(Body)
    head(80f, 28f, 10f)
}

/** Side view, facing right: hands press the handles from armpit height down to the hips. */
private fun DrawScope.seatedDip(p: Float) {
    box(Machine, 40f, 40f, 6f, 62f)
    box(Machine, 44f, 98f, 44f, 6f)
    limb(Facet, 8f, 58f, 98f, 86f, 98f, 88f, 126f)
    poly(Body, 50f, 46f, 66f, 46f, 64f, 98f, 52f, 98f)
    poly(Facet, 58f, 46f, 66f, 46f, 64f, 98f, 58f, 98f)
    rotate(lerp(50f, 0f, p), pivot = Offset(58f, 52f)) {
        limb(Body, 7f, 58f, 52f, 58f, 74f)
        rotate(lerp(-100f, 0f, p), pivot = Offset(58f, 74f)) {
            limb(Body, 6f, 58f, 74f, 58f, 94f)
            box(Pad, 52f, 92f, 16f, 5f, 1.5f)
        }
    }
    head(60f, 32f, 9f)
}

/** Front view lateral raise machine: arms lift from the sides to shoulder height, pads at the elbows. */
private fun DrawScope.lateralRaise(p: Float) {
    seat()
    limb(Facet, 8f, 73f, 86f, 70f, 110f, 70f, 130f)
    limb(Facet, 8f, 87f, 86f, 90f, 110f, 90f, 130f)
    frontTorso()
    val arm: DrawScope.(Color) -> Unit = { c ->
        rotate(lerp(-10f, -85f, p), pivot = Offset(96f, 48f)) {
            limb(c, 7f, 96f, 48f, 96f, 70f)
            box(Pad, 91f, 62f, 10f, 5f, 1.5f)
            rotate(lerp(0f, 10f, p), pivot = Offset(96f, 70f)) { limb(c, 6f, 96f, 70f, 96f, 88f) }
        }
    }
    arm(Body)
    mirrored { arm(Facet) }
    head(80f, 28f, 10f)
}

/** Fallback for exercises without a drawing yet. */
private fun DrawScope.barbell() {
    box(Machine, 20f, 66f, 120f, 8f, 4f)
    box(Body, 30f, 44f, 12f, 52f, 3f)
    box(Facet, 44f, 52f, 8f, 36f, 2f)
    box(Body, 118f, 44f, 12f, 52f, 3f)
    box(Facet, 108f, 52f, 8f, 36f, 2f)
}
