package io.github.andrewtran1995.liftlog.ui.log

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.andrewtran1995.liftlog.data.ExerciseEntity
import io.github.andrewtran1995.liftlog.data.ExerciseTypeEntity
import io.github.andrewtran1995.liftlog.data.ExerciseWithSets
import io.github.andrewtran1995.liftlog.data.SetEntity
import io.github.andrewtran1995.liftlog.data.WorkoutDao
import io.github.andrewtran1995.liftlog.data.WorkoutEntity
import io.github.andrewtran1995.liftlog.data.WorkoutWithExercises
import io.github.andrewtran1995.liftlog.data.exerciseKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class DraftSet(weight: String = "", reps: String = "") {
    var weight by mutableStateOf(weight)
    var reps by mutableStateOf(reps)
}

class DraftExercise(name: String = "", typeId: String? = null) {
    var name by mutableStateOf(name)
    /** Set once the exercise is picked from (or added to) the catalog. */
    var typeId by mutableStateOf(typeId)
    val sets = mutableStateListOf(DraftSet())
}

/** The last time an exercise was done, for the "last time" hint. */
data class LastTime(val workout: WorkoutEntity, val exercise: ExerciseWithSets)

class LogViewModel(private val dao: WorkoutDao) : ViewModel() {
    var date by mutableStateOf(LocalDate.now())
    var name by mutableStateOf("")
    var notes by mutableStateOf("")
    var unit by mutableStateOf("lb") // TODO: move to a DataStore-backed setting
    val exercises = mutableStateListOf(DraftExercise())

    val catalog = dao.observeExerciseTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val workouts = dao.observeWorkouts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<WorkoutWithExercises>())

    /** Most recent logged entry for [ex], matched by catalog entry, or by name for unlinked rows. */
    fun lastTime(ex: DraftExercise): LastTime? {
        val key = exerciseKey(ex.name)
        if (ex.typeId == null && key.isEmpty()) return null
        for (w in workouts.value) { // newest first
            val hit = w.exercises.firstOrNull {
                (ex.typeId != null && it.exercise.typeId == ex.typeId) || exerciseKey(it.exercise.name) == key
            }
            if (hit != null) return LastTime(w.workout, hit)
        }
        return null
    }

    fun pick(ex: DraftExercise, type: ExerciseTypeEntity) {
        ex.name = type.name
        ex.typeId = type.id
    }

    /** Adds a new exercise to the catalog and selects it. */
    fun addToCatalog(ex: DraftExercise, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { pick(ex, dao.ensureType(name)) }
    }

    fun useLastTime(ex: DraftExercise) {
        val last = lastTime(ex) ?: return
        ex.sets.clear()
        last.exercise.sets.sortedBy { it.position }.forEach { s ->
            ex.sets += DraftSet(s.weight?.let { w -> if (w % 1.0 == 0.0) w.toLong().toString() else w.toString() }.orEmpty(), s.reps?.toString().orEmpty())
        }
        if (ex.sets.isEmpty()) ex.sets += DraftSet()
    }

    fun addExercise() { exercises += DraftExercise() }

    fun addSet(ex: DraftExercise) {
        val last = ex.sets.lastOrNull()
        ex.sets += DraftSet(last?.weight.orEmpty(), last?.reps.orEmpty())
    }

    fun removeSet(ex: DraftExercise, set: DraftSet) {
        ex.sets.remove(set)
        if (ex.sets.isEmpty()) ex.sets += DraftSet()
    }

    fun removeExercise(ex: DraftExercise) {
        exercises.remove(ex)
        if (exercises.isEmpty()) exercises += DraftExercise()
    }

    /** Returns false (and saves nothing) when no exercise has a name. Unknown names join the catalog. */
    fun save(onSaved: () -> Unit): Boolean {
        val named = exercises.filter { it.name.isNotBlank() }
        if (named.isEmpty()) return false
        val workoutId = UUID.randomUUID().toString()
        val workout = WorkoutEntity(
            id = workoutId,
            date = date.toString(),
            name = name.trim(),
            notes = notes.trim(),
            unit = unit,
            updatedAt = System.currentTimeMillis(),
        )
        viewModelScope.launch {
            val built = named.mapIndexed { i, ex ->
                val type = dao.ensureType(ex.name)
                val exerciseId = UUID.randomUUID().toString()
                val sets = ex.sets
                    .map { it.weight.replace(',', '.').toDoubleOrNull()?.takeIf { w -> w > 0 } to it.reps.toIntOrNull()?.takeIf { r -> r > 0 } }
                    .filter { (w, r) -> w != null || r != null }
                    .mapIndexed { j, (w, r) -> SetEntity(UUID.randomUUID().toString(), exerciseId, j, w, r) }
                ExerciseEntity(exerciseId, workoutId, i, type.name, type.id) to sets
            }
            dao.saveWorkout(workout, built)
            reset()
            onSaved()
        }
        return true
    }

    private fun reset() {
        date = LocalDate.now(); name = ""; notes = ""
        exercises.clear(); exercises += DraftExercise()
    }
}
