package io.github.andrewtran1995.liftlog.ui.intel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.andrewtran1995.liftlog.data.SetEntity
import io.github.andrewtran1995.liftlog.data.WorkoutDao
import io.github.andrewtran1995.liftlog.data.exerciseKey
import io.github.andrewtran1995.liftlog.data.formatStamp
import io.github.andrewtran1995.liftlog.data.summarizeSets
import androidx.compose.foundation.layout.size
import io.github.andrewtran1995.liftlog.ui.figures.ExerciseFigure
import io.github.andrewtran1995.liftlog.ui.theme.Caption
import io.github.andrewtran1995.liftlog.ui.theme.Mono
import io.github.andrewtran1995.liftlog.ui.theme.numbers
import io.github.andrewtran1995.liftlog.ui.theme.Panel
import io.github.andrewtran1995.liftlog.ui.theme.ReadoutRow
import io.github.andrewtran1995.liftlog.ui.theme.ScreenHeader
import io.github.andrewtran1995.liftlog.ui.theme.Spy
import java.time.LocalDate
import kotlin.math.roundToInt

private const val DisplayUnit = "lb" // TODO: follow the units setting once it exists
private const val KgToLb = 2.20462

private enum class Metric(val label: String) { Heaviest("Heaviest"), Volume("Volume"), Reps("Reps") }

/** One session's numbers for the chosen exercise, weights converted to [DisplayUnit]. */
private data class Point(val date: String, val sets: List<SetEntity>, val unit: String, val heaviest: Double, val volume: Double, val reps: Int) {
    fun value(m: Metric): Double = when (m) { Metric.Heaviest -> heaviest; Metric.Volume -> volume; Metric.Reps -> reps.toDouble() }
}

private fun fmt(v: Double, m: Metric): String = when (m) {
    Metric.Reps -> v.roundToInt().toString()
    Metric.Volume -> "%,d $DisplayUnit".format(v.roundToInt())
    Metric.Heaviest -> if (v <= 0.0) "BW" else (if (v % 1.0 == 0.0) "${v.toLong()}" else "%.1f".format(v)) + " $DisplayUnit"
}

@Composable
fun IntelScreen(dao: WorkoutDao, contentPadding: PaddingValues) {
    val typesFlow = remember(dao) { dao.observeExerciseTypes() }
    val workoutsFlow = remember(dao) { dao.observeWorkouts() }
    val types by typesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val workouts by workoutsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var metric by rememberSaveable { mutableStateOf(Metric.Heaviest) }

    if (types.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Caption("// No intel yet")
                Text("Log an exercise to start tracking it.", color = Spy.Muted, textAlign = TextAlign.Center)
            }
        }
        return
    }
    val selected = types.firstOrNull { it.id == selectedId } ?: types.first()

    // Derived once per data change, not on every recomposition (chip taps, scrolling).
    val points = remember(workouts, selected.id) { workouts.asReversed().mapNotNull { w -> // oldest first
        val ex = w.exercises.firstOrNull { it.exercise.typeId == selected.id || exerciseKey(it.exercise.name) == selected.nameKey }
            ?: return@mapNotNull null
        val f = if (w.workout.unit == "kg" && DisplayUnit == "lb") KgToLb else 1.0
        Point(
            date = w.workout.date,
            sets = ex.sets.sortedBy { it.position },
            unit = w.workout.unit,
            heaviest = ex.sets.maxOfOrNull { (it.weight ?: 0.0) * f } ?: 0.0,
            volume = ex.sets.sumOf { (it.weight ?: 0.0) * f * (it.reps ?: 0) },
            reps = ex.sets.sumOf { it.reps ?: 0 },
        )
    } }

    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("// ${types.size} IN CATALOG", "Intel") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(types, key = { it.id }) { t ->
                    FilterChip(
                        selected = t.id == selected.id,
                        onClick = { selectedId = t.id },
                        label = { Text(t.name.lowercase(), maxLines = 1) },
                        leadingIcon = { ExerciseFigure(t.name, Modifier.size(width = 28.dp, height = 24.dp)) },
                        modifier = Modifier.height(io.github.andrewtran1995.liftlog.ui.log.ChipHeight),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Spy.Signal.copy(alpha = 0.18f),
                            selectedLabelColor = Spy.Signal,
                            labelColor = Spy.Text,
                        ),
                    )
                }
            }
        }
        item(key = "figure") {
            Panel {
                ExerciseFigure(selected.name, Modifier.fillMaxWidth().height(150.dp), animated = true, playKey = selected.id)
            }
        }
        if (points.isEmpty()) {
            item { Text("No sessions logged for ${selected.name.lowercase()} yet.", color = Spy.Muted) }
            return@LazyColumn
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric.entries.forEach { m ->
                    FilterChip(
                        selected = m == metric,
                        onClick = { metric = m },
                        label = { Text(m.label.uppercase(), style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Spy.Signal.copy(alpha = 0.18f),
                            selectedLabelColor = Spy.Signal,
                            labelColor = Spy.Muted,
                        ),
                    )
                }
            }
        }
        item {
            val values = points.map { it.value(metric) }
            val best = values.max()
            val first = values.first()
            val latest = values.last()
            val change = if (points.size > 1 && first > 0) ((latest - first) / first * 100).roundToInt() else null
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("Best", fmt(best, metric), formatStamp(points[values.lastIndexOf(best)].date).take(10), Modifier.weight(1f))
                Stat("Latest", fmt(latest, metric), points.last().date, Modifier.weight(1f))
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("Since first", change?.let { (if (it > 0) "+" else "") + "$it%" } ?: "—", points.first().date, Modifier.weight(1f),
                    color = when { change == null || change == 0 -> Spy.Text; change > 0 -> Spy.Signal; else -> Spy.Classified })
                Stat("Sessions", "${points.size}", "logged", Modifier.weight(1f))
            }
        }
        item {
            Panel {
                Caption("${metric.label} per session")
                TrendChart(points.map { it.date to it.value(metric) }, Modifier.fillMaxWidth().height(200.dp))
            }
        }
        item { Caption("Session log") }
        items(points.asReversed()) { p ->
            Panel { ReadoutRow(formatStamp(p.date), summarizeSets(p.sets, p.unit)) }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, sub: String, modifier: Modifier = Modifier, color: Color = Spy.Text) {
    Panel(modifier) {
        Caption(label)
        Text(value, style = MaterialTheme.typography.headlineSmall.numbers(), color = color, maxLines = 1)
        Text(sub, style = MaterialTheme.typography.bodySmall.numbers(), color = Spy.Muted, maxLines = 1)
    }
}

