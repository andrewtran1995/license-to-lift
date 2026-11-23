package io.github.andrewtran1995.liftlog.ui.log

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.andrewtran1995.liftlog.R
import io.github.andrewtran1995.liftlog.data.ExerciseTypeEntity
import io.github.andrewtran1995.liftlog.data.ExerciseWithSets
import io.github.andrewtran1995.liftlog.data.SessionStore
import io.github.andrewtran1995.liftlog.data.WorkoutDao
import io.github.andrewtran1995.liftlog.data.WorkoutWithExercises
import io.github.andrewtran1995.liftlog.data.formatLoggedAt
import io.github.andrewtran1995.liftlog.data.formatStamp
import io.github.andrewtran1995.liftlog.data.formatWeight
import io.github.andrewtran1995.liftlog.data.nextLoggedAtChange
import io.github.andrewtran1995.liftlog.data.summarizeSets
import kotlinx.coroutines.delay
import io.github.andrewtran1995.liftlog.ui.figures.ExerciseFigure
import io.github.andrewtran1995.liftlog.ui.theme.Caption
import io.github.andrewtran1995.liftlog.ui.theme.Mono
import io.github.andrewtran1995.liftlog.ui.theme.Panel
import io.github.andrewtran1995.liftlog.ui.theme.Spy
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
internal fun fieldColors() = OutlinedTextFieldDefaults.colors(
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

/** Every exercise chip is this tall, whatever its figure, matching the pinned + button. */
internal val ChipHeight = 40.dp

private val BigNumber = TextStyle(fontFamily = Mono, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, textAlign = TextAlign.Center)

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Spy.Panel,
    labelColor = Spy.Text,
    selectedContainerColor = Spy.Signal.copy(alpha = 0.18f),
    selectedLabelColor = Spy.Signal,
)

