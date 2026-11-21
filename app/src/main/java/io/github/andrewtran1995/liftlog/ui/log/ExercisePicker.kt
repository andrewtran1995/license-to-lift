package io.github.andrewtran1995.liftlog.ui.log

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import io.github.andrewtran1995.liftlog.data.ExerciseTypeEntity
import io.github.andrewtran1995.liftlog.data.exerciseKey
import io.github.andrewtran1995.liftlog.ui.theme.Spy

/**
 * Exercise field backed by the catalog. Typing filters the catalog; a name that isn't in it yet
 * can be added with one tap, so every logged exercise maps to one catalog entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePicker(
    value: String,
    catalog: List<ExerciseTypeEntity>,
    colors: TextFieldColors,
    onValueChange: (String) -> Unit,
    onPick: (ExerciseTypeEntity) -> Unit,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val key = exerciseKey(value)
    val matches = catalog.filter { key.isEmpty() || it.nameKey.contains(key) }.take(8)
    val exact = catalog.any { it.nameKey == key }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); expanded = true },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            placeholder = { Text("Exercise") },
            textStyle = MaterialTheme.typography.titleMedium,
            singleLine = true,
            colors = colors,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        )
        val showAdd = key.isNotEmpty() && !exact
        if (matches.isNotEmpty() || showAdd) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = Spy.PanelHi,
            ) {
                matches.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name, style = MaterialTheme.typography.bodyLarge) },
                        onClick = { onPick(type); expanded = false },
                    )
                }
                if (showAdd) {
                    DropdownMenuItem(
                        text = { Text("+ Add “${value.trim()}” to catalog", color = Spy.Signal, style = MaterialTheme.typography.bodyLarge) },
                        onClick = { onAdd(value); expanded = false },
                    )
                }
            }
        }
    }
}
