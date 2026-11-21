package io.github.andrewtran1995.liftlog.ui.history

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.github.andrewtran1995.liftlog.data.BackupExport
import io.github.andrewtran1995.liftlog.data.BackupImport
import io.github.andrewtran1995.liftlog.data.DatabaseDump
import io.github.andrewtran1995.liftlog.data.SessionStore
import io.github.andrewtran1995.liftlog.data.WorkoutDao
import io.github.andrewtran1995.liftlog.ui.theme.Caption
import io.github.andrewtran1995.liftlog.ui.theme.Spy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/** What was pasted or picked, once its format is known. */
private sealed interface Incoming {
    val label: String
    data class Dump(val dump: DatabaseDump, override val label: String) : Incoming
    data class Web(val raw: String, val count: Int, override val label: String) : Incoming
    data class Invalid(override val label: String) : Incoming
}

private fun detect(raw: String, source: String): Incoming = try {
    if (DatabaseDump.looksLikeDump(raw)) {
        val d = DatabaseDump.parse(raw)
        Incoming.Dump(d, "$source · full database · ${d.workouts.count { !it.deleted }} missions, ${d.types.size} exercises")
    } else {
        val n = BackupImport.parse(raw).size
        if (n == 0) Incoming.Invalid("$source · no workouts found") else Incoming.Web(raw, n, "$source · web app backup · $n missions")
    }
} catch (e: IllegalArgumentException) {
    Incoming.Invalid("$source · can't import: ${e.message}")
} catch (e: Exception) {
    Incoming.Invalid("$source · not a License to Lift or Lift Log export")
}

private fun fileName(context: Context, uri: Uri): String =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    } ?: "file"

