package io.github.andrewtran1995.liftlog.ui.log

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.andrewtran1995.liftlog.data.ExerciseEntity
import io.github.andrewtran1995.liftlog.data.ExerciseTypeEntity
import io.github.andrewtran1995.liftlog.data.ExerciseWithSets
import io.github.andrewtran1995.liftlog.data.SessionStore
import io.github.andrewtran1995.liftlog.data.SetEntity
import io.github.andrewtran1995.liftlog.data.WorkoutDao
import io.github.andrewtran1995.liftlog.data.WorkoutEntity
import io.github.andrewtran1995.liftlog.data.WorkoutWithExercises
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.UUID

/** The last time an exercise was done, for prefill and the "last time" line. */
data class LastTime(val workout: WorkoutEntity, val exercise: ExerciseWithSets)

/** The numbers waiting in the entry panel for the next set. */
class Entry(weight: String, reps: Int?, val fromLastTime: Boolean) {
    var weight by mutableStateOf(weight)
    var reps by mutableStateOf(reps)
}

/**
 * A live mission. Every change is written to Room straight away (no save button), and the active
 * workout's id is kept in [SessionStore], so closing the app mid-session loses nothing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModel(private val dao: WorkoutDao, private val store: SessionStore) : ViewModel() {
    val unit = "lb" // TODO: follow a units setting

    val activeId: StateFlow<String?> = store.activeId

    val session: StateFlow<WorkoutWithExercises?> = store.activeId
        .flatMapLatest { id -> if (id == null) flowOf(null) else dao.observeWorkout(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val catalog = dao.observeExerciseTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history = dao.observeWorkouts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Index of the exercise tab on screen; equal to the exercise count when the add panel is open. */
    var selected by mutableIntStateOf(0)

    private val writes = Mutex() // keeps rapid taps from interleaving positions

    private suspend fun ensureWorkout(): String {
        store.activeId.value?.let { return it }
        val id = UUID.randomUUID().toString()
        dao.upsertWorkout(WorkoutEntity(id, LocalDate.now().toString(), "", "", unit, System.currentTimeMillis()))
        store.set(id)
        return id
    }

    private fun exercisesNow(): List<ExerciseWithSets> =
        session.value?.exercises?.sortedBy { it.exercise.position }.orEmpty()

    /** Newest earlier entry for a catalog exercise, ignoring the mission in progress. */
    fun lastTime(typeId: String?, history: List<WorkoutWithExercises>): LastTime? {
        if (typeId == null) return null
        val active = store.activeId.value
        for (w in history) {
            if (w.workout.id == active) continue
            val hit = w.exercises.firstOrNull { it.exercise.typeId == typeId } ?: continue
            return LastTime(w.workout, hit)
        }
        return null
    }

    /**
     * Prefill for the next set: repeat the set just logged; otherwise use the matching set from
     * last time (set 2 from last time for your 2nd set), else its final set; otherwise blank.
     */
    fun nextEntry(ex: ExerciseWithSets, last: LastTime?): Entry {
        val logged = ex.sets.sortedBy { it.position }
        logged.lastOrNull()?.let { return Entry(fmtWeight(it.weight), it.reps, fromLastTime = false) }
        val prev = last?.exercise?.sets?.sortedBy { it.position }.orEmpty()
        val pick = prev.getOrNull(logged.size) ?: prev.lastOrNull()
        return if (pick != null) Entry(fmtWeight(pick.weight), pick.reps, fromLastTime = true) else Entry("", null, false)
    }

    fun addExercise(type: ExerciseTypeEntity) = viewModelScope.launch {
        writes.withLock {
            val wid = ensureWorkout()
            val index = exercisesNow().size
            val pos = (exercisesNow().maxOfOrNull { it.exercise.position } ?: -1) + 1
            dao.upsertExercises(listOf(ExerciseEntity(UUID.randomUUID().toString(), wid, pos, type.name, type.id)))
            dao.touchWorkout(wid, System.currentTimeMillis())
            selected = index
        }
    }

    fun addByName(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) addExercise(dao.ensureType(name)).join()
    }

    /** Loads the exercises (not the sets) of an earlier workout, and its name if this one has none. */
    fun repeat(from: WorkoutWithExercises) = viewModelScope.launch {
        writes.withLock {
            val wid = ensureWorkout()
            val have = exercisesNow().mapNotNull { it.exercise.typeId }.toMutableSet()
            var pos = (exercisesNow().maxOfOrNull { it.exercise.position } ?: -1) + 1
            val firstNew = exercisesNow().size
            from.exercises.sortedBy { it.exercise.position }.forEach { e ->
                val type = dao.ensureType(e.exercise.name)
                if (have.add(type.id)) {
                    dao.upsertExercises(listOf(ExerciseEntity(UUID.randomUUID().toString(), wid, pos++, type.name, type.id)))
                }
            }
            val current = session.value?.workout
            if (current != null && current.name.isBlank() && from.workout.name.isNotBlank()) {
                dao.upsertWorkout(current.copy(name = from.workout.name, updatedAt = System.currentTimeMillis()))
            } else {
                dao.touchWorkout(wid, System.currentTimeMillis())
            }
            selected = firstNew
        }
    }

    fun logSet(ex: ExerciseWithSets, entry: Entry) = viewModelScope.launch {
        val w = entry.weight.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
        val r = entry.reps?.takeIf { it > 0 }
        writes.withLock {
            val pos = (ex.sets.maxOfOrNull { it.position } ?: -1) + 1
            dao.upsertSets(listOf(SetEntity(UUID.randomUUID().toString(), ex.exercise.id, pos, w, r)))
            dao.touchWorkout(ex.exercise.workoutId, System.currentTimeMillis())
        }
    }

    fun deleteSet(set: SetEntity, workoutId: String) = viewModelScope.launch {
        writes.withLock { dao.deleteSet(set.id); dao.touchWorkout(workoutId, System.currentTimeMillis()) }
    }

    fun removeExercise(ex: ExerciseWithSets) = viewModelScope.launch {
        writes.withLock {
            dao.deleteSetsFor(ex.exercise.id)
            dao.deleteExercise(ex.exercise.id)
            dao.touchWorkout(ex.exercise.workoutId, System.currentTimeMillis())
            selected = (selected - 1).coerceAtLeast(0)
        }
    }

    fun updateDetails(name: String, notes: String, date: LocalDate) = viewModelScope.launch {
        val current = session.value?.workout ?: return@launch
        writes.withLock {
            dao.upsertWorkout(current.copy(name = name.trim(), notes = notes.trim(), date = date.toString(), updatedAt = System.currentTimeMillis()))
        }
    }

    /** Ends the mission. One with no logged sets is thrown away rather than filed. */
    fun finish(onDone: (filed: Boolean) -> Unit) = viewModelScope.launch {
        val s = session.value
        // An exercise with no sets still counts (e.g. "Knee lifts: done").
        val filed = s != null && s.exercises.isNotEmpty()
        if (s != null && !filed) dao.markDeleted(s.workout.id, System.currentTimeMillis())
        store.set(null)
        selected = 0
        onDone(filed)
    }

    fun discard() = viewModelScope.launch {
        store.activeId.value?.let { dao.markDeleted(it, System.currentTimeMillis()) }
        store.set(null)
        selected = 0
    }

    /** Re-opens a filed workout for editing. Returns false if another mission is in progress. */
    fun resume(id: String): Boolean {
        val active = store.activeId.value
        if (active != null && active != id) return false
        store.set(id)
        selected = 0
        return true
    }

    companion object {
        fun fmtWeight(w: Double?): String = when {
            w == null || w <= 0.0 -> ""
            w % 1.0 == 0.0 -> w.toLong().toString()
            else -> w.toString()
        }
    }
}
