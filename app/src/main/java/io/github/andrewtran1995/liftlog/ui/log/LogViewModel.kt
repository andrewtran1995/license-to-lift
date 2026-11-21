package io.github.andrewtran1995.liftlog.ui.log

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.andrewtran1995.liftlog.data.ExerciseEntity
import io.github.andrewtran1995.liftlog.data.SetEntity
import io.github.andrewtran1995.liftlog.data.WorkoutDao
import io.github.andrewtran1995.liftlog.data.WorkoutEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class DraftSet(weight: String = "", reps: String = "") {
    var weight by mutableStateOf(weight)
    var reps by mutableStateOf(reps)
}

class DraftExercise(name: String = "") {
    var name by mutableStateOf(name)
    val sets = mutableStateListOf(DraftSet())
}

class LogViewModel(private val dao: WorkoutDao) : ViewModel() {
    var date by mutableStateOf(LocalDate.now())
    var name by mutableStateOf("")
    var notes by mutableStateOf("")
    var unit by mutableStateOf("lb") // TODO: move to a DataStore-backed setting
    val exercises = mutableStateListOf(DraftExercise())

    val exerciseNames = dao.observeExerciseNames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    /** Returns false (and saves nothing) when no exercise has a name. */
    fun save(onSaved: () -> Unit): Boolean {
        val workoutId = UUID.randomUUID().toString()
        val built = exercises.filter { it.name.isNotBlank() }.mapIndexed { i, ex ->
            val exerciseId = UUID.randomUUID().toString()
            val sets = ex.sets
                .map { it.weight.replace(',', '.').toDoubleOrNull() to it.reps.toIntOrNull()?.takeIf { r -> r > 0 } }
                .filter { (w, r) -> w != null || r != null }
                .mapIndexed { j, (w, r) -> SetEntity(UUID.randomUUID().toString(), exerciseId, j, w, r) }
            ExerciseEntity(exerciseId, workoutId, i, ex.name.trim()) to sets
        }
        if (built.isEmpty()) return false
        val workout = WorkoutEntity(
            id = workoutId,
            date = date.toString(),
            name = name.trim(),
            notes = notes.trim(),
            unit = unit,
            updatedAt = System.currentTimeMillis(),
        )
        viewModelScope.launch {
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
