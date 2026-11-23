package io.github.andrewtran1995.liftlog.ui.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.andrewtran1995.liftlog.data.ExerciseTypeEntity
import io.github.andrewtran1995.liftlog.data.exerciseKey
import io.github.andrewtran1995.liftlog.ui.figures.ExerciseFigure
import io.github.andrewtran1995.liftlog.ui.theme.Spy

/**
 * Catalog matches for [query]: names that start with it first, then names that contain it.
 * [exact] is the entry whose name equals the query (ignoring case and spaces), if any.
 */
data class SearchResult(val exact: ExerciseTypeEntity?, val matches: List<ExerciseTypeEntity>)

fun searchCatalog(query: String, catalog: List<ExerciseTypeEntity>): SearchResult {
    val key = exerciseKey(query)
    if (key.isEmpty()) return SearchResult(null, emptyList())
    val hits = catalog.filter { key in it.nameKey }.sortedWith(compareBy({ !it.nameKey.startsWith(key) }, { it.name.lowercase() }))
    return SearchResult(catalog.firstOrNull { it.nameKey == key }, hits)
}

/**
 * The search box. It has no popup: matches are drawn inline below it by the caller, so nothing
 * ever covers the box while the keyboard is up. Enter calls [onSubmit].
 */
@Composable
fun ExerciseSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    colors: TextFieldColors,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search or add an exercise") },
        textStyle = MaterialTheme.typography.titleMedium,
        singleLine = true,
        colors = colors,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
    )
}

/** A full-width result row: figure and name. */
@Composable
fun ExerciseResultRow(name: String, onClick: () -> Unit, highlight: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onClick).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ExerciseFigure(name, Modifier.size(width = 36.dp, height = 32.dp))
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (highlight) Spy.Signal else Spy.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
