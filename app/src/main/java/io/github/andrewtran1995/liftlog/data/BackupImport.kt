package io.github.andrewtran1995.liftlog.data

import org.json.JSONArray
import org.json.JSONObject

/** One workout parsed from a web-app backup, before it is written to the database. */
data class ImportedWorkout(
    val id: String,
    val date: String,
    val name: String,
    val notes: String,
    val unit: String,
    val updatedAt: Long,
    val exercises: List<Pair<String, List<Pair<Double?, Int?>>>>,
)

/**
 * Reads the text from the web app's Settings → Copy backup:
 * `{"app":"liftlog","v":1,"sessions":[{"id","date","name","notes","unit","t","exercises":[{"name","sets":[{"w","r"}]}]}]}`.
 * A bare array of sessions is accepted too. Malformed sessions are skipped, not fatal.
 */
object BackupImport {
    private val isoDate = Regex("""\d{4}-\d{2}-\d{2}""")

    fun parse(text: String): List<ImportedWorkout> {
        val trimmed = text.trim()
        val sessions: JSONArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> JSONObject(trimmed).optJSONArray("sessions") ?: JSONArray()
        }
        return (0 until sessions.length()).mapNotNull { i ->
            val s = sessions.optJSONObject(i) ?: return@mapNotNull null
            val id = s.optString("id").ifBlank { return@mapNotNull null }
            val date = s.optString("date").takeIf { isoDate.matches(it) } ?: return@mapNotNull null
            val exArray = s.optJSONArray("exercises") ?: return@mapNotNull null
            val exercises = (0 until exArray.length()).mapNotNull { j ->
                val e = exArray.optJSONObject(j) ?: return@mapNotNull null
                val name = e.optString("name").trim().ifBlank { return@mapNotNull null }
                val setArray = e.optJSONArray("sets") ?: JSONArray()
                val sets = (0 until setArray.length()).mapNotNull { k ->
                    val set = setArray.optJSONObject(k) ?: return@mapNotNull null
                    val w = set.optDouble("w").takeIf { !it.isNaN() && it > 0.0 }
                    val r = set.optDouble("r").takeIf { !it.isNaN() && it > 0.0 }?.toInt()
                    if (w == null && r == null) null else w to r
                }
                name to sets
            }
            ImportedWorkout(
                id = id,
                date = date,
                name = s.optString("name").trim(),
                notes = s.optString("notes").trim(),
                unit = s.optString("unit").takeIf { it == "kg" || it == "lb" } ?: "kg",
                updatedAt = s.optLong("t", System.currentTimeMillis()),
                exercises = exercises,
            )
        }
    }

    /**
     * Writes [workouts], linking every exercise to the catalog. Ids come from the backup, so
     * importing the same backup twice updates rather than duplicates.
     */
    suspend fun save(dao: WorkoutDao, workouts: List<ImportedWorkout>): Int {
        for (w in workouts) {
            val exercises = w.exercises.mapIndexed { i, (name, sets) ->
                val type = dao.ensureType(name)
                val exId = "${w.id}-e$i"
                ExerciseEntity(exId, w.id, i, type.name, type.id) to sets.mapIndexed { j, (weight, reps) ->
                    SetEntity("$exId-s$j", exId, j, weight, reps)
                }
            }
            dao.saveWorkout(
                WorkoutEntity(w.id, w.date, cleanName(w.name), w.notes, w.unit, w.updatedAt),
                exercises,
            )
        }
        return workouts.size
    }
}
