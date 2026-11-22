package io.github.andrewtran1995.liftlog.ui.log

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import io.github.andrewtran1995.liftlog.ui.theme.Spy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long LOG SET must be held. Long enough to rule out a brush, short enough not to slow you. */
private const val HoldMillis = 500

/**
 * Press and hold to confirm. The button fills left to right while held and fires [onConfirm]
 * with a short buzz when full; letting go early cancels and briefly shows "HOLD TO LOG".
 * Screen readers activate it with a normal double-tap (a semantics click action).
 */
@Composable
fun HoldToLogButton(label: String, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    val confirm by rememberUpdatedState(onConfirm)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val fill = remember { Animatable(0f) }
    var hint by remember { mutableStateOf(false) }

    LaunchedEffect(hint) { if (hint) { delay(1500); hint = false } }

    val shape = MaterialTheme.shapes.extraLarge
    Box(
        modifier
            .clip(shape)
            .background(Spy.Signal.copy(alpha = 0.28f))
            .semantics {
                role = Role.Button
                onClick(label) { confirm(); true }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    hint = false
                    val hold = scope.launch {
                        fill.animateTo(1f, tween((HoldMillis * (1f - fill.value)).toInt(), easing = LinearEasing))
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        confirm()
                        fill.snapTo(0f)
                    }
                    tryAwaitRelease()
                    if (hold.isActive) {
                        hold.cancel()
                        hint = true
                        scope.launch { fill.animateTo(0f, tween(150)) }
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(fill.value).background(Spy.Signal))
        Text(
            if (hint) "HOLD TO LOG" else label,
            style = MaterialTheme.typography.labelLarge,
            color = if (fill.value > 0.5f) Spy.Ink else Spy.Text,
        )
    }
}
