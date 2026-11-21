package io.github.andrewtran1995.liftlog.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("EEE, MMM d")
private val dayYearFormat = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")

fun formatDate(iso: String): String {
    val d = LocalDate.parse(iso)
    return d.format(if (d.year == LocalDate.now().year) dayFormat else dayYearFormat)
}

fun formatWeight(w: Double?, unit: String): String =
    if (w == null || w == 0.0) "BW" else "${if (w % 1.0 == 0.0) w.toLong().toString() else w.toString()} $unit"

/** "3×8 @ 60 lb, 1×6 @ 65 lb"; weight-only sets read "77 lb"; no sets reads "Done". Same rules as the web app. */
fun summarizeSets(sets: List<SetEntity>, unit: String): String {
    if (sets.isEmpty()) return "Done"
    data class Group(val weight: Double?, val reps: Int?, var count: Int)
    val groups = mutableListOf<Group>()
    for (s in sets.sortedBy { it.position }) {
        val last = groups.lastOrNull()
        if (last != null && last.weight == s.weight && last.reps == s.reps) last.count++ else groups += Group(s.weight, s.reps, 1)
    }
    return groups.joinToString(", ") { g ->
        val w = formatWeight(g.weight, unit)
        when {
            g.reps != null -> "${g.count}×${g.reps} @ $w"
            g.count > 1 -> "${g.count} sets @ $w"
            else -> w
        }
    }
}
