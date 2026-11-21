package io.github.andrewtran1995.liftlog.ui.log

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.andrewtran1995.liftlog.R
import io.github.andrewtran1995.liftlog.data.WorkoutDao
import io.github.andrewtran1995.liftlog.data.formatStamp
import io.github.andrewtran1995.liftlog.ui.theme.Caption
import io.github.andrewtran1995.liftlog.ui.theme.Mono
import io.github.andrewtran1995.liftlog.ui.theme.Panel
import io.github.andrewtran1995.liftlog.ui.theme.ScreenHeader
import io.github.andrewtran1995.liftlog.ui.theme.Spy
import java.time.Instant
import java.time.ZoneOffset

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Spy.Signal,
    unfocusedBorderColor = Spy.Wire,
    focusedContainerColor = Spy.PanelHi,
    unfocusedContainerColor = Spy.PanelHi,
    cursorColor = Spy.Signal,
    focusedLabelColor = Spy.Signal,
    unfocusedLabelColor = Spy.Muted,
    unfocusedPlaceholderColor = Spy.Muted,
    focusedPlaceholderColor = Spy.Muted,
)

private val NumberStyle = androidx.compose.ui.text.TextStyle(
    fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center,
)

// Column widths shared by the set header and every set row, so they line up.
private val SetNumWidth = 28.dp
private val TimesWidth = 14.dp
private val RemoveWidth = 40.dp

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
        item { ScreenHeader("// NEW ENTRY", "Mission log") }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Caption("Date")
                    Text(formatStamp(vm.date.toString()), style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = { pickingDate = true }) { Text("CHANGE", style = MaterialTheme.typography.labelLarge) }
            }
        }
        item {
            OutlinedTextField(
                vm.name, { vm.name = it }, Modifier.fillMaxWidth(),
                label = { Text("Operation") }, placeholder = { Text("e.g. Leg day") },
                singleLine = true, colors = fieldColors(),
            )
        }
        items(vm.exercises) { ex ->
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        ex.name, { ex.name = it }, Modifier.weight(1f),
                        placeholder = { Text("Exercise") },
                        textStyle = MaterialTheme.typography.titleMedium,
                        singleLine = true, colors = fieldColors(),
                    )
                    IconButton(onClick = { vm.removeExercise(ex) }, Modifier.width(RemoveWidth)) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = "Remove exercise", tint = Spy.Muted)
                    }
                }
                // Column captions once per exercise, so the fields need no labels and stay one line high.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Caption("#", Modifier.width(SetNumWidth))
                    Caption("Weight · ${vm.unit}", Modifier.weight(1f))
                    Spacer(TimesWidth)
                    Caption("Reps · opt", Modifier.weight(1f))
                    Spacer(RemoveWidth)
                }
                ex.sets.forEachIndexed { i, set ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "%02d".format(i + 1), Modifier.width(SetNumWidth),
                            style = MaterialTheme.typography.labelLarge, color = Spy.Muted,
                        )
                        OutlinedTextField(
                            set.weight, { set.weight = it }, Modifier.weight(1f),
                            placeholder = { Text("—", Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            textStyle = NumberStyle, singleLine = true, colors = fieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                        Text("×", Modifier.width(TimesWidth), color = Spy.Muted, textAlign = TextAlign.Center)
                        OutlinedTextField(
                            set.reps, { set.reps = it }, Modifier.weight(1f),
                            placeholder = { Text("—", Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            textStyle = NumberStyle, singleLine = true, colors = fieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        IconButton(onClick = { vm.removeSet(ex, set) }, Modifier.width(RemoveWidth)) {
                            Icon(painterResource(R.drawable.ic_close), contentDescription = "Remove set ${i + 1}", tint = Spy.Muted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                TextButton(onClick = { vm.addSet(ex) }) { Text("+ ADD SET", style = MaterialTheme.typography.labelLarge) }
            }
        }
        item {
            OutlinedButton(
                onClick = vm::addExercise,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, Spy.Wire),
            ) { Text("+ ADD EXERCISE", style = MaterialTheme.typography.labelLarge) }
        }
        item {
            OutlinedTextField(
                vm.notes, { vm.notes = it }, Modifier.fillMaxWidth().height(110.dp),
                label = { Text("Field notes") }, placeholder = { Text("How it went, anything to remember") },
                colors = fieldColors(),
            )
        }
        item {
            Button(
                onClick = {
                    val ok = vm.save {
                        Toast.makeText(context, "Mission logged", Toast.LENGTH_SHORT).show()
                        onSaved()
                    }
                    if (!ok) Toast.makeText(context, "Add at least one exercise", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = MaterialTheme.shapes.medium,
            ) { Text("LOG MISSION", style = MaterialTheme.typography.labelLarge) }
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
                }) { Text("CONFIRM") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("ABORT") } },
        ) { DatePicker(state) }
    }
}

@Composable
private fun Spacer(width: androidx.compose.ui.unit.Dp) = androidx.compose.foundation.layout.Spacer(Modifier.width(width))