/**
 * Everything about moving data in and out: export the full database (save or share), export a
 * web-app backup, and import either format. A full database can be merged into what's here or
 * replace it entirely.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSheet(dao: WorkoutDao, store: SessionStore, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pasted by remember { mutableStateOf("") }
    var incoming by remember { mutableStateOf<Incoming?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirmReplace by remember { mutableStateOf(false) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    val stamp = LocalDate.now().toString()

    val saveDump = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val json = DatabaseDump.toJson(dao.dump())
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
                }.isSuccess
            }
            toast(if (ok) "Database exported" else "Export failed")
        }
    }
    val saveWeb = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val json = BackupExport.toJson(dao.dump().let { d ->
                        // Same rows as the live Dossier: filed workouts only.
                        val live = d.workouts.filter { !it.deleted }
                        live.map { w ->
                            io.github.andrewtran1995.liftlog.data.WorkoutWithExercises(
                                w,
                                d.exercises.filter { it.workoutId == w.id }.map { e ->
                                    io.github.andrewtran1995.liftlog.data.ExerciseWithSets(e, d.sets.filter { it.exerciseId == e.id })
                                },
                            )
                        }
                    })
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
                }.isSuccess
            }
            toast(if (ok) "Web app backup exported" else "Export failed")
        }
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            incoming = withContext(Dispatchers.IO) {
                val raw = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
                if (raw == null) Incoming.Invalid("Couldn't read that file") else detect(raw, fileName(context, uri))
            }
            confirmReplace = false
            busy = false
        }
    }

    fun share() = scope.launch {
        busy = true
        val uri = withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(dir, "license-to-lift-db-$stamp.json")
                file.writeText(DatabaseDump.toJson(dao.dump()))
                FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
            }.getOrNull()
        }
        busy = false
        if (uri == null) { toast("Export failed"); return@launch }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "License to Lift database $stamp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share database"))
    }

    fun apply(replace: Boolean) = scope.launch {
        val inc = incoming ?: return@launch
        busy = true
        val msg = runCatching {
            when (inc) {
                is Incoming.Dump -> {
                    val r = dao.importDump(inc.dump, replace)
                    // Don't leave the Mission tab pointing at a workout that no longer exists.
                    val active = store.activeId.value
                    if (replace && active != null && inc.dump.workouts.none { it.id == active && !it.deleted }) store.set(null)
                    (if (replace) "Replaced: " else "Merged: ") + "${r.workouts} missions, ${r.sets} sets"
                }
                is Incoming.Web -> "Imported ${BackupImport.save(dao, BackupImport.parse(inc.raw))} missions"
                is Incoming.Invalid -> null
            }
        }.getOrElse { "Import failed: ${it.message}" }
        busy = false
        if (msg != null) { toast(msg); if (!msg.startsWith("Import failed")) onDismiss() }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Spy.Signal, unfocusedBorderColor = Spy.Wire,
        focusedContainerColor = Spy.PanelHi, unfocusedContainerColor = Spy.PanelHi,
    )

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Spy.Panel) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Caption("// Data transfer")
            Text("EXPORT", style = MaterialTheme.typography.headlineSmall)
            Text(
                "The full database has every table and id, so it can be edited and imported back without losing anything.",
                style = MaterialTheme.typography.bodyMedium, color = Spy.Muted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { saveDump.launch("license-to-lift-db-$stamp.json") }, enabled = !busy, modifier = Modifier.weight(1f).height(52.dp)) {
                    Text("SAVE FILE", style = MaterialTheme.typography.labelLarge)
                }
                Button(onClick = { share() }, enabled = !busy, modifier = Modifier.weight(1f).height(52.dp)) {
                    Text("SHARE", style = MaterialTheme.typography.labelLarge)
                }
            }
            TextButton(onClick = { saveWeb.launch("lift-log-backup-$stamp.json") }, enabled = !busy) {
                Text("Web app backup instead", style = MaterialTheme.typography.labelMedium)
            }

            HorizontalDivider(color = Spy.Wire)

            Text("IMPORT", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Choose a full database export or a web app backup, or paste its text.",
                style = MaterialTheme.typography.bodyMedium, color = Spy.Muted,
            )
            OutlinedTextField(
                value = pasted,
                onValueChange = { pasted = it; incoming = if (it.isBlank()) null else detect(it, "Pasted"); confirmReplace = false },
                modifier = Modifier.fillMaxWidth().height(110.dp),
                placeholder = { Text("{\"format\":\"database\", …}") },
                textStyle = MaterialTheme.typography.bodySmall,
                colors = fieldColors,
            )
            OutlinedButton(onClick = { pickFile.launch(arrayOf("application/json", "text/plain", "*/*")) }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("CHOOSE FILE", style = MaterialTheme.typography.labelLarge)
            }

            incoming?.let { inc ->
                Text(inc.label, style = MaterialTheme.typography.bodyMedium, color = if (inc is Incoming.Invalid) Spy.Classified else Spy.Signal)
                when (inc) {
                    is Incoming.Dump -> {
                        Text(
                            "Merge: the file's rows win and its missions replace their copies here; everything else stays. Replace: this phone's data becomes exactly the file.",
                            style = MaterialTheme.typography.bodySmall, color = Spy.Muted,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { apply(replace = false) }, enabled = !busy, modifier = Modifier.weight(1f).height(52.dp)) {
                                Text("MERGE", style = MaterialTheme.typography.labelLarge)
                            }
                            OutlinedButton(
                                onClick = { if (confirmReplace) apply(replace = true) else confirmReplace = true },
                                enabled = !busy,
                                modifier = Modifier.weight(1f).height(52.dp),
                            ) {
                                Text(if (confirmReplace) "TAP TO REPLACE" else "REPLACE ALL", style = MaterialTheme.typography.labelLarge, color = Spy.Classified)
                            }
                        }
                        if (confirmReplace) Caption("Replace deletes everything not in the file. Export first if unsure.")
                    }
                    is Incoming.Web -> Button(onClick = { apply(replace = false) }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("IMPORT ${inc.count} MISSIONS", style = MaterialTheme.typography.labelLarge)
                    }
                    is Incoming.Invalid -> Unit
                }
            }
        }
    }
}
