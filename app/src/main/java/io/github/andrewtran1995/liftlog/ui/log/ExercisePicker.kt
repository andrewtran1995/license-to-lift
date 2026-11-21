package io.github.andrewtran1995.liftlog.ui.log

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import io.github.andrewtran1995.liftlog.data.ExerciseTypeEntity
import io.github.andrewtran1995.liftlog.data.exerciseKey
import io.github.andrewtran1995.liftlog.ui.theme.Spy

/**
 * Exercise field backed by the catalog. Typing filters the catalog; a name that isn't in it yet
 * can be added with one tap, so every logged exercise maps to one catalog entry. The suggestion
 * menu doesn't take focus, so the keyboard stays up while you type.
 */
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
    var focused by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    var widthPx by remember { mutableStateOf(0) }
    val key = exerciseKey(value)
    val matches = catalog.filter { (key.isEmpty() || it.nameKey.contains(key)) && it.nameKey != key }.take(8)
    val showAdd = key.isNotEmpty() && catalog.none { it.nameKey == key }
    val expanded = focused && !dismissed && (matches.isNotEmpty() || showAdd)

    Box(modifier.onSizeChanged { widthPx = it.width }) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); dismissed = false },
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused; if (it.isFocused) dismissed = false },
            placeholder = { Text("Exercise") },
            textStyle = MaterialTheme.typography.titleMedium,
            singleLine = true,
            colors = colors,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { dismissed = true },
            properties = PopupProperties(focusable = false),
            containerColor = Spy.PanelHi,
            modifier = Modifier.width(with(LocalDensity.current) { widthPx.toDp() }.coerceAtLeast(200.dp)),
        ) {
            matches.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name, style = MaterialTheme.typography.bodyLarge) },
                    onClick = { onPick(type); dismissed = true },
                )
            }
            if (showAdd) {
                DropdownMenuItem(
                    text = { Text("+ Add “${value.trim()}” to catalog", color = Spy.Signal, style = MaterialTheme.typography.bodyLarge) },
                    onClick = { onAdd(value); dismissed = true },
                )
            }
        }
    }
}
