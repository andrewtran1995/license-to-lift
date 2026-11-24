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
    indices = [Index("workoutId"), Index("name"), Index("typeId")],
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val position: Int,
    /** Display name as logged; kept so old entries read the same even if the catalog changes. */
    val name: String,
    /** The catalog entry this belongs to; what progress is tracked by. Null only for unmigrated rows. */
    val typeId: String? = null,
)

/**
 * The exercise catalog: the discrete set of exercises you log against, so "Leg curls" in March
 * and "leg curls " in May are the same line on the chart. [nameKey] is the trimmed, lower-cased
 * name and is unique.
 */
@Entity(tableName = "exercise_types", indices = [Index(value = ["nameKey"], unique = true)])
data class ExerciseTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameKey: String,
    val createdAt: Long,
)

/** Exercise and operation names are stored trimmed and lower-case. */
fun cleanName(name: String): String = name.trim().lowercase()

fun exerciseKey(name: String): String = cleanName(name)

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
    /** Epoch millis when the set was logged. Null for sets logged before v3, which never recorded it. */
    val loggedAt: Long? = null,
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
