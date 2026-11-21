package io.github.andrewtran1995.liftlog.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

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

    @Upsert abstract suspend fun upsertWorkout(workout: WorkoutEntity)
    @Upsert abstract suspend fun upsertExercises(exercises: List<ExerciseEntity>)
    @Upsert abstract suspend fun upsertSets(sets: List<SetEntity>)

    @Query("DELETE FROM exercises WHERE workoutId = :workoutId")
    abstract suspend fun clearExercises(workoutId: String)

    @Query("UPDATE workouts SET deleted = 1, dirty = 1, updatedAt = :now WHERE id = :id")
    abstract suspend fun markDeleted(id: String, now: Long)

    /** Saves a whole workout atomically, replacing its exercises and sets. */
    @Transaction
    open suspend fun saveWorkout(workout: WorkoutEntity, exercises: List<Pair<ExerciseEntity, List<SetEntity>>>) {
        upsertWorkout(workout)
        clearExercises(workout.id)
        upsertExercises(exercises.map { it.first })
        upsertSets(exercises.flatMap { it.second })
    }
}
