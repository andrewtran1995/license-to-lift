package io.github.andrewtran1995.liftlog.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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

/** Readout-style date: "2026-11-16 · mon". */
fun formatStamp(iso: String): String {
    val d = LocalDate.parse(iso)
    return "$iso · ${d.dayOfWeek.name.take(3).lowercase()}"
}

private const val SECOND = 1_000L
private const val MINUTE = 60 * SECOND
private const val HOUR = 60 * MINUTE

/**
 * When a set was logged, for the Log screen: "12s ago" for the first minute, "4m ago" for the
 * first hour, then the clock time ("7:42 PM"). A time ahead of [now] (clock changed) reads "0s ago".
 */
fun formatLoggedAt(
    loggedAt: Long,
    now: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    clock: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT),
): String {
    val age = (now - loggedAt).coerceAtLeast(0)
    return when {
        age < MINUTE -> "${age / SECOND}s ago"
        age < HOUR -> "${age / MINUTE}m ago"
        else -> Instant.ofEpochMilli(loggedAt).atZone(zone).format(clock)
    }
}

/**
 * The next moment any [formatLoggedAt] label for [times] changes, so the screen redraws only
 * then: each second while a set is under a minute old, each minute while under an hour, and
 * never once every set shows a clock time. Null means nothing will change.
 */
fun nextLoggedAtChange(times: List<Long>, now: Long): Long? = times.mapNotNull { t ->
    val age = now - t
    when {
        age < 0 -> t + SECOND // reads "0s ago" until it's a second old
        age < MINUTE -> now + (SECOND - age % SECOND)
        age < HOUR -> now + (MINUTE - age % MINUTE)
        else -> null
    }
}.minOrNull()
