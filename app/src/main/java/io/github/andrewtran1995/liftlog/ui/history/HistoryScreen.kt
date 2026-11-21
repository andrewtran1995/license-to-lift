package io.github.andrewtran1995.liftlog.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.andrewtran1995.liftlog.data.WorkoutDao
import io.github.andrewtran1995.liftlog.data.formatDate
import io.github.andrewtran1995.liftlog.data.summarizeSets
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(dao: WorkoutDao) {
    val flow = remember(dao) { dao.observeWorkouts() }
    val workouts by flow.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val list = workouts
    when {
        list == null -> Unit
        list.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No workouts yet. Log your first session.")
        }
        else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("History", style = MaterialTheme.typography.headlineLarge) }
            items(list, key = { it.workout.id }) { w ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(formatDate(w.workout.date), style = MaterialTheme.typography.labelLarge)
                                Text(w.workout.name.ifBlank { "Workout" }, style = MaterialTheme.typography.titleLarge)
                            }
                            TextButton(onClick = { scope.launch { dao.markDeleted(w.workout.id, System.currentTimeMillis()) } }) { Text("Delete") }
                        }
                        w.exercises.sortedBy { it.exercise.position }.forEach { e ->
                            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(e.exercise.name, fontWeight = FontWeight.SemiBold)
                                Text(summarizeSets(e.sets, w.workout.unit), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (w.workout.notes.isNotBlank()) {
                            Text(w.workout.notes, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