/** Line over time, x spaced by real dates, on a faint instrument grid. */
@Composable
private fun TrendChart(series: List<Pair<String, Double>>, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = Mono, color = Spy.Muted, fontSize = 11.sp)
    Canvas(modifier) {
        if (series.isEmpty()) return@Canvas
        val days = series.map { LocalDate.parse(it.first).toEpochDay().toFloat() }
        val vals = series.map { it.second.toFloat() }
        var lo = vals.min(); var hi = vals.max()
        if (lo == hi) { lo -= 1f; hi += 1f }
        val pad = (hi - lo) * 0.12f; lo = (lo - pad).coerceAtLeast(0f); hi += pad
        val left = 44.dp.toPx(); val right = size.width - 8.dp.toPx()
        val top = 8.dp.toPx(); val bottom = size.height - 22.dp.toPx()
        val d0 = days.first(); val d1 = days.last()
        fun x(d: Float) = if (d1 == d0) (left + right) / 2 else left + (d - d0) / (d1 - d0) * (right - left)
        fun y(v: Float) = top + (hi - v) / (hi - lo) * (bottom - top)

        for (i in 0..3) {
            val v = lo + (hi - lo) * i / 3
            val yy = y(v)
            drawLine(Spy.Wire, Offset(left, yy), Offset(right, yy), 1.dp.toPx())
            val t = measurer.measure(v.roundToInt().toString(), labelStyle)
            drawText(t, topLeft = Offset(left - t.size.width - 6.dp.toPx(), yy - t.size.height / 2))
        }
        val first = measurer.measure(series.first().first.substring(5), labelStyle)
        drawText(first, topLeft = Offset(left, bottom + 6.dp.toPx()))
        if (d1 != d0) {
            val last = measurer.measure(series.last().first.substring(5), labelStyle)
            drawText(last, topLeft = Offset(right - last.size.width, bottom + 6.dp.toPx()))
        }

        val pts = days.indices.map { Offset(x(days[it]), y(vals[it])) }
        if (pts.size > 1) {
            val line = Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
            val area = Path().apply { addPath(line); lineTo(pts.last().x, bottom); lineTo(pts.first().x, bottom); close() }
            drawPath(area, Brush.verticalGradient(listOf(Spy.Signal.copy(alpha = 0.22f), Color.Transparent), startY = top, endY = bottom))
            drawPath(line, Spy.Signal, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        pts.forEach {
            drawCircle(Spy.Ink, 5.dp.toPx(), it)
            drawCircle(Spy.Signal, 5.dp.toPx(), it, style = Stroke(2.dp.toPx()))
        }
    }
}
