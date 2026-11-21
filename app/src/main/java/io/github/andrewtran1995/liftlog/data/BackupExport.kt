package io.github.andrewtran1995.liftlog.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Writes every filed workout in the web app's backup format, so the file can be re-imported here
 * or pasted into the web app's Settings → restore.
 */
object BackupExport {
    fun toJson(workouts: List<WorkoutWithExercises>): String {
        val sessions = JSONArray()
        workouts.forEach { w ->
            val exercises = JSONArray()
            w.exercises.sortedBy { it.exercise.position }.forEach { e ->
                val sets = JSONArray()
                e.sets.sortedBy { it.position }.forEach { s ->
                    sets.put(JSONObject().put("w", s.weight ?: 0).put("r", s.reps ?: JSONObject.NULL))
                }
                exercises.put(JSONObject().put("name", e.exercise.name).put("sets", sets))
            }
            sessions.put(
                JSONObject()
                    .put("id", w.workout.id)
                    .put("date", w.workout.date)
                    .put("name", w.workout.name)
                    .put("notes", w.workout.notes)
                    .put("unit", w.workout.unit)
                    .put("t", w.workout.updatedAt)
                    .put("exercises", exercises),
            )
        }
        return JSONObject().put("app", "liftlog").put("v", 1).put("sessions", sessions).toString()
    }
}
