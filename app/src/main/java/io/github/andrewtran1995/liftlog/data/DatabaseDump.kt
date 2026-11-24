package io.github.andrewtran1995.liftlog.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * A full copy of the database: every row of every table, ids included, as editable JSON.
 * Format: docs/database-format.md. Unlike the web-app backup, nothing is lost or renamed in a
 * round trip, so a dump can be edited (by hand, by a script, by Claude) and imported back.
 */
data class DatabaseDump(
    val schemaVersion: Int,
    val exportedAt: Long,
    val types: List<ExerciseTypeEntity>,
    val workouts: List<WorkoutEntity>,
    val exercises: List<ExerciseEntity>,
    val sets: List<SetEntity>,
) {
    companion object {
        const val APP = "license-to-lift"
        const val FORMAT = "database"
        /** Keep equal to the Room database version in LiftLogDatabase. */
        const val SCHEMA_VERSION = 4

        /** True if [text] looks like a full-database dump rather than a web-app backup. */
        fun looksLikeDump(text: String): Boolean =
            runCatching { JSONObject(text.trim()).optString("format") == FORMAT }.getOrDefault(false)

        fun toJson(d: DatabaseDump): String {
            val types = JSONArray()
            d.types.sortedBy { it.name.lowercase() }.forEach {
                types.put(JSONObject().put("id", it.id).put("name", it.name).put("nameKey", it.nameKey).put("createdAt", it.createdAt))
            }
            val workouts = JSONArray()
            d.workouts.sortedWith(compareBy({ it.date }, { it.updatedAt })).forEach {
                workouts.put(
                    JSONObject().put("id", it.id).put("date", it.date).put("name", it.name).put("notes", it.notes)
                        .put("unit", it.unit).put("updatedAt", it.updatedAt).put("deleted", it.deleted).put("dirty", it.dirty),
                )
            }
            val exercises = JSONArray()
            d.exercises.sortedWith(compareBy({ it.workoutId }, { it.position })).forEach {
                exercises.put(
                    JSONObject().put("id", it.id).put("workoutId", it.workoutId).put("position", it.position)
                        .put("name", it.name).put("typeId", it.typeId ?: JSONObject.NULL),
                )
            }
            val sets = JSONArray()
            d.sets.sortedWith(compareBy({ it.exerciseId }, { it.position })).forEach {
                sets.put(
                    JSONObject().put("id", it.id).put("exerciseId", it.exerciseId).put("position", it.position)
                        .put("weight", it.weight ?: JSONObject.NULL).put("reps", it.reps ?: JSONObject.NULL)
                        .put("loggedAt", it.loggedAt ?: JSONObject.NULL),
                )
            }
            return JSONObject()
                .put("app", APP)
                .put("format", FORMAT)
                .put("schemaVersion", d.schemaVersion)
                .put("exportedAt", d.exportedAt)
                .put(
                    "tables",
                    JSONObject().put("exercise_types", types).put("workouts", workouts)
                        .put("exercises", exercises).put("sets", sets),
                )
                .toString(2)
        }

        /**
         * Parses and checks a dump. Throws [IllegalArgumentException] with a message fit to show
         * the user when it can't be imported safely; nothing is written until this passes.
         */
        fun parse(text: String): DatabaseDump {
            val root = try { JSONObject(text.trim()) } catch (e: Exception) { bad("Not valid JSON") }
            if (root.optString("format") != FORMAT) bad("Not a full database export")
            val version = root.optInt("schemaVersion", -1)
            if (version < 1) bad("Missing schemaVersion")
            if (version > SCHEMA_VERSION) bad("Made by a newer version of the app (schema $version). Update the app first.")
            val tables = root.optJSONObject("tables") ?: bad("Missing tables")

            fun rows(name: String): List<JSONObject> {
                val a = tables.optJSONArray(name) ?: return emptyList()
                return (0 until a.length()).map { a.optJSONObject(it) ?: bad("$name[$it] is not an object") }
            }
            fun JSONObject.str(key: String, table: String): String =
                optString(key, "").ifBlank { bad("$table row is missing \"$key\"") }
            fun JSONObject.nullableDouble(key: String): Double? =
                if (isNull(key)) null else optDouble(key).takeIf { !it.isNaN() }
            fun JSONObject.nullableInt(key: String): Int? =
                if (isNull(key)) null else optDouble(key).takeIf { !it.isNaN() }?.toInt()

            val types = rows("exercise_types").map {
                val name = it.str("name", "exercise_types").trim()
                ExerciseTypeEntity(it.str("id", "exercise_types"), name, exerciseKey(name), it.optLong("createdAt", 0L))
            }
            val workouts = rows("workouts").map {
                val date = it.str("date", "workouts")
                if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(date)) bad("workouts row ${it.optString("id")} has a bad date \"$date\"")
                WorkoutEntity(
                    id = it.str("id", "workouts"),
                    date = date,
                    name = it.optString("name", ""),
                    notes = it.optString("notes", ""),
                    unit = it.optString("unit", "lb").takeIf { u -> u == "lb" || u == "kg" } ?: bad("workouts row ${it.optString("id")} has unit other than lb/kg"),
                    updatedAt = it.optLong("updatedAt", System.currentTimeMillis()),
                    deleted = it.optBoolean("deleted", false),
                    dirty = it.optBoolean("dirty", true),
                )
            }
            val exercises = rows("exercises").map {
                ExerciseEntity(
                    id = it.str("id", "exercises"),
                    workoutId = it.str("workoutId", "exercises"),
                    position = it.optInt("position", 0),
                    name = it.str("name", "exercises").trim(),
                    typeId = if (it.isNull("typeId")) null else it.optString("typeId").ifBlank { null },
                )
            }
            val sets = rows("sets").map {
                SetEntity(
                    id = it.str("id", "sets"),
                    exerciseId = it.str("exerciseId", "sets"),
                    position = it.optInt("position", 0),
                    weight = it.nullableDouble("weight")?.takeIf { w -> w > 0 },
                    reps = it.nullableInt("reps")?.takeIf { r -> r > 0 },
                    // Missing in schema 1–2 files: those sets get no time.
                    loggedAt = if (!it.has("loggedAt") || it.isNull("loggedAt")) null else it.optLong("loggedAt").takeIf { t -> t > 0 },
                )
            }

            // Referential checks: every child must come with its parent, so a merge can replace a
            // workout's exercises and sets as a unit.
            fun dupes(ids: List<String>, table: String) {
                val d = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
                if (d.isNotEmpty()) bad("$table has duplicate ids: ${d.take(3).joinToString()}")
            }
            dupes(types.map { it.id }, "exercise_types")
            dupes(workouts.map { it.id }, "workouts")
            dupes(exercises.map { it.id }, "exercises")
            dupes(sets.map { it.id }, "sets")
            val keyDupes = types.groupingBy { it.nameKey }.eachCount().filterValues { it > 1 }.keys
            if (keyDupes.isNotEmpty()) bad("exercise_types has two entries named \"${keyDupes.first()}\"")
            val workoutIds = workouts.map { it.id }.toSet()
            exercises.firstOrNull { it.workoutId !in workoutIds }?.let { bad("exercise ${it.id} points at workout ${it.workoutId}, which isn't in the file") }
            val exerciseIds = exercises.map { it.id }.toSet()
            sets.firstOrNull { it.exerciseId !in exerciseIds }?.let { bad("set ${it.id} points at exercise ${it.exerciseId}, which isn't in the file") }

            return DatabaseDump(version, root.optLong("exportedAt", 0L), types, workouts, exercises, sets)
        }

        private fun bad(message: String): Nothing = throw IllegalArgumentException(message)
    }
}

/** What an import did, for the confirmation message. */
data class ImportResult(val workouts: Int, val exercises: Int, val sets: Int, val catalog: Int)
