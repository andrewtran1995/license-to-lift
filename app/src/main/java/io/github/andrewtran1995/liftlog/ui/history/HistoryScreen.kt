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
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import io.github.andrewtran1995.liftlog.data.BackupExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import io.github.andrewtran1995.liftlog.data.SessionStore
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
fun HistoryScreen(dao: WorkoutDao, store: SessionStore, contentPadding: PaddingValues, onEdit: () -> Unit) {
    val flow = remember(dao) { dao.observeWorkouts() }
    val workouts by flow.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activeId by store.activeId.collectAsStateWithLifecycle()
    var importing by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val all = workouts
        if (uri != null && all != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(BackupExport.toJson(all)) }
                }.isSuccess
            }
            Toast.makeText(context, if (ok) "Exported ${all.size} missions" else "Export failed", Toast.LENGTH_SHORT).show()
        }
    }
    if (importing) ImportSheet(dao, onDismiss = { importing = false })
    val list = workouts
    when {
        list == null -> Unit
        list.isEmpty() -> Box(Modifier.fillMaxSize().padding(contentPadding).padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Caption("// Dossier empty")
                Text("No missions on file.", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text("Log your first session to open the dossier.", color = Spy.Muted, textAlign = TextAlign.Center)
                TextButton(onClick = { importing = true }) { Text("IMPORT FROM WEB APP", style = MaterialTheme.typography.labelLarge) }
            }
        }
        else -> LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(verticalAlignment = Alignment.Bottom) {
                    ScreenHeader("// ${list.size} ON FILE", "Dossier", Modifier.weight(1f))
                    TextButton(onClick = { importing = true }) { Text("IMPORT", style = MaterialTheme.typography.labelLarge) }
                    TextButton(onClick = { export.launch("license-to-lift-backup-${LocalDate.now()}.json") }) {
                        Text("EXPORT", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            itemsIndexed(list, key = { _, w -> w.workout.id }) { index, w ->
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                (if (w.workout.id == activeId) "IN PROGRESS  ·  " else "FILE %03d  ·  ".format(list.size - index)) + formatStamp(w.workout.date),
                                style = MaterialTheme.typography.labelMedium, color = Spy.Signal,
                            )
                            Text(
                                "OP: " + w.workout.name.ifBlank { "Untitled" }.uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                        val live = w.workout.id == activeId
                        TextButton(onClick = {
                            if (activeId != null && !live) {
                                Toast.makeText(context, "Finish the mission in progress first", Toast.LENGTH_SHORT).show()
                            } else {
                                store.set(w.workout.id); onEdit()
                            }
                        }) {
                            Text(if (live) "RESUME" else "EDIT", style = MaterialTheme.typography.labelMedium, color = Spy.Signal)
                        }
                        if (!live) TextButton(onClick = { scope.launch { dao.markDeleted(w.workout.id, System.currentTimeMillis()) } }) {
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
