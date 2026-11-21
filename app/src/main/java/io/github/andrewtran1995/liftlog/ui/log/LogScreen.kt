package io.github.andrewtran1995.liftlog.ui.log

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.andrewtran1995.liftlog.data.WorkoutDao
import io.github.andrewtran1995.liftlog.data.formatDate
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(dao: WorkoutDao, onSaved: () -> Unit) {
    val vm: LogViewModel = viewModel(factory = viewModelFactory { initializer { LogViewModel(dao) } })
    val context = LocalContext.current
    var pickingDate by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Log a workout", style = MaterialTheme.typography.headlineLarge) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatDate(vm.date.toString()), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { pickingDate = true }) { Text("Change date") }
            }
        }
        item {
            OutlinedTextField(vm.name, { vm.name = it }, Modifier.fillMaxWidth(), label = { Text("Workout (e.g. Leg day)") }, singleLine = true)
        }
        items(vm.exercises) { ex ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(ex.name, { ex.name = it }, Modifier.weight(1f), label = { Text("Exercise") }, singleLine = true)
                        TextButton(onClick = { vm.removeExercise(ex) }) { Text("Remove") }
                    }
                    ex.sets.forEachIndexed { i, set ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${i + 1}", Modifier.width(20.dp), textAlign = TextAlign.Center)
                            OutlinedTextField(
                                set.weight, { set.weight = it }, Modifier.weight(1f),
                                label = { Text(vm.unit) }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            )
                            Text("×")
                            OutlinedTextField(
                                set.reps, { set.reps = it }, Modifier.weight(1f),
                                label = { Text("reps (opt.)") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                            TextButton(onClick = { vm.removeSet(ex, set) }) { Text("✕") }
                        }
                    }
                    TextButton(onClick = { vm.addSet(ex) }) { Text("+ Add set") }
                }
            }
        }
        item { TextButton(onClick = vm::addExercise) { Text("+ Add exercise") } }
        item {
            OutlinedTextField(vm.notes, { vm.notes = it }, Modifier.fillMaxWidth().height(110.dp), label = { Text("Notes (optional)") })
        }
        item {
            Button(
                onClick = {
                    val ok = vm.save {
                        Toast.makeText(context, "Workout saved", Toast.LENGTH_SHORT).show()
                        onSaved()
                    }
                    if (!ok) Toast.makeText(context, "Add at least one exercise", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Save workout") }
        }
    }

    if (pickingDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = vm.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { vm.date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
}
