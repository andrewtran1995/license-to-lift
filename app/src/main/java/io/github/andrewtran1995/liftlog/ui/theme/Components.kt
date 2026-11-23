package io.github.andrewtran1995.liftlog.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Screen title with a small file-code line above it, e.g. "// FILE 01" over "NEW MISSION". */
@Composable
fun ScreenHeader(code: String, title: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(bottom = 4.dp)) {
        Text(code, style = MaterialTheme.typography.labelMedium, color = Spy.Signal)
        Text(title.lowercase(), style = MaterialTheme.typography.headlineLarge)
    }
}

/** Translucent instrument panel: the blueprint grid shows faintly through it. */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Spy.Panel),
        border = BorderStroke(1.dp, Spy.Wire),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

/** Small lowercase label used for column headings and field captions. */
@Composable
fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(text.lowercase(), modifier, style = MaterialTheme.typography.labelSmall, color = Spy.Muted, maxLines = 1)
}

/**
 * A label with its readout underneath: name or date on top, sets below in signal cyan.
 * Stacked rather than side by side so a long set list wraps instead of squeezing the label.
 */
@Composable
fun ReadoutRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium.numbers(), color = Spy.Signal)
    }
}
