package io.github.andrewtran1995.liftlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
abstract class WorkoutDao {

    @Transaction
    @Query("SELECT * FROM workouts WHERE deleted = 0 ORDER BY date DESC, updatedAt DESC")
    abstract fun observeWorkouts(): Flow<List<WorkoutWithExercises>>

    /** Exercise names you've used, most recently done first. Feeds name suggestions. */
    @Query(
        """
        SELECT e.name FROM exercises e JOIN workouts w ON w.id = e.workoutId
        WHERE w.deleted = 0
        GROUP BY e.name COLLATE NOCASE
        ORDER BY MAX(w.date) DESC
        """
    )
    abstract fun observeExerciseNames(): Flow<List<String>>

    /** The catalog, most recently trained first; never-used entries last, alphabetically. */
    @Query(
        """
        SELECT t.* FROM exercise_types t
        LEFT JOIN exercises e ON e.typeId = t.id
        LEFT JOIN workouts w ON w.id = e.workoutId AND w.deleted = 0
        GROUP BY t.id
        ORDER BY MAX(w.date) DESC, t.name COLLATE NOCASE
        """
    )
    abstract fun observeExerciseTypes(): Flow<List<ExerciseTypeEntity>>

    @Query("SELECT * FROM exercise_types WHERE nameKey = :key LIMIT 1")
    abstract suspend fun typeByKey(key: String): ExerciseTypeEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertType(type: ExerciseTypeEntity)

    /** Returns the catalog entry for [name], adding it to the catalog if it's new. */
    @Transaction
    open suspend fun ensureType(name: String): ExerciseTypeEntity {
        val key = exerciseKey(name)
        typeByKey(key)?.let { return it }
        val type = ExerciseTypeEntity(UUID.randomUUID().toString(), key, key, System.currentTimeMillis())
        insertType(type)
        return typeByKey(key) ?: type
    }

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    abstract fun observeWorkout(id: String): Flow<WorkoutWithExercises?>

    @Query("DELETE FROM sets WHERE id = :id")
    abstract suspend fun deleteSet(id: String)

    @Query("DELETE FROM sets WHERE exerciseId = :exerciseId")
    abstract suspend fun deleteSetsFor(exerciseId: String)

    @Query("DELETE FROM exercises WHERE id = :id")
    abstract suspend fun deleteExercise(id: String)

    @Query("UPDATE workouts SET updatedAt = :now, dirty = 1 WHERE id = :id")
    abstract suspend fun touchWorkout(id: String, now: Long)

    // ---- full-database export / import ----

    @Query("SELECT * FROM exercise_types") abstract suspend fun allTypes(): List<ExerciseTypeEntity>
    @Query("SELECT * FROM workouts") abstract suspend fun allWorkoutRows(): List<WorkoutEntity>
    @Query("SELECT * FROM exercises") abstract suspend fun allExerciseRows(): List<ExerciseEntity>
    @Query("SELECT * FROM sets") abstract suspend fun allSetRows(): List<SetEntity>
    @Query("SELECT * FROM exercise_types WHERE id = :id") abstract suspend fun typeById(id: String): ExerciseTypeEntity?
    @Upsert abstract suspend fun upsertTypes(types: List<ExerciseTypeEntity>)
    @Query("DELETE FROM sets") abstract suspend fun wipeSets()
    @Query("DELETE FROM exercises") abstract suspend fun wipeExercises()
    @Query("DELETE FROM workouts") abstract suspend fun wipeWorkouts()
    @Query("DELETE FROM exercise_types") abstract suspend fun wipeTypes()

    /** Every row of every table, including deleted (tombstoned) workouts. */
    @Transaction
    open suspend fun dump(): DatabaseDump = DatabaseDump(
        schemaVersion = DatabaseDump.SCHEMA_VERSION,
        exportedAt = System.currentTimeMillis(),
        types = allTypes(),
        workouts = allWorkoutRows(),
        exercises = allExerciseRows(),
        sets = allSetRows(),
    )

    /**
     * Applies a parsed dump in one transaction: it all lands or none of it does.
     *
     * Replace: empties every table first, so the database becomes exactly the file.
     * Merge: rows in the file win; each workout in the file has its exercises and sets replaced
     * by the file's; nothing outside the file is touched. A catalog entry whose name already
     * exists under another id is folded into the existing one.
     */
    @Transaction
    open suspend fun importDump(file: DatabaseDump, replace: Boolean): ImportResult {
        val d = file.copy(
            types = file.types.map { it.copy(name = cleanName(it.name), nameKey = cleanName(it.name)) },
            workouts = file.workouts.map { it.copy(name = cleanName(it.name)) },
            exercises = file.exercises.map { it.copy(name = cleanName(it.name)) },
)
        if (replace) { wipeSets(); wipeExercises(); wipeWorkouts(); wipeTypes() }

        val remap = mutableMapOf<String, String>()
        for (t in d.types) {
            val byKey = typeByKey(t.nameKey)
            if (byKey != null && byKey.id != t.id) remap[t.id] = byKey.id
            else upsertTypes(listOf(t))
        }
        for (w in d.workouts) {
            clearSetsForWorkout(w.id)
            clearExercises(w.id)
            upsertWorkout(w)
        }
        val fileTypeIds = d.types.map { it.id }.toSet()
        val exercises = d.exercises.map { e ->
            val typeId = e.typeId?.let { remap[it] ?: it.takeIf { id -> id in fileTypeIds || typeById(id) != null } }
                ?: ensureType(e.name).id
            e.copy(typeId = typeId)
        }
        upsertExercises(exercises)
        upsertSets(d.sets)
        return ImportResult(d.workouts.size, exercises.size, d.sets.size, d.types.size - remap.size)
    }

    @Upsert abstract suspend fun upsertWorkout(workout: WorkoutEntity)
    @Upsert abstract suspend fun upsertExercises(exercises: List<ExerciseEntity>)
    @Upsert abstract suspend fun upsertSets(sets: List<SetEntity>)

    @Query("DELETE FROM sets WHERE exerciseId IN (SELECT id FROM exercises WHERE workoutId = :workoutId)")
    abstract suspend fun clearSetsForWorkout(workoutId: String)

    @Query("DELETE FROM exercises WHERE workoutId = :workoutId")
    abstract suspend fun clearExercises(workoutId: String)

    @Query("UPDATE workouts SET deleted = 1, dirty = 1, updatedAt = :now WHERE id = :id")
    abstract suspend fun markDeleted(id: String, now: Long)

    /** Saves a whole workout atomically, replacing its exercises and sets. */
    @Transaction
    open suspend fun saveWorkout(workout: WorkoutEntity, exercises: List<Pair<ExerciseEntity, List<SetEntity>>>) {
        upsertWorkout(workout)
        clearSetsForWorkout(workout.id)
        clearExercises(workout.id)
        upsertExercises(exercises.map { it.first })
        upsertSets(exercises.flatMap { it.second })
    }
}