/**
 * Live logging, one exercise at a time. Exercises sit in a tab row; the current one fills the
 * screen with its sets and an entry panel prefilled from the last set (or from last time), so a
 * straight set is one half-second hold on LOG SET. Everything saves as you go.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LogScreen(dao: WorkoutDao, store: SessionStore, contentPadding: PaddingValues, onFinished: () -> Unit) {
    val vm: SessionViewModel = viewModel(factory = viewModelFactory { initializer { SessionViewModel(dao, store) } })
    val context = LocalContext.current
    val session by vm.session.collectAsStateWithLifecycle()
    val activeId by vm.activeId.collectAsStateWithLifecycle()
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    var editingDetails by remember { mutableStateOf(false) }

    // Opening (or coming back to) a mission lands on the exercise you were last working on.
    LaunchedEffect(activeId, session?.exercises?.isNotEmpty()) {
        val list = session?.exercises?.sortedBy { it.exercise.position }.orEmpty()
        if (activeId != null && list.isNotEmpty()) vm.selectLatest(activeId!!, list)
    }

    val exercises = session?.exercises?.sortedBy { it.exercise.position }.orEmpty()
    val selected = vm.selected.coerceIn(0, (exercises.size - 1).coerceAtLeast(0))
    // A fresh mission opens on the full add screen; after that, adding is the pinned + and its sheet.
    val adding = exercises.isEmpty()
    var addSheet by remember { mutableStateOf(false) }

    // Lift the layout above the keyboard only when a field on this screen is focused (e.g. weight).
    // The keyboard also opens for the details sheet and other popups; those float on their own
    // window, so the exercise card underneath must stay put.
    var fieldFocused by remember { mutableStateOf(false) }
    val ime = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val bottom = if (fieldFocused) max(contentPadding.calculateBottomPadding(), ime + 12.dp) else contentPadding.calculateBottomPadding()
    Column(
        Modifier.fillMaxSize().onFocusChanged { fieldFocused = it.hasFocus }.padding(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding(), bottom = bottom,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header: tap the title to name the mission, change the date or add notes.
        Row(verticalAlignment = Alignment.CenterVertically) {
            val w = session?.workout
            Column(Modifier.weight(1f).clickable(enabled = w != null) { editingDetails = true }) {
                Caption(if (w == null) "// Standby" else "// Mission active · ${formatStamp(w.date)}")
                Text(
                    (w?.name?.ifBlank { null } ?: if (w == null) "Mission log" else "Untitled op").uppercase(),
                    style = MaterialTheme.typography.headlineLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (w != null && w.name.isBlank()) Caption("Tap to name it")
            }
            if (w != null) {
                Button(
                    onClick = {
                        vm.finish { filed ->
                            Toast.makeText(context, if (filed) "Mission filed" else "Empty mission discarded", Toast.LENGTH_SHORT).show()
                            if (filed) onFinished()
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                ) { Text("FINISH", style = MaterialTheme.typography.labelLarge) }
            }
        }

        if (exercises.isNotEmpty()) {
            val rowState = rememberLazyListState()
            // The row shows newest first, right beside the pinned +, while `exercises` and
            // `selected` stay in workout order (so NEXT, the Dossier and exports keep it).
            val displayIndex = exercises.size - 1 - selected
            LaunchedEffect(selected, exercises.size) { rowState.animateScrollToItem(displayIndex) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PinnedAdd(onClick = { addSheet = true })
                LazyRow(
                    state = rowState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).fadeEdges(rowState),
                ) {
                    itemsIndexed(exercises.asReversed(), key = { _, e -> e.exercise.id }) { d, e ->
                        val i = exercises.size - 1 - d
                        FilterChip(
                            selected = i == selected,
                            onClick = { vm.selected = i },
                            label = { Text("${e.exercise.name} · ${e.sets.size}", maxLines = 1) },
                            leadingIcon = { ExerciseFigure(e.exercise.name, Modifier.size(width = 28.dp, height = 24.dp)) },
                            colors = chipColors(),
                            modifier = Modifier.height(ChipHeight),
                        )
                    }
                }
            }
        }

        Box(Modifier.weight(1f)) {
            if (adding) {
                AddExercisePanel(
                    catalog = catalog.filter { t -> exercises.none { it.exercise.typeId == t.id } },
                    repeatable = if (exercises.isEmpty()) recentMissions(history, activeId) else emptyList(),
                    onPick = { vm.addExercise(it) },
                    onAdd = { vm.addByName(it) },
                    onRepeat = { vm.repeat(it) },
                )
            } else {
                val ex = exercises[selected]
                val last = remember(ex.exercise.typeId, history, activeId) { vm.lastTime(ex.exercise.typeId, history) }
                ExercisePanel(
                    vm = vm,
                    ex = ex,
                    last = last,
                    next = exercises.getOrNull(selected + 1)?.exercise?.name,
                    onNext = { vm.selected = selected + 1 },
                )
            }
        }
    }

    if (addSheet && exercises.isNotEmpty()) {
        AddExerciseSheet(
            catalog = catalog.filter { t -> exercises.none { it.exercise.typeId == t.id } },
            // addExercise selects the new exercise, so closing the sheet lands on it, ready to log.
            onPick = { vm.addExercise(it); addSheet = false },
            onAdd = { vm.addByName(it); addSheet = false },
            onDismiss = { addSheet = false },
        )
    }

    val w = session?.workout
    if (editingDetails && w != null) {
        DetailsSheet(
            name = w.name, notes = w.notes, date = LocalDate.parse(w.date),
            onSave = { n, no, d -> vm.updateDetails(n, no, d); editingDetails = false },
            onDiscard = { vm.discard(); editingDetails = false },
            onDismiss = { editingDetails = false },
        )
    }
}

/** Always-visible add button pinned left of the exercise tabs. */
@Composable
private fun PinnedAdd(onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(MaterialTheme.shapes.small)
            .background(Spy.Signal.copy(alpha = 0.14f))
            .border(1.dp, Spy.Signal.copy(alpha = 0.45f), MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClickLabel = "Add exercise", onClick = onClick)
            .semantics { contentDescription = "Add exercise" },
        contentAlignment = Alignment.Center,
    ) { Text("+", style = BigNumber.copy(fontSize = 22.sp, color = Spy.Signal)) }
}

