package io.github.andrewtran1995.liftlog.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * One gym session. Ids are UUID strings so records made offline on any device never collide,
 * which keeps a future sync backend simple. [dirty] marks rows not yet pushed to a remote;
 * [deleted] is a tombstone so deletes can sync too.
 */
@Entity(tableName = "workouts", indices = [Index("date")])
data class WorkoutEntity(
    @PrimaryKey val id: String,
    /** ISO date, e.g. 2026-11-16, in the user's local calendar. */
    val date: String,
    val name: String,
    val notes: String,
    /** "lb" or "kg": the unit the weights in this session were entered in. */
    val unit: String,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = true,
)

@Entity(
    tableName = "exercises",
    foreignKeys = [ForeignKey(
        entity = WorkoutEntity::class,
        parentColumns = ["id"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("workoutId"), Index("name")],
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val position: Int,
    val name: String,
)

/** A set. Weight and reps are both optional: a set can be just a weight, and bodyweight work has no weight. */
@Entity(
    tableName = "sets",
    foreignKeys = [ForeignKey(
        entity = ExerciseEntity::class,
        parentColumns = ["id"],
        childColumns = ["exerciseId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("exerciseId")],
)
data class SetEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val position: Int,
    val weight: Double?,
    val reps: Int?,
)

data class ExerciseWithSets(
    @Embedded val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "exerciseId")
    val sets: List<SetEntity>,
)

data class WorkoutWithExercises(
    @Embedded val workout: WorkoutEntity,
    @Relation(entity = ExerciseEntity::class, parentColumn = "id", entityColumn = "workoutId")
    val exercises: List<ExerciseWithSets>,
)
