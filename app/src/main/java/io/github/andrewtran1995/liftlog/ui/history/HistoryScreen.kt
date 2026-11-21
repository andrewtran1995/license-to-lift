package io.github.andrewtran1995.liftlog.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.andrewtran1995.liftlog.data.WorkoutDao
import io.github.andrewtran1995.liftlog.data.formatStamp
import io.github.andrewtran1995.liftlog.data.summarizeSets
import io.github.andrewtran1995.liftlog.ui.theme.Caption
import io.github.andrewtran1995.liftlog.ui.theme.Panel
import io.github.andrewtran1995.liftlog.ui.theme.ReadoutRow
import io.github.andrewtran1995.liftlog.ui.theme.ScreenHeader
import io.github.andrewtran1995.liftlog.ui.theme.Spy
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(dao: WorkoutDao, contentPadding: PaddingValues) {
    val flow = remember(dao) { dao.observeWorkouts() }
    val workouts by flow.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val list = workouts
    when {
        list == null -> Unit
        list.isEmpty() -> Box(Modifier.fillMaxSize().padding(contentPadding).padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Caption("// Dossier empty")
                Text("No missions on file.", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text("Log your first session to open the dossier.", color = Spy.Muted, textAlign = TextAlign.Center)
            }
        }
        else -> LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { ScreenHeader("// ${list.size} ON FILE", "Dossier") }
            itemsIndexed(list) { index, w ->
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "FILE %03d  ·  %s".format(list.size - index, formatStamp(w.workout.date)),
                                style = MaterialTheme.typography.labelMedium, color = Spy.Signal,
                            )
                            Text(
                                "OP: " + w.workout.name.ifBlank { "Untitled" }.uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                        TextButton(onClick = { scope.launch { dao.markDeleted(w.workout.id, System.currentTimeMillis()) } }) {
                            Text("BURN", style = MaterialTheme.typography.labelMedium, color = Spy.Classified)
                        }
                    }
                    HorizontalDivider(color = Spy.Wire)
                    w.exercises.sortedBy { it.exercise.position }.forEach { e ->
                        ReadoutRow(e.exercise.name, summarizeSets(e.sets, w.workout.unit))
                    }
                    if (w.workout.notes.isNotBlank()) {
                        Text("> " + w.workout.notes, style = MaterialTheme.typography.bodySmall, color = Spy.Muted)
                    }
                }
            }
        }
    }
}