/** Fades whichever edge of a horizontal list has more items scrolled out of view. */
private fun Modifier.fadeEdges(state: androidx.compose.foundation.lazy.LazyListState, width: Dp = 24.dp): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val f = (width.toPx() / size.width).coerceIn(0f, 0.5f)
            val start = if (state.canScrollBackward) Color.Transparent else Color.Black
            val end = if (state.canScrollForward) Color.Transparent else Color.Black
            drawRect(
                Brush.horizontalGradient(0f to start, f to Color.Black, 1f - f to Color.Black, 1f to end),
                blendMode = BlendMode.DstIn,
            )
        }

/** Mid-mission add: recent exercises and search in a sheet over the exercise you're on. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseSheet(
    catalog: List<ExerciseTypeEntity>,
    onPick: (ExerciseTypeEntity) -> Unit,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // Fully expanded, and padded for the keyboard, so the search box and its matches stay above it.
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = Spy.Panel) {
        Box(Modifier.fillMaxHeight().padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding().imePadding()) {
            AddExercisePanel(
                catalog = catalog,
                repeatable = emptyList(),
                onPick = onPick,
                onAdd = onAdd,
                onRepeat = {},
            )
        }
    }
}

/** Up to three recent missions with distinct names, newest first, for "repeat". */
private fun recentMissions(history: List<WorkoutWithExercises>, activeId: String?): List<WorkoutWithExercises> {
    val seen = mutableSetOf<String>()
    return history.filter { it.workout.id != activeId && it.exercises.isNotEmpty() }
        .filter { seen.add(it.workout.name.trim().lowercase().ifBlank { it.workout.id }) }
        .take(3)
}

/**
 * Add screen: search box first, then (with an empty box) Repeat a mission and recent exercises.
 * While you type, only the matches show below the box, inline, so the keyboard and results
 * never cover it. Enter picks an exact match or adds the text as a new exercise.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddExercisePanel(
    catalog: List<ExerciseTypeEntity>,
    repeatable: List<WorkoutWithExercises>,
    onPick: (ExerciseTypeEntity) -> Unit,
    onAdd: (String) -> Unit,
    onRepeat: (WorkoutWithExercises) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val result = remember(query, catalog) { searchCatalog(query, catalog) }
    fun pick(t: ExerciseTypeEntity) { focus.clearFocus(); query = ""; onPick(t) }
    fun add(name: String) { focus.clearFocus(); query = ""; onAdd(name) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "search") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Caption(if (catalog.isEmpty()) "Add your first exercise" else "Add exercise")
                ExerciseSearchField(
                    value = query,
                    onValueChange = { query = it },
                    onSubmit = {
                        when {
                            result.exact != null -> pick(result.exact)
                            query.isNotBlank() -> add(query.trim())
                        }
                    },
                    colors = fieldColors(),
                )
            }
        }
        if (query.isNotBlank()) {
            if (result.exact == null) {
                item(key = "add") { ExerciseResultRow("+ Add \u201c${query.trim()}\u201d to catalog", onClick = { add(query.trim()) }, highlight = true) }
            }
            items(result.matches, key = { "m-" + it.id }) { t -> ExerciseResultRow(t.name, onClick = { pick(t) }) }
            return@LazyColumn
        }
        if (repeatable.isNotEmpty()) {
            item { Caption("Repeat a mission") }
            repeatable.forEach { w ->
                item(key = "repeat-" + w.workout.id) {
                    Panel(Modifier.clickable { onRepeat(w) }) {
                        Text(
                            "OP: " + w.workout.name.ifBlank { "Untitled" }.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "${formatStamp(w.workout.date)}  ·  " + w.exercises.sortedBy { it.exercise.position }.joinToString(", ") { it.exercise.name },
                            style = MaterialTheme.typography.bodySmall, color = Spy.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        if (catalog.isNotEmpty()) {
            item { Caption("Recent") }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    catalog.take(12).forEach { t ->
                        FilterChip(
                            selected = false,
                            onClick = { pick(t) },
                            label = { Text(t.name, maxLines = 1) },
                            leadingIcon = { ExerciseFigure(t.name, Modifier.size(width = 28.dp, height = 24.dp)) },
                            colors = chipColors(),
                            modifier = Modifier.height(ChipHeight),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The current time, updated only when one of the "x ago" labels for [times] would change
 * (see nextLoggedAtChange), so the screen isn't redrawn every frame.
 */
