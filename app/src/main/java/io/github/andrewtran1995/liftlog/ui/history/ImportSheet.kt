package io.github.andrewtran1995.liftlog.ui.history

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.andrewtran1995.liftlog.data.BackupImport
import io.github.andrewtran1995.liftlog.data.WorkoutDao
import io.github.andrewtran1995.liftlog.ui.theme.Caption
import io.github.andrewtran1995.liftlog.ui.theme.Spy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Brings workouts over from the web app: paste the text from its Settings → Copy backup, or pick
 * a saved .json file. Re-importing the same backup updates entries instead of duplicating them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSheet(dao: WorkoutDao, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun run(raw: String) {
        busy = true
        scope.launch {
            val message = try {
                val parsed = withContext(Dispatchers.Default) { BackupImport.parse(raw) }
                if (parsed.isEmpty()) "No workouts found in that backup"
                else "Imported ${BackupImport.save(dao, parsed)} missions"
            } catch (e: Exception) {
                "That isn't a Lift Log backup"
            }
            busy = false
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            if (message.startsWith("Imported")) onDismiss()
        }
    }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val raw = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }
            if (raw != null) run(raw)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Spy.Panel) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Caption("// Inbound transfer")
            Text("IMPORT FROM WEB APP", style = MaterialTheme.typography.headlineSmall)
            Text(
                "In the web app, open Settings → Copy backup, then paste it here. Or choose a saved backup file.",
                style = MaterialTheme.typography.bodyMedium, color = Spy.Muted,
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(140.dp),
                placeholder = { Text("{\"app\":\"liftlog\",\"sessions\":[…]}") },
                textStyle = MaterialTheme.typography.bodySmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Spy.Signal, unfocusedBorderColor = Spy.Wire,
                    focusedContainerColor = Spy.PanelHi, unfocusedContainerColor = Spy.PanelHi,
                ),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { pickFile.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    enabled = !busy,
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("CHOOSE FILE", style = MaterialTheme.typography.labelLarge) }
                Button(
                    onClick = { run(text) },
                    enabled = !busy && text.isNotBlank(),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text(if (busy) "IMPORTING…" else "IMPORT", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}