@Composable
private fun rememberNow(times: List<Long>): State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(times) {
        while (true) {
            val t = System.currentTimeMillis()
            now.longValue = t
            val next = nextLoggedAtChange(times, t) ?: break
            delay((next - t).coerceAtLeast(1))
        }
    }
    return now
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExercisePanel(vm: SessionViewModel, ex: ExerciseWithSets, last: LastTime?, next: String?, onNext: () -> Unit) {
    val focus = LocalFocusManager.current
    val sets = ex.sets.sortedBy { it.position }
    val entry = remember(ex.exercise.id, sets.size, last?.workout?.id) { vm.nextEntry(ex, last) }
    var confirmRemove by remember(ex.exercise.id) { mutableStateOf(false) }
    var armedSet by remember(ex.exercise.id) { mutableStateOf<String?>(null) }
    // Every two-tap confirm cancels itself after 3 seconds.
    LaunchedEffect(confirmRemove) { if (confirmRemove) { delay(3_000); confirmRemove = false } }
    LaunchedEffect(armedSet) { if (armedSet != null) { delay(3_000); armedSet = null } }
    val now = rememberNow(sets.mapNotNull { it.loggedAt })

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Two slow reps when the exercise opens and after each logged set; tap to replay.
            ExerciseFigure(ex.exercise.name, Modifier.size(width = 72.dp, height = 63.dp), animated = true, playKey = sets.size)
            Column(Modifier.weight(1f)) {
                Text(ex.exercise.name, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (last != null) {
                    Text(
                        "LAST ${last.workout.date.substring(5)}: " + summarizeSets(last.exercise.sets, last.workout.unit),
                        style = MaterialTheme.typography.bodySmall, color = Spy.Signal, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Caption("First time on file")
                }
            }
            TextButton(onClick = { if (confirmRemove) vm.removeExercise(ex) else confirmRemove = true }) {
                Text(if (confirmRemove) "CONFIRM" else "REMOVE", style = MaterialTheme.typography.labelMedium, color = if (confirmRemove) Spy.Classified else Spy.Muted)
            }
        }

        // Logged sets: the only part that scrolls, and only once there are many.
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (sets.isEmpty()) {
                item { Text("No sets yet. Log the first one below.", color = Spy.Muted, style = MaterialTheme.typography.bodyMedium) }
            }
            itemsIndexed(sets, key = { _, s -> s.id }) { i, s ->
                Row(
                    Modifier.fillMaxWidth().height(40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("%02d".format(i + 1), Modifier.width(36.dp), style = MaterialTheme.typography.labelLarge, color = Spy.Muted)
                    Text(
                        formatWeight(s.weight, vm.unit) + (s.reps?.let { "  ×  $it" } ?: ""),
                        Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                    )
                    // Read `now` here, so only this label redraws when it ticks.
                    s.loggedAt?.let { t ->
                        Text(formatLoggedAt(t, now.value), style = MaterialTheme.typography.bodySmall, color = Spy.Muted, maxLines = 1)
                    }
                    // Two taps to delete: ✕ arms the row, CONFIRM deletes; it disarms after 3 seconds.
                    if (armedSet == s.id) {
                        TextButton(onClick = { armedSet = null; vm.deleteSet(s, ex.exercise.workoutId) }) {
                            Text("CONFIRM", style = MaterialTheme.typography.labelMedium, color = Spy.Classified)
                        }
                    } else {
                        IconButton(onClick = { armedSet = s.id }) {
                            Icon(painterResource(R.drawable.ic_close), contentDescription = "Delete set ${i + 1}", tint = Spy.Muted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Entry panel, pinned above the nav bar.
        Panel {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Caption("Weight · ${vm.unit}")
                    OutlinedTextField(
                        value = entry.weight,
                        onValueChange = { entry.weight = it },
                        placeholder = { Text("BW", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = BigNumber.copy(fontSize = 22.sp)) },
                        textStyle = BigNumber,
                        singleLine = true,
                        colors = fieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    )
                }
                Column(Modifier.weight(1.25f)) {
                    Caption("Reps")
                    RepsStepper(entry.reps, onChange = { entry.reps = it })
                }
            }
            QuickReps(base = remember(entry) { entry.reps ?: last?.exercise?.sets?.lastOrNull()?.reps ?: 10 }, current = entry.reps, onPick = { entry.reps = it })
            if (entry.fromLastTime) Caption("Prefilled from last time")
            HoldToLogButton(
                label = "✓  HOLD TO LOG SET %02d".format(sets.size + 1),
                onConfirm = { focus.clearFocus(); vm.logSet(ex, entry) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            )
            if (next != null) {
                TextButton(onClick = onNext, modifier = Modifier.align(Alignment.End)) {
                    Text("NEXT: ${next.uppercase()} →", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun RepsStepper(reps: Int?, onChange: (Int?) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
            onClick = { onChange(reps?.let { if (it <= 1) null else it - 1 }) },
            modifier = Modifier.size(56.dp),
            contentPadding = PaddingValues(0.dp),
            border = BorderStroke(1.dp, Spy.Wire),
        ) { Text("−", style = BigNumber.copy(fontSize = 24.sp)) }
        Text(reps?.toString() ?: "—", Modifier.weight(1f), style = BigNumber)
        OutlinedButton(
            onClick = { onChange((reps ?: 0) + 1) },
            modifier = Modifier.size(56.dp),
            contentPadding = PaddingValues(0.dp),
            border = BorderStroke(1.dp, Spy.Wire),
        ) { Text("+", style = BigNumber.copy(fontSize = 24.sp)) }
    }
}

/** Five one-tap rep counts around your usual number, e.g. 6 · 8 · 10 · 12 · 14. */
@Composable
private fun QuickReps(base: Int, current: Int?, onPick: (Int) -> Unit) {
    val options = listOf(base - 4, base - 2, base, base + 2, base + 4).filter { it > 0 }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { n ->
            FilterChip(
                selected = n == current,
                onClick = { onPick(n) },
                label = { Text("$n", Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                modifier = Modifier.weight(1f),
                colors = chipColors(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DetailsSheet(
    name: String, notes: String, date: LocalDate,
    onSave: (String, String, LocalDate) -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
) {
    var n by remember { mutableStateOf(name) }
    var no by remember { mutableStateOf(notes) }
    var d by remember { mutableStateOf(date) }
    var pickingDate by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Spy.Panel) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Caption("// Mission details")
            OutlinedTextField(n, { n = it }, Modifier.fillMaxWidth(), label = { Text("Operation") }, placeholder = { Text("e.g. Leg day") }, singleLine = true, colors = fieldColors())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Caption("Date"); Text(formatStamp(d.toString()), style = MaterialTheme.typography.titleMedium) }
                TextButton(onClick = { pickingDate = true }) { Text("CHANGE", style = MaterialTheme.typography.labelLarge) }
            }
            OutlinedTextField(no, { no = it }, Modifier.fillMaxWidth().height(110.dp), label = { Text("Field notes") }, colors = fieldColors())
            Button(onClick = { onSave(n, no, d) }, modifier = Modifier.fillMaxWidth().height(52.dp), shapes = ButtonDefaults.shapes()) {
                Text("SAVE DETAILS", style = MaterialTheme.typography.labelLarge)
            }
            TextButton(onClick = { if (confirmDiscard) onDiscard() else confirmDiscard = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (confirmDiscard) "TAP AGAIN TO BURN THIS MISSION" else "DISCARD MISSION", color = Spy.Classified, style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    if (pickingDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { d = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickingDate = false
                }) { Text("CONFIRM") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("ABORT") } },
        ) { DatePicker(state) }
    }
}
